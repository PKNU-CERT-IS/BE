package org.certis.studyplatform.project.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.project.application.object.command.CreateProjectMeetingCommand;
import org.certis.studyplatform.project.application.object.command.DeleteProjectMeetingCommand;
import org.certis.studyplatform.project.application.object.command.UpdateProjectMeetingCommand;
import org.certis.studyplatform.project.application.object.query.GetAllProjectMeetingsQuery;
import org.certis.studyplatform.project.application.object.query.GetProjectMeetingByIdQuery;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingLinkCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingLinkQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.certis.studyplatform.project.domain.vo.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Project Meeting Domain Service
 *
 * ✅ CQRS 엄격 적용 및 링크 정보 포함된 응답 처리
 * ✅ 권한 체크 로직 추가
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMeetingDomainService {

    private final ProjectMeetingCommandRepository projectMeetingCommandRepository;
    private final ProjectMeetingQueryRepository projectMeetingQueryRepository;
    private final ProjectMeetingLinkCommandRepository projectMeetingLinkCommandRepository;
    private final ProjectMeetingLinkQueryRepository projectMeetingLinkQueryRepository;
    private final ProjectParticipantQueryRepository projectParticipantQueryRepository;
    private final ProjectQueryRepository projectQueryRepository;

    /**
     * 프로젝트 회의록 생성
     */
    public ProjectMeetingCreatedVo createProjectMeeting(CreateProjectMeetingCommand command) {
        log.info("MeetingDomain: Creating project meeting - projectId: {}, writerId: {}, title: {}",
                command.projectId(), command.writerId(), command.title());

        // 프로젝트 참여 권한 체크 (필요시 추가)
        validateProjectAccess(command.projectId(), command.writerId());

        ProjectMeetingVo meetingVo = ProjectMeetingVo.of(
                null,
                command.projectId(),
                command.title(),
                command.content(),
                command.participantNumber(),
                command.writerId(),
                true,
                null,
                null
        );

        ProjectMeetingCreatedVo createdVo = projectMeetingCommandRepository.save(meetingVo);

        if (command.links() != null && !command.links().isEmpty()) {
            for (var link : command.links()) {
                ProjectMeetingLinkVo linkVo = ProjectMeetingLinkVo.forCreation(
                        createdVo.projectId(),
                        createdVo.writerId(),
                        link.getTitle(),
                        link.getUrl()
                );
                projectMeetingLinkCommandRepository.save(linkVo);
                log.info("MeetingDomain: Project meeting link saved - Title: {}, URL: {}", link.getTitle(), link.getUrl());
            }
        }

        log.info("MeetingDomain: Project meeting created successfully - ID: {}", createdVo.id());
        return createdVo;
    }

    /**
     * 프로젝트 회의록 수정
     */
    public ProjectMeetingUpdatedVo updateProjectMeeting(UpdateProjectMeetingCommand command) {
        log.info("MeetingDomain: Updating project meeting - meetingId: {}, requesterId: {}",
                command.meetingId(), command.requesterId());

        ProjectMeetingVo existingMeeting = projectMeetingQueryRepository.findById(command.meetingId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_INFRASTRUCTURE_NOT_FOUND, "회의록을 찾을 수 없습니다"));

        // 권한 체크: 작성자만 수정 가능
        validateWriterPermission(existingMeeting.writerId(), command.requesterId(), "회의록을 수정할 권한이 없습니다");

        ProjectMeetingVo updatedMeetingVo = ProjectMeetingVo.of(
                command.meetingId(),
                existingMeeting.projectId(),
                command.title(),
                command.content(),
                command.participantNumber(),
                existingMeeting.writerId(),
                true,
                existingMeeting.createdAt(),
                null
        );

        ProjectMeetingUpdatedVo updatedVo = projectMeetingCommandRepository.update(updatedMeetingVo);

        if (command.links() != null) {
            projectMeetingLinkCommandRepository.deleteByProjectId(existingMeeting.projectId());

            if (!command.links().isEmpty()) {
                for (var link : command.links()) {
                    ProjectMeetingLinkVo linkVo = ProjectMeetingLinkVo.forCreation(
                            existingMeeting.projectId(),
                            command.requesterId(),
                            link.getTitle(),
                            link.getUrl()
                    );
                    projectMeetingLinkCommandRepository.save(linkVo);
                    log.info("MeetingDomain: Project meeting link updated - Title: {}, URL: {}", link.getTitle(), link.getUrl());
                }
            }
        }

        log.info("MeetingDomain: Project meeting updated successfully - ID: {}", updatedVo.id());
        return updatedVo;
    }

    /**
     * 프로젝트 회의록 삭제
     */
    public void deleteProjectMeeting(DeleteProjectMeetingCommand command) {
        log.info("MeetingDomain: Deleting project meeting - meetingId: {}, requesterId: {}",
                command.meetingId(), command.requesterId());

        ProjectMeetingVo existingMeeting = projectMeetingQueryRepository.findById(command.meetingId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_INFRASTRUCTURE_NOT_FOUND, "회의록을 찾을 수 없습니다"));

        // 권한 체크: 작성자만 삭제 가능
        validateWriterPermission(existingMeeting.writerId(), command.requesterId(), "회의록을 삭제할 권한이 없습니다");

        projectMeetingLinkCommandRepository.deleteByProjectId(existingMeeting.projectId());
        log.info("MeetingDomain: Project meeting links deleted - projectId: {}", existingMeeting.projectId());

        projectMeetingCommandRepository.deleteByIdWithPermission(command.meetingId(), command.requesterId());

        log.info("MeetingDomain: Project meeting deleted successfully - ID: {}", command.meetingId());
    }

    /**
     * 프로젝트 회의록 상세 조회 (링크 포함)
     */
    public ProjectMeetingDetailVo getProjectMeetingById(GetProjectMeetingByIdQuery query) {
        log.info("MeetingDomain: Getting project meeting by ID - meetingId: {}",
                query.meetingId());

        // 회의록 기본 정보 조회
        ProjectMeetingVo meetingVo = projectMeetingQueryRepository.findById(query.meetingId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_INFRASTRUCTURE_NOT_FOUND, "회의록을 찾을 수 없습니다"));

        // 해당 프로젝트의 모든 링크 조회
        List<ProjectMeetingLinkVo> links = projectMeetingLinkQueryRepository.findByProjectId(meetingVo.projectId());
        log.info("MeetingDomain: Found {} links for meeting - meetingId: {}", links.size(), query.meetingId());

        // 링크 정보를 포함한 상세 VO 생성
        ProjectMeetingDetailVo detailVo = ProjectMeetingDetailVo.from(meetingVo, links);

        log.info("MeetingDomain: Project meeting detail retrieved successfully - ID: {}, Links: {}",
                detailVo.id(), detailVo.getLinkCount());
        return detailVo;
    }

    /**
     * 프로젝트 회의록 전체 목록 조회 (링크 개수 포함)
     */
    public ProjectMeetingPageResultVo getAllProjectMeetings(GetAllProjectMeetingsQuery query) {
        log.info("MeetingDomain: Getting all project meetings - projectId: {}",
                query.projectId());


        // 회의록 목록 조회 (페이징)
        Page<ProjectMeetingSummaryVo> meetings = projectMeetingQueryRepository.findByProjectId(
                query.projectId(), query.pageable());

        // 해당 프로젝트의 모든 링크 조회
        List<ProjectMeetingLinkVo> allLinks = projectMeetingLinkQueryRepository.findByProjectId(query.projectId());
        log.info("MeetingDomain: Found {} total links for project - projectId: {}", allLinks.size(), query.projectId());

        // 링크 정보를 포함한 페이지 결과 생성
        ProjectMeetingPageResultVo result = ProjectMeetingPageResultVo.from(meetings, allLinks);

        log.info("MeetingDomain: Found {} project meetings with {} total links (avg: {:.1f} links per meeting)",
                meetings.getTotalElements(), result.totalLinkCount(), result.getAverageLinkCount());
        return result;
    }

    /**
     * 작성자 권한 검증
     */
    private void validateWriterPermission(Long writerId, Long requesterId, String errorMessage) {
        if (!writerId.equals(requesterId)) {
            log.warn("Permission denied - writerId: {}, requesterId: {}", writerId, requesterId);
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_PERMISSION_DENINED, errorMessage);
        }
        log.debug("Writer permission validated - writerId: {}, requesterId: {}", writerId, requesterId);
    }

    /**
     * 프로젝트 접근 권한 검증
     * 프로젝트의 승인된 멤버만 접근 가능
     */
    private void validateProjectAccess(Long projectId, Long requesterId) {
        if (projectId == null || requesterId == null) {
            log.warn("Invalid project access parameters - projectId: {}, requesterId: {}", projectId, requesterId);
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_PERMISSION_DENINED, 
                    "프로젝트 접근 권한이 없습니다.");
        }

        // 프로젝트 생성자는 항상 접근 가능
        try {
            var projectVoOptional = projectQueryRepository.findById(projectId);
            if (projectVoOptional.isEmpty()) {
                log.warn("Project not found - projectId: {}", projectId);
                throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND, "프로젝트를 찾을 수 없습니다");
            }
            if (requesterId.equals(projectVoOptional.get().creatorId())) {
                log.debug("Project access granted - requester is project creator: projectId: {}, requesterId: {}", projectId, requesterId);
                return;
            }
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Project lookup failed during access validation - projectId: {}. Proceeding to participant check.", projectId);
        }

        // 프로젝트의 승인된 멤버인지 확인
        boolean isApprovedMember = projectParticipantQueryRepository.isApprovedMember(projectId, requesterId);
        
        if (!isApprovedMember) {
            log.warn("Project access denied - projectId: {}, requesterId: {} (not an approved member)", 
                    projectId, requesterId);
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_PERMISSION_DENINED, 
                    "프로젝트의 승인된 멤버만 접근할 수 있습니다.");
        }

        log.debug("Project access validated - projectId: {}, requesterId: {}", projectId, requesterId);
    }
}