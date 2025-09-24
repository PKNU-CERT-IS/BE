package org.certis.studyplatform.study.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.GracePeriodService;
import org.certis.studyplatform.study.domain.service.StudyDomainService;
import org.certis.studyplatform.study.domain.service.StudyParticipantDomainService;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.study.application.object.command.CreateStudyCommand;
import org.certis.studyplatform.study.application.object.command.DeleteStudyCommand;
import org.certis.studyplatform.study.application.object.command.EndStudyCommand;
import org.certis.studyplatform.study.application.object.command.UpdateStudyCommand;
import org.certis.studyplatform.shared.service.S3FileService;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyAttachedEntity;
import org.certis.studyplatform.study.infrastructure.persistence.jpa.StudyAttachedJpaRepository;
import org.certis.studyplatform.study.infrastructure.persistence.jpa.StudyJpaRepository;
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
    private final S3FileService s3FileService;
    private final GracePeriodService gracePeriodService;
    private final StudyAttachedJpaRepository studyAttachedJpaRepository;
    private final StudyJpaRepository studyJpaRepository;
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

        // Command 객체를 Domain Service로 전달
        StudyVo createdVo = studyDomainService.createStudy(command);

        // 첨부파일 저장 (생성 시 첨부가 포함된 경우)
        if (command.attachedFiles() != null && !command.attachedFiles().isEmpty()) {
            for (var file : command.attachedFiles()) {
                StudyAttachedEntity entity = StudyAttachedEntity.builder()
                        .studyId(createdVo.id())
                        .memberId(command.creatorId())
                        .attachedUrl(file.url())
                        .name(file.name())
                        .type(file.type() != null ? file.type().name() : null)
                        .size(file.size() != null ? String.valueOf(file.size()) : "0")
                        .build();
                studyAttachedJpaRepository.save(entity);
            }
        }

        studyParticipantDomainService.registerStudyCreatorAsParticipant(createdVo.id(), command.creatorId());

        log.info("Command: Study created successfully - ID: {}", createdVo.id());
        return createdVo;
    }

    /**
     * 프로젝트 수정
     */
    @Transactional
    public StudyVo updateStudy(UpdateStudyCommand command) {
        log.info("Command: Updating study - ID: {}", command.id());

        // Command 객체를 Domain Service로 전달
        StudyVo updatedVo = studyDomainService.updateStudy(command);

        // 첨부파일 null이면 기존 첨부 전체 삭제 (S3 + DB)
        if (command.attachedFiles() == null) {
            // 기존 첨부 전체 삭제 (S3 + DB)
            java.util.List<StudyAttachedEntity> existing = studyAttachedJpaRepository.findByStudyId(updatedVo.id());
            for (StudyAttachedEntity e : existing) {
                try {
                    s3FileService.deleteFile(e.getAttachedUrl());
                } catch (Exception ex) {
                    log.warn("Failed to delete study attachment from S3 url={} studyId={}", e.getAttachedUrl(), updatedVo.id(), ex);
                }
            }
            studyAttachedJpaRepository.deleteByStudyId(updatedVo.id());
        } else if (!command.attachedFiles().isEmpty()) {
            for (var file : command.attachedFiles()) {
                StudyAttachedEntity entity = StudyAttachedEntity.builder()
                        .studyId(updatedVo.id())
                        .memberId(command.requesterId())
                        .attachedUrl(file.url())
                        .name(file.name())
                        .type(file.type() != null ? file.type().name() : null)
                        .size(file.size() != null ? String.valueOf(file.size()) : "0")
                        .build();
                studyAttachedJpaRepository.save(entity);
            }
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

        // Command 객체를 Domain Service로 전달 (파일명은 Domain Service에서 처리)
        StudyVo endedVo = studyDomainService.endStudy(command);

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
        if (command.files() != null && !command.files().isEmpty()) {
            for (MultipartFile file : command.files()) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String originalFilename = file.getOriginalFilename();
                        String fileExtension = "";
                        if (originalFilename != null && originalFilename.contains(".")) {
                            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                        }
                        String customFilename = String.format("%s_%d_%s%s",
                                sanitizeFilename(endedVo.title()),
                                endedVo.id(),
                                sanitizeFilename(endedVo.creatorName()),
                                fileExtension);
                        String fileUrl = s3FileService.uploadFileWithCustomName(file, "study-end-attachments", customFilename);
                        ObjectNode attachment = objectMapper.createObjectNode();
                        attachment.put("name", originalFilename != null ? originalFilename : customFilename);
                        attachment.put("url", fileUrl);
                        attachmentsArray.add(attachment);
                    } catch (Exception e) {
                        log.error("Failed to upload study end attachment: {}", file.getOriginalFilename(), e);
                        throw new RuntimeException("스터디 종료 첨부파일 업로드에 실패했습니다: " + file.getOriginalFilename(), e);
                    }
                }
            }
        }

        String attachmentUrl = null;
        if (attachmentsArray.size() > 0) {
            attachmentUrl = attachmentsArray.get(0).get("url").asText(null);
        }
        studyJpaRepository.updateResultSubmission(
                endedVo.id(),
                OffsetDateTime.now(),
                ResultSubmitStatus.INPROGRESS,
                attachmentUrl
        );
        // 즉시 승인 처리 및 종료 시간 설정
        studyJpaRepository.approveEnd(
                endedVo.id(),
                OffsetDateTime.now(),
                ResultSubmitStatus.COMPLETED
        );

        log.info("Command: Study ended and approved successfully - ID: {}", endedVo.id());
        return endedVo;
    }

    @Transactional
    public void approveStudyEnd(Long studyId, Long adminId) {
        log.info("Command: Approving study end - studyId: {} by admin: {}", studyId, adminId);
        studyJpaRepository.approveEnd(studyId, OffsetDateTime.now(), ResultSubmitStatus.COMPLETED);
    }

    @Transactional
    public void rejectStudyEnd(Long studyId, Long adminId) {
        log.info("Command: Rejecting study end - studyId: {} by admin: {}", studyId, adminId);
        // 현재 첨부 읽어 삭제
        studyJpaRepository.findById(studyId).ifPresent(entity -> {
            String url = entity.getResultAttachmentUrl();
            if (url != null && !url.isEmpty()) {
                try { s3FileService.deleteFile(url); } catch (Exception ex) {
                    log.warn("Failed to delete S3 file on reject: {}", url, ex);
                }
            }
        });
        studyJpaRepository.rejectEnd(studyId, ResultSubmitStatus.REJECTED, OffsetDateTime.now());
    }

    /**
     * 스터디 생성 승인: 유예기간 연장만 수행 (상태 계산은 조회 시 동적 반영)
     */
    @Transactional
    public void approveStudyCreation(Long studyId, Long adminId) {
        log.info("Command: Approving study creation - studyId: {} by admin: {}", studyId, adminId);
        studyJpaRepository.findById(studyId).ifPresent(entity -> {
            try {
                gracePeriodService.extendGracePeriodForApprovedStudy(
                        entity.getId(), entity.getStartedAt(), entity.getEndedAt());
            } catch (Exception e) {
                log.warn("Failed to extend grace period on study creation approve - studyId: {}", studyId, e);
            }
        });
    }

    /**
     * 스터디 생성 거절: 소프트 삭제 처리 (deleted_at 설정)
     */
    @Transactional
    public void rejectStudyCreation(Long studyId, Long adminId) {
        log.info("Command: Rejecting study creation - studyId: {} by admin: {}", studyId, adminId);
        studyJpaRepository.bulkSoftDeleteById(studyId, OffsetDateTime.now());
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