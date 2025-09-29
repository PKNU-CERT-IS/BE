package org.certis.studyplatform.project.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.ApplicationException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.project.application.object.command.CreateProjectCommand;
import org.certis.studyplatform.project.application.object.command.DeleteProjectCommand;
import org.certis.studyplatform.project.application.object.command.EndProjectCommand;
import org.certis.studyplatform.project.application.object.command.UpdateProjectCommand;
import org.certis.studyplatform.member.application.GracePeriodService;
import org.certis.studyplatform.project.domain.service.ProjectDomainService;
import org.certis.studyplatform.project.domain.service.ProjectParticipantDomainService;
import org.certis.studyplatform.project.domain.repository.ProjectCommandRepository;
import org.certis.studyplatform.project.domain.vo.*;
import org.certis.studyplatform.shared.service.S3FileService;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectAttachedEntity;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectAttachedJpaRepository;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.certis.studyplatform.project.application.object.command.CreateProjectAttachedCommand;

import java.time.OffsetDateTime;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import java.util.Base64;

/**
 * Project Command Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 쓰기 작업 처리 (CQRS Command Side)
 *
 * Command 객체를 Domain Service로 전달하여 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectCommandService {

    private final ProjectDomainService projectDomainService;
    private final ProjectParticipantDomainService projectParticipantDomainService;
    private final ProjectCommandRepository projectCommandRepository;
    private final S3FileService s3FileService;
    private final GracePeriodService gracePeriodService;
    private final ProjectAttachedJpaRepository projectAttachedJpaRepository;
    private final ProjectJpaRepository projectJpaRepository;
    // ObjectMapper retained for potential future use
    // private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 프로젝트 생성
     */

    //TODO: 추후에 Transactional 결합도 낮추기
    // 1. 생성자 존재 확인
    // 2. 프로젝트 생성
    // 3. 생성자를 참가자로 등록
    @Transactional
    public ProjectVo createProject(CreateProjectCommand command) {
        log.info("Command: Creating project - {}", command.title());

        // 1) 첨부파일을 S3에 먼저 업로드 (data URL이면 업로드, 아니면 기존 URL 사용)
        java.util.List<CreateProjectAttachedCommand> processed = null;
        if (command.attachedFiles() != null && !command.attachedFiles().isEmpty()) {
            processed = new java.util.ArrayList<>();
            for (var fileCmd : command.attachedFiles()) {
                String finalUrl = fileCmd.url();
                if (finalUrl != null && finalUrl.startsWith("data:")) {
                    try {
                        String[] parts = finalUrl.split(",", 2);
                        String base64Part = parts.length == 2 ? parts[1] : parts[0];
                        byte[] bytes = Base64.getDecoder().decode(base64Part);
                        String contentType = mapAttachedTypeToContentType(fileCmd.type());
                        finalUrl = s3FileService.uploadBytes(bytes, contentType, fileCmd.name(), S3FileService.DomainFolders.PROJECT_ATTACHMENTS, System.currentTimeMillis());
                    } catch (Exception e) {
                        log.error("S3 upload failed for project attachment: {}", fileCmd.name(), e);
                        throw new ApplicationException(
                                ExceptionStatus.PROJECT_APPLICATION_ATTACHMENT_UPLOAD_FAILED,
                                "프로젝트 첨부파일 업로드에 실패했습니다: " + fileCmd.name(), e);
                    }
                }
                if (finalUrl == null || finalUrl.isEmpty()) {
                    throw new ApplicationException(
                            ExceptionStatus.PROJECT_APPLICATION_ATTACHMENT_UPLOAD_FAILED,
                            "attachments[].attachedUrl 이 누락되었습니다. data: URL 또는 사전 업로드된 S3 URL을 보내주세요: " + fileCmd.name());
                }
                processed.add(CreateProjectAttachedCommand.of(fileCmd.name(), fileCmd.type(), fileCmd.size(), finalUrl));
            }
        }

        // 2) Command 객체를 Domain Service로 전달
        ProjectVo createdVo = projectDomainService.createProject(command);

        // 3) 첨부파일 저장 (생성 시 첨부가 포함된 경우)
        java.util.List<CreateProjectAttachedCommand> attachmentsToSave = processed != null ? processed : command.attachedFiles();
        if (attachmentsToSave != null && !attachmentsToSave.isEmpty()) {
            for (var file : attachmentsToSave) {
                ProjectAttachedEntity entity = ProjectAttachedEntity.builder()
                        .projectId(createdVo.id())
                        .memberId(command.creatorId())
                        .attachedUrl(file.url())
                        .name(file.name())
                        .type(file.type() != null ? file.type().name() : null)
                        .size(file.size() != null ? String.valueOf(file.size()) : "0")
                        .build();
                projectAttachedJpaRepository.save(entity);
            }
        }

        projectParticipantDomainService.registerProjectCreatorAsParticipant(createdVo.id(), command.creatorId());

        log.info("Command: Project created successfully - ID: {}", createdVo.id());
        return createdVo;
    }

    /**
     * 프로젝트 수정
     */
    @Transactional
    public ProjectVo updateProject(UpdateProjectCommand command) {
        log.info("Command: Updating project - ID: {}", command.id());

        // 1) 첨부파일 사전 처리 (data URL -> S3 업로드)
        java.util.List<CreateProjectAttachedCommand> processed = null;
        if (command.attachedFiles() != null) {
            processed = new java.util.ArrayList<>();
            for (var fileCmd : command.attachedFiles()) {
                String finalUrl = fileCmd.url();
                if (finalUrl != null && finalUrl.startsWith("data:")) {
                    try {
                        String[] parts = finalUrl.split(",", 2);
                        String base64Part = parts.length == 2 ? parts[1] : parts[0];
                        byte[] bytes = Base64.getDecoder().decode(base64Part);
                        String contentType = mapAttachedTypeToContentType(fileCmd.type());
                        finalUrl = s3FileService.uploadBytes(bytes, contentType, fileCmd.name(), S3FileService.DomainFolders.PROJECT_ATTACHMENTS, System.currentTimeMillis());
                    } catch (Exception e) {
                        log.error("S3 upload failed for project attachment(update): {}", fileCmd.name(), e);
                        throw new ApplicationException(
                                ExceptionStatus.PROJECT_APPLICATION_ATTACHMENT_UPLOAD_FAILED,
                                "프로젝트 첨부파일 업로드에 실패했습니다: " + fileCmd.name(), e);
                    }
                }
                if (finalUrl == null || finalUrl.isEmpty()) {
                    throw new ApplicationException(
                            ExceptionStatus.PROJECT_APPLICATION_ATTACHMENT_UPLOAD_FAILED,
                            "attachments[].attachedUrl 이 누락되었습니다. data: URL 또는 사전 업로드된 S3 URL을 보내주세요: " + fileCmd.name());
                }
                processed.add(CreateProjectAttachedCommand.of(fileCmd.name(), fileCmd.type(), fileCmd.size(), finalUrl));
            }
        }

        // 2) Command 객체를 Domain Service로 전달
        ProjectVo updatedVo = projectDomainService.updateProject(command);

        // 정책: attachments == null -> 변경 없음, attachments 제공됨(빈 포함) -> 기존 전체 삭제(S3 포함) 후 신규로 덮어쓰기
        if (command.attachedFiles() == null) {
            return updatedVo;
        }

        // 기존 첨부 전체 삭제 (소프트 딜리트) + S3 원본 삭제
        var existing = projectAttachedJpaRepository.findByProjectId(updatedVo.id());
        if (!existing.isEmpty()) {
            for (ProjectAttachedEntity entity : existing) {
                try { s3FileService.deleteFile(entity.getAttachedUrl()); } catch (Exception ex) {
                    log.warn("Failed to delete S3 file on project update clear: {}", entity.getAttachedUrl(), ex);
                }
            }
            projectAttachedJpaRepository.deleteAll(existing);
        }

        // 빈 리스트면 여기서 종료 (완전 삭제 상태 유지)
        if (command.attachedFiles().isEmpty()) {
            return updatedVo;
        }

        // 신규 첨부 저장 (덮어쓰기)
        java.util.List<CreateProjectAttachedCommand> attachmentsToSave = processed != null ? processed : command.attachedFiles();
        for (var file : attachmentsToSave) {
            ProjectAttachedEntity entity = ProjectAttachedEntity.builder()
                    .projectId(updatedVo.id())
                    .memberId(command.requesterId())
                    .attachedUrl(file.url())
                    .name(file.name())
                    .type(file.type() != null ? file.type().name() : null)
                    .size(file.size() != null ? String.valueOf(file.size()) : "0")
                    .build();
            projectAttachedJpaRepository.save(entity);
        }

        log.info("Command: Project updated successfully - ID: {}", updatedVo.id());
        return updatedVo;
    }

    /**
     * 프로젝트 삭제
     */
    @Transactional
    public void deleteProject(DeleteProjectCommand command) {
        log.info("Command: Deleting project - ID: {}", command.id());

        // Command 객체를 Domain Service로 전달
        projectDomainService.deleteProject(command);

        log.info("Command: Project deleted successfully - ID: {}", command.id());
    }

    /**
     * 프로젝트 종료
     */
    @Transactional
    public ProjectVo endProject(EndProjectCommand command) {
        log.info("Command: Ending project - ID: {}", command.projectId());

        // 현재 상태 선조회하여 중복 신청 방지 (INPROGRESS/COMPLETED 차단)
        try {
            ProjectVo current = projectDomainService.getProjectById(new org.certis.studyplatform.project.application.object.query.GetProjectByIdQuery(command.projectId()));
            if (current != null && current.resultSubmitStatus() != null) {
                if (current.resultSubmitStatus().isInProgress()) {
                    throw new ApplicationException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION, "이미 종료 신청이 진행 중입니다");
                }
                if (current.resultSubmitStatus().isCompleted()) {
                    throw new ApplicationException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION, "이미 종료된 프로젝트입니다");
                }
            }
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception ignore) {
            // 조회 실패는 뒤 단계에서 도메인에서 처리됨
        }

        // Command 객체를 Domain Service로 전달 (파일명은 Domain Service에서 처리)
        ProjectVo endedVo = projectDomainService.endProject(command);

        // 조기 종료 시 유예기간 재조정 (실제 종료 시점은 현재 시각으로 판단)
        log.info("Command: Checking for early termination - project ID: {}, startDate: {}", 
            endedVo.id(), endedVo.startDate());
        
        if (endedVo.startDate() != null) {
            OffsetDateTime originalEndDate = endedVo.startDate().plusWeeks(4); // 기본 4주 프로젝트 가정
            OffsetDateTime actualEndDate = OffsetDateTime.now();
            
            log.info("Command: Early termination check - originalEndDate: {}, actualEndDate(now): {}, isEarly: {}", 
                originalEndDate, actualEndDate, actualEndDate.isBefore(originalEndDate));
            
            if (actualEndDate.isBefore(originalEndDate)) {
                log.info("Command: Project terminated early - adjusting grace period for project ID: {}", endedVo.id());
                try {
                    gracePeriodService.adjustGracePeriodForEarlyTerminatedProject(
                        endedVo.id(), 
                        endedVo.startDate(), 
                        actualEndDate
                    );
                    log.info("Command: Grace period adjustment completed for project ID: {}", endedVo.id());
                } catch (Exception e) {
                    log.error("Command: Failed to adjust grace period for early terminated project - ID: {}, error: {}", 
                        endedVo.id(), e.getMessage(), e);
                    // 유예기간 재조정 실패가 프로젝트 종료를 중단시키지 않도록 처리
                }
            } else {
                log.info("Command: Project not terminated early - no grace period adjustment needed for project ID: {}", endedVo.id());
            }
        }

        // 파일 업로드 후 단일 URL 저장 및 제출 상태 갱신
        String attachmentUrl = null;
        if (command.attachment() != null && !command.attachment().isBlank()) {
            String provided = command.attachment();
            try {
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
                    attachmentUrl = s3FileService.uploadBytes(bytes, contentType, customFilename, S3FileService.DomainFolders.PROJECT_END_ATTACHMENTS, endedVo.id());
                } else {
                    attachmentUrl = provided;
                }
            } catch (Exception e) {
                log.error("Failed to handle project end attachment (string)", e);
                throw new ApplicationException(
                    ExceptionStatus.PROJECT_APPLICATION_ATTACHMENT_UPLOAD_FAILED,
                    "프로젝트 종료 첨부파일 처리에 실패했습니다",
                    e
                );
            }
        }

        projectJpaRepository.updateResultSubmission(
                endedVo.id(),
                OffsetDateTime.now(),
                ResultSubmitStatus.INPROGRESS,
                attachmentUrl
        );

        log.info("Command: Project end submitted (awaiting approval) - ID: {}", endedVo.id());
        return endedVo;
    }

    @Transactional
    public void approveProjectEnd(Long projectId, Long adminId) {
        log.info("Command: Approving project end - projectId: {} by admin: {}", projectId, adminId);
        projectJpaRepository.approveEnd(projectId, OffsetDateTime.now(), ResultSubmitStatus.COMPLETED);
    }

    @Transactional
    public void rejectProjectEnd(Long projectId, Long adminId) {
        log.info("Command: Rejecting project end - projectId: {} by admin: {}", projectId, adminId);
        // Read current attachment URL and delete from S3
        projectJpaRepository.findById(projectId).ifPresent(entity -> {
            String url = entity.getResultAttachmentUrl();
            if (url != null && !url.isEmpty()) {
                try { s3FileService.deleteFile(url); } catch (Exception ex) {
                    log.warn("Failed to delete S3 file on reject: {}", url, ex);
                }
            }
        });
        // Mark result submission rejected only; keep deletedAt as-is (null) for visibility
        OffsetDateTime now = OffsetDateTime.now();
        projectJpaRepository.rejectEnd(projectId, ResultSubmitStatus.REJECTED, now);
    }

    /**
     * 프로젝트 생성 승인: status를 APPROVED로 변경하고 유예기간 연장
     */
    @Transactional
    public void approveProjectCreation(Long projectId, Long adminId) {
        log.info("Command: Approving project creation - projectId: {} by admin: {}", projectId, adminId);
        // 1. 프로젝트 status를 APPROVED로 변경 (startedAt은 유지)
        projectCommandRepository.approveCreation(projectId);
        log.info("Command: Project status updated to APPROVED - projectId: {}", projectId);
        
        try {
            // 2. 프로젝트 정보 조회 후 유예기간 연장
            projectJpaRepository.findById(projectId).ifPresent(entity -> {
                try {
                    gracePeriodService.extendGracePeriodForApprovedProject(
                            entity.getId(), entity.getStartedAt(), entity.getEndedAt());
                    log.info("Command: Grace period extended for approved project - projectId: {}", projectId);
                } catch (Exception e) {
                    log.warn("Failed to extend grace period on project creation approve - projectId: {}", projectId, e);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to process project creation approve - projectId: {}", projectId, e);
        }
    }

    /**
     * 프로젝트 생성 거절: 소프트 삭제 처리 (deleted_at 설정)
     */
    @Transactional
    public void rejectProjectCreation(Long projectId, Long adminId) {
        log.info("Command: Rejecting project creation - projectId: {} by admin: {}", projectId, adminId);
        projectJpaRepository.bulkSoftDeleteById(projectId, OffsetDateTime.now());
    }

    /**
     * 파일명에서 특수문자 제거 및 안전한 파일명 생성
     */
    /**
     * 프로젝트 첨부파일 업로드
     */
    @Transactional
    public String uploadProjectAttachment(Long projectId, Long memberId, MultipartFile file) {
        log.info("Command: Uploading project attachment for project ID: {}, member ID: {}", projectId, memberId);

        // S3에 첨부파일 업로드
        String attachmentUrl = projectDomainService.uploadProjectAttachment(projectId, memberId, file);

        log.info("Command: Project attachment uploaded successfully for project ID: {}, member ID: {}, URL: {}", projectId, memberId, attachmentUrl);
        return attachmentUrl;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "unknown";
        return filename.replaceAll("[^a-zA-Z0-9가-힣]", "_")
                      .replaceAll("_{2,}", "_")
                      .replaceAll("^_|_$", "");
    }

    private String mapAttachedTypeToContentType(org.certis.studyplatform.shared.type.AttachedType type) {
        if (type == null) return "application/octet-stream";
        return switch (type) {
            case PDF -> "application/pdf";
            case HWP -> "application/x-hwp";
            case HWPX -> "application/vnd.hancom.hwpx";
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
}