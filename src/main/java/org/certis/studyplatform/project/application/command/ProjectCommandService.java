package org.certis.studyplatform.project.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.application.object.command.CreateProjectCommand;
import org.certis.studyplatform.project.application.object.command.DeleteProjectCommand;
import org.certis.studyplatform.project.application.object.command.EndProjectCommand;
import org.certis.studyplatform.project.application.object.command.UpdateProjectCommand;
import org.certis.studyplatform.member.application.GracePeriodService;
import org.certis.studyplatform.project.domain.service.ProjectDomainService;
import org.certis.studyplatform.project.domain.service.ProjectParticipantDomainService;
import org.certis.studyplatform.project.domain.vo.*;
import org.certis.studyplatform.shared.service.S3FileService;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectAttachedEntity;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectAttachedJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;

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
    private final S3FileService s3FileService;
    private final GracePeriodService gracePeriodService;
    private final ProjectAttachedJpaRepository projectAttachedJpaRepository;

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

        // Command 객체를 Domain Service로 전달
        ProjectVo createdVo = projectDomainService.createProject(command);

        // 첨부파일 저장 (생성 시 첨부가 포함된 경우)
        if (command.attachedFiles() != null && !command.attachedFiles().isEmpty()) {
            for (var file : command.attachedFiles()) {
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

        // Command 객체를 Domain Service로 전달
        ProjectVo updatedVo = projectDomainService.updateProject(command);

        // 첨부파일 저장 (요청에 첨부가 포함된 경우 추가)
        if (command.attachedFiles() != null && !command.attachedFiles().isEmpty()) {
            for (var file : command.attachedFiles()) {
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

        // Command 객체를 Domain Service로 전달 (파일명은 Domain Service에서 처리)
        ProjectVo endedVo = projectDomainService.endProject(command);

        // 조기 종료 시 유예기간 재조정
        log.info("Command: Checking for early termination - project ID: {}, startDate: {}, endDate: {}", 
            endedVo.id(), endedVo.startDate(), endedVo.endDate());
        
        if (endedVo.startDate() != null && endedVo.endDate() != null) {
            // 원래 예정된 종료일과 실제 종료일을 비교하여 조기 종료인지 확인
            OffsetDateTime originalEndDate = endedVo.startDate().plusWeeks(4); // 기본 4주 프로젝트 가정
            OffsetDateTime actualEndDate = endedVo.endDate();
            
            log.info("Command: Early termination check - originalEndDate: {}, actualEndDate: {}, isEarly: {}", 
                originalEndDate, actualEndDate, actualEndDate.isBefore(originalEndDate));
            
            // 실제 종료일이 원래 예정일보다 빠른 경우 조기 종료로 간주
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
        } else {
            log.warn("Command: Cannot check early termination - missing dates for project ID: {}", endedVo.id());
        }

        // 파일 업로드 (있는 경우) - Domain Service에서 프로젝트 정보를 가져온 후 처리
        if (command.files() != null && !command.files().isEmpty()) {
            for (MultipartFile file : command.files()) {
                if (!file.isEmpty()) {
                    try {
                        // 파일명 생성: 프로젝트제목_ID_생성자이름_원본파일명
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
                        
                        String fileUrl = s3FileService.uploadFileWithCustomName(file, "project-end-attachments", customFilename);
                        log.info("Project end attachment uploaded successfully: {}", fileUrl);
                    } catch (Exception e) {
                        log.error("Failed to upload project end attachment: {}", file.getOriginalFilename(), e);
                        throw new RuntimeException("프로젝트 종료 첨부파일 업로드에 실패했습니다: " + file.getOriginalFilename(), e);
                    }
                }
            }
        }

        log.info("Command: Project ended successfully - ID: {}", endedVo.id());
        return endedVo;
    }

    /**
     * 파일명에서 특수문자 제거 및 안전한 파일명 생성
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unknown";
        return filename.replaceAll("[^a-zA-Z0-9가-힣]", "_")
                      .replaceAll("_{2,}", "_")
                      .replaceAll("^_|_$", "");
    }
}