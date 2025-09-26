package org.certis.studyplatform.study.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.ApplicationException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.application.GracePeriodService;
import org.certis.studyplatform.study.domain.service.StudyDomainService;
import org.certis.studyplatform.study.domain.service.StudyParticipantDomainService;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.repository.StudyCommandRepository;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.study.application.object.command.CreateStudyCommand;
import org.certis.studyplatform.study.application.object.command.CreateStudyAttachedCommand;
import org.certis.studyplatform.study.application.object.command.DeleteStudyCommand;
import org.certis.studyplatform.study.application.object.command.EndStudyCommand;
import org.certis.studyplatform.study.application.object.command.UpdateStudyCommand;
import org.certis.studyplatform.shared.service.S3FileService;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;

/**
 * Study Command Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 쓰기 작업 처리 (CQRS Command Side)
 *
 * Command 객체를 Domain Service로 전달하여 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyCommandService {

    private final StudyDomainService studyDomainService;
    private final StudyParticipantDomainService studyParticipantDomainService;
    private final StudyQueryRepository studyQueryRepository;
    private final StudyCommandRepository studyCommandRepository;
    private final S3FileService s3FileService;
    private final GracePeriodService gracePeriodService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 프로젝트 생성
     */

    //TODO: 추후에 Transactional 결합도 낮추기
    // 1. 생성자 존재 확인
    // 2. 프로젝트 생성
    // 3. 생성자를 참가자로 등록
    @Transactional
    public StudyVo createStudy(CreateStudyCommand command) {
        log.info("Command: Creating study - {}", command.title());

        // 1) 첨부파일을 S3에 먼저 업로드 (data URL이면 업로드, 아니면 기존 URL 사용)
        java.util.List<CreateStudyAttachedCommand> processed = null;
        if (command.attachedFiles() != null && !command.attachedFiles().isEmpty()) {
            processed = new java.util.ArrayList<>();
            for (var fileCmd : command.attachedFiles()) {
                String finalUrl = fileCmd.url();
                if (finalUrl != null && finalUrl.startsWith("data:")) {
                    try {
                        String[] parts = finalUrl.split(",", 2);
                        String base64Part = parts.length == 2 ? parts[1] : parts[0];
                        byte[] bytes = java.util.Base64.getDecoder().decode(base64Part);
                        String contentType = mapAttachedTypeToContentType(fileCmd.type());
                        finalUrl = s3FileService.uploadBytes(bytes, contentType, fileCmd.name(), S3FileService.DomainFolders.STUDY_ATTACHMENTS, System.currentTimeMillis());
                    } catch (Exception e) {
                        log.error("S3 upload failed for study attachment: {}", fileCmd.name(), e);
                        throw new ApplicationException(
                                ExceptionStatus.STUDY_APPLICATION_ATTACHMENT_UPLOAD_FAILED,
                                "스터디 첨부파일 업로드에 실패했습니다: " + fileCmd.name(), e);
                    }
                }
                if (finalUrl == null || finalUrl.isEmpty()) {
                    throw new ApplicationException(
                            ExceptionStatus.STUDY_APPLICATION_ATTACHMENT_UPLOAD_FAILED,
                            "attachments[].attachedUrl 이 누락되었습니다. data: URL 또는 사전 업로드된 S3 URL을 보내주세요: " + fileCmd.name());
                }
                processed.add(CreateStudyAttachedCommand.of(fileCmd.name(), fileCmd.type(), fileCmd.size(), finalUrl));
            }
        }

        // 2) 첨부가 반영된 새로운 Command 생성
        CreateStudyCommand finalCommand = CreateStudyCommand.of(
                command.title(),
                command.description(),
                command.content(),
                command.category(),
                command.subCategory(),
                command.startDate(),
                command.endDate(),
                command.githubUrl(),
                command.externalUrl(),
                command.thumbnailUrl(),
                processed,
                command.maxParticipants(),
                command.creatorId()
        );

        // 3) 도메인 서비스 호출 (생성)
        StudyVo createdVo = studyDomainService.createStudy(finalCommand);

        studyParticipantDomainService.registerStudyCreatorAsParticipant(createdVo.id(), command.creatorId());

        // 4) 첨부파일 정보 저장 (DB)
        if (processed != null && !processed.isEmpty()) {
            studyDomainService.updateStudyAttachments(createdVo.id(), command.creatorId(), processed);
        }

        log.info("Command: Study created successfully - ID: {}", createdVo.id());
        return createdVo;
    }

    private String mapAttachedTypeToContentType(org.certis.studyplatform.shared.type.AttachedType type) {
        if (type == null) return "application/octet-stream";
        return switch (type) {
            case PDF -> "application/pdf";
            case HWP -> "application/x-hwp";
            case WORD -> "application/msword";
            case PPT -> "application/vnd.ms-powerpoint";
            case PPTX -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case EXCEL -> "application/vnd.ms-excel";
            case TEXT -> "text/plain";
            case PNG -> "image/png";
            case JPEG, JPG -> "image/jpeg";
            case ZIP -> "application/zip";
        };
    }

    /**
     * 프로젝트 수정
     */
    @Transactional
    public StudyVo updateStudy(UpdateStudyCommand command) {
        log.info("Command: Updating study - ID: {}", command.id());

        // 1) 첨부파일 사전 처리 (data URL -> S3 업로드)
        java.util.List<CreateStudyAttachedCommand> processed = null;
        if (command.attachedFiles() != null) {
            processed = new java.util.ArrayList<>();
            for (var fileCmd : command.attachedFiles()) {
                String finalUrl = fileCmd.url();
                if (finalUrl != null && finalUrl.startsWith("data:")) {
                    try {
                        String[] parts = finalUrl.split(",", 2);
                        String base64Part = parts.length == 2 ? parts[1] : parts[0];
                        byte[] bytes = java.util.Base64.getDecoder().decode(base64Part);
                        String contentType = mapAttachedTypeToContentType(fileCmd.type());
                        finalUrl = s3FileService.uploadBytes(bytes, contentType, fileCmd.name(), S3FileService.DomainFolders.STUDY_ATTACHMENTS, System.currentTimeMillis());
                    } catch (Exception e) {
                        log.error("S3 upload failed for study attachment(update): {}", fileCmd.name(), e);
                        throw new ApplicationException(
                                ExceptionStatus.STUDY_APPLICATION_ATTACHMENT_UPLOAD_FAILED,
                                "스터디 첨부파일 업로드에 실패했습니다: " + fileCmd.name(), e);
                    }
                }
                if (finalUrl == null || finalUrl.isEmpty()) {
                    throw new ApplicationException(
                            ExceptionStatus.STUDY_APPLICATION_ATTACHMENT_UPLOAD_FAILED,
                            "attachments[].attachedUrl 이 누락되었습니다. data: URL 또는 사전 업로드된 S3 URL을 보내주세요: " + fileCmd.name());
                }
                processed.add(CreateStudyAttachedCommand.of(fileCmd.name(), fileCmd.type(), fileCmd.size(), finalUrl));
            }
        }

        // 2) 도메인 서비스로 업데이트 수행
        StudyVo updatedVo = studyDomainService.updateStudy(command);

        // 3) 첨부파일 덮어쓰기 (Infra 정책에 따라 전체 교체)
        if (processed != null) {
            studyDomainService.updateStudyAttachments(updatedVo.id(), command.requesterId(), processed);
        }

        log.info("Command: Study updated successfully - ID: {}", updatedVo.id());
        return updatedVo;
    }

    /**
     * 프로젝트 삭제
     */
    @Transactional
    public void deleteStudy(DeleteStudyCommand command) {
        log.info("Command: Deleting study - ID: {}", command.id());

        // Command 객체를 Domain Service로 전달
        studyDomainService.deleteStudy(command);

        log.info("Command: Study deleted successfully - ID: {}", command.id());
    }

    /**
     * 스터디 종료
     */
    @Transactional
    public StudyVo endStudy(EndStudyCommand command) {
        log.info("Command: Ending study - ID: {}", command.studyId());

        // 현재 상태 선조회하여 중복 신청 방지 (INPROGRESS/COMPLETED 차단)
        try {
            StudyVo current = studyDomainService.getStudyById(new org.certis.studyplatform.study.application.object.query.GetStudyByIdQuery(command.studyId()));
            if (current != null && current.resultSubmitStatus() != null) {
                if (current.resultSubmitStatus().isInProgress()) {
                    throw new ApplicationException(ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "이미 종료 신청이 진행 중입니다");
                }
                if (current.resultSubmitStatus().isCompleted()) {
                    throw new ApplicationException(ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "이미 종료된 스터디입니다");
                }
            }
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception ignore) {
            // 조회 실패는 뒤 단계에서 도메인에서 처리됨
        }

        // Command 객체를 Domain Service로 전달 (파일명은 Domain Service에서 처리)
        StudyVo endedVo = studyDomainService.endStudy(command);

        // 이미 종료 신청 진행 중 또는 완료된 경우 중복 신청 방지
        if (endedVo.resultSubmitStatus() != null) {
            if (endedVo.resultSubmitStatus().isInProgress()) {
                throw new ApplicationException(ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION,
                        "이미 종료 신청이 진행 중입니다");
            }
            if (endedVo.resultSubmitStatus().isCompleted()) {
                throw new ApplicationException(ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION,
                        "이미 종료된 스터디입니다");
            }
        }

        // 조기 종료 시 유예기간 재조정 (실제 종료 시점은 현재 시각으로 판단)
        if (endedVo.startDate() != null) {
            OffsetDateTime originalEndDate = endedVo.startDate().plusWeeks(4); // 기본 4주 스터디 가정
            OffsetDateTime actualEndDate = OffsetDateTime.now();
            
            if (actualEndDate.isBefore(originalEndDate)) {
                log.info("Command: Study terminated early - adjusting grace period for study ID: {}", endedVo.id());
                try {
                    gracePeriodService.adjustGracePeriodForEarlyTerminatedStudy(
                        endedVo.id(), 
                        endedVo.startDate(), 
                        actualEndDate
                    );
                } catch (Exception e) {
                    log.error("Command: Failed to adjust grace period for early terminated study - ID: {}, error: {}", 
                        endedVo.id(), e.getMessage(), e);
                    // 유예기간 재조정 실패가 스터디 종료를 중단시키지 않도록 처리
                }
            }
        }

        // 파일 업로드 후 첨부 JSON 생성 및 제출 상태 갱신
        ArrayNode attachmentsArray = objectMapper.createArrayNode();
        if (command.attachment() != null && !command.attachment().isBlank()) {
            String provided = command.attachment();
            try {
                String fileUrl;
                String originalName = null;
                if (provided.startsWith("data:")) {
                    String header = provided.substring(5, provided.indexOf(',')); // e.g., image/png;base64
                    String contentType = header.contains(";") ? header.substring(0, header.indexOf(';')) : "application/octet-stream";
                    String base64Part = provided.substring(provided.indexOf(',') + 1);
                    byte[] bytes = java.util.Base64.getDecoder().decode(base64Part);
                    String extension = switch (contentType) {
                        case "image/png" -> ".png";
                        case "image/jpeg" -> ".jpg";
                        case "application/pdf" -> ".pdf";
                        case "application/zip" -> ".zip";
                        default -> "";
                    };
                    String customFilename = String.format("%s_%d_%s%s",
                            sanitizeFilename(endedVo.title()),
                            endedVo.id(),
                            sanitizeFilename(endedVo.creatorName()),
                            extension);
                    originalName = customFilename;
                    fileUrl = s3FileService.uploadBytes(bytes, contentType, customFilename, S3FileService.DomainFolders.STUDY_END_ATTACHMENTS, endedVo.id());
                } else {
                    // 이미 업로드된 S3 URL
                    fileUrl = provided;
                }
                ObjectNode attachment = objectMapper.createObjectNode();
                attachment.put("name", originalName != null ? originalName : "result");
                attachment.put("url", fileUrl);
                attachmentsArray.add(attachment);
            } catch (Exception e) {
                log.error("Failed to handle study end attachment (string)", e);
                throw new ApplicationException(
                    ExceptionStatus.STUDY_APPLICATION_ATTACHMENT_UPLOAD_FAILED,
                    "스터디 종료 첨부파일 처리에 실패했습니다",
                    e
                );
            }
        }

        String attachmentUrl = null;
        if (attachmentsArray.size() > 0) {
            attachmentUrl = attachmentsArray.get(0).get("url").asText(null);
        }
        studyDomainService.updateResultSubmission(
                endedVo.id(),
                OffsetDateTime.now(),
                ResultSubmitStatus.INPROGRESS,
                attachmentUrl
        );

        log.info("Command: Study end submitted (awaiting approval) - ID: {}", endedVo.id());
        return endedVo;
    }

    @Transactional
    public void approveStudyEnd(Long studyId, Long adminId) {
        log.info("Command: Approving study end - studyId: {} by admin: {}", studyId, adminId);
        studyDomainService.approveEnd(studyId, OffsetDateTime.now(), ResultSubmitStatus.COMPLETED);
    }

    @Transactional
    public void rejectStudyEnd(Long studyId, Long adminId) {
        log.info("Command: Rejecting study end - studyId: {} by admin: {}", studyId, adminId);
        // 현재 첨부 읽어 삭제
        studyDomainService.getResultAttachmentUrlById(studyId).ifPresent(url -> {
            if (url != null && !url.isEmpty()) {
                try { s3FileService.deleteFile(url); } catch (Exception ex) {
                    log.warn("Failed to delete S3 file on reject: {}", url, ex);
                }
            }
        });
        studyDomainService.rejectEnd(studyId, ResultSubmitStatus.REJECTED, OffsetDateTime.now());
    }

    /**
     * 스터디 생성 승인: status를 APPROVED로 변경하고 유예기간 연장
     */
    @Transactional
    public void approveStudyCreation(Long studyId, Long adminId) {
        log.info("Command: Approving study creation - studyId: {} by admin: {}", studyId, adminId);
        
        // 1. 스터디 status를 APPROVED로 변경
        studyCommandRepository.approveCreation(studyId);
        log.info("Command: Study status updated to APPROVED - studyId: {}", studyId);
        
        try {
            // 2. Domain Service를 통해 스터디 정보 조회 후 유예기간 연장
            StudyVo studyVo = studyDomainService.getStudyById(new org.certis.studyplatform.study.application.object.query.GetStudyByIdQuery(studyId));
            gracePeriodService.extendGracePeriodForApprovedStudy(
                    studyVo.id(), studyVo.startDate(), studyVo.endDate());
            log.info("Command: Grace period extended for approved study - studyId: {}", studyId);
        } catch (Exception e) {
            log.warn("Failed to extend grace period on study creation approve - studyId: {}", studyId, e);
        }
    }

    /**
     * 스터디 생성 거절: 소프트 삭제 처리 (deleted_at 설정)
     */
    @Transactional
    public void rejectStudyCreation(Long studyId, Long adminId) {
        log.info("Command: Rejecting study creation - studyId: {} by admin: {}", studyId, adminId);
        studyDomainService.bulkSoftDeleteById(studyId, OffsetDateTime.now());
    }

    /**
     * 파일명에서 특수문자 제거 및 안전한 파일명 생성
     */
    /**
     * 스터디 첨부파일 업로드
     */
    @Transactional
    public String uploadStudyAttachment(Long studyId, Long memberId, MultipartFile file) {
        log.info("Command: Uploading study attachment for study ID: {}, member ID: {}", studyId, memberId);

        // S3에 첨부파일 업로드
        String attachmentUrl = studyDomainService.uploadStudyAttachment(studyId, memberId, file);

        log.info("Command: Study attachment uploaded successfully for study ID: {}, member ID: {}, URL: {}", studyId, memberId, attachmentUrl);
        return attachmentUrl;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "unknown";
        return filename.replaceAll("[^a-zA-Z0-9가-힣]", "_")
                      .replaceAll("_{2,}", "_")
                      .replaceAll("^_|_$", "");
    }
}