package org.certis.studyplatform.study.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.study.application.object.command.CreateStudyMeetingCommand;
import org.certis.studyplatform.study.application.object.command.DeleteStudyMeetingCommand;
import org.certis.studyplatform.study.application.object.command.UpdateStudyMeetingCommand;
import org.certis.studyplatform.study.application.object.query.GetAllStudyMeetingsQuery;
import org.certis.studyplatform.study.application.object.query.GetStudyMeetingByIdQuery;
import org.certis.studyplatform.study.domain.repository.StudyMeetingCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyMeetingLinkCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyMeetingLinkQueryRepository;
import org.certis.studyplatform.study.domain.repository.StudyMeetingQueryRepository;
import org.certis.studyplatform.study.domain.repository.StudyParticipantQueryRepository;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.vo.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Study Meeting Domain Service
 *
 * ✅ CQRS 엄격 적용 및 링크 정보 포함된 응답 처리
 * ✅ 권한 체크 로직 추가
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyMeetingDomainService {

    private final StudyMeetingCommandRepository studyMeetingCommandRepository;
    private final StudyMeetingQueryRepository studyMeetingQueryRepository;
    private final StudyMeetingLinkCommandRepository studyMeetingLinkCommandRepository;
    private final StudyMeetingLinkQueryRepository studyMeetingLinkQueryRepository;
    private final StudyParticipantQueryRepository studyParticipantQueryRepository;
    private final StudyQueryRepository studyQueryRepository;

    /**
     * 스터디 회의록 생성
     */
    public StudyMeetingCreatedVo createStudyMeeting(CreateStudyMeetingCommand command) {
        log.info("MeetingDomain: Creating study meeting - studyId: {}, writerId: {}, title: {}",
                command.studyId(), command.writerId(), command.title());

        // 스터디 참여 권한 체크 (필요시 추가)
        validateStudyAccess(command.studyId(), command.writerId());

        StudyMeetingVo meetingVo = StudyMeetingVo.of(
                null,
                command.studyId(),
                command.title(),
                command.content(),
                command.participantNumber(),
                command.writerId(),
                true,
                null,
                null
        );

        StudyMeetingCreatedVo createdVo = studyMeetingCommandRepository.save(meetingVo);

        if (command.links() != null && !command.links().isEmpty()) {
            for (var link : command.links()) {
                StudyMeetingLinkVo linkVo = StudyMeetingLinkVo.forCreation(
                        createdVo.id(),
                        createdVo.writerId(),
                        link.getTitle(),
                        link.getUrl()
                );
                studyMeetingLinkCommandRepository.save(linkVo);
                log.info("MeetingDomain: Study meeting link saved - Title: {}, URL: {}", link.getTitle(), link.getUrl());
            }
        }

        log.info("MeetingDomain: Study meeting created successfully - ID: {}", createdVo.id());
        return createdVo;
    }

    /**
     * 스터디 회의록 수정
     */
    public StudyMeetingUpdatedVo updateStudyMeeting(UpdateStudyMeetingCommand command) {
        log.info("MeetingDomain: Updating study meeting - meetingId: {}, requesterId: {}",
                command.meetingId(), command.requesterId());

        StudyMeetingVo existingMeeting = studyMeetingQueryRepository.findById(command.meetingId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_INFRASTRUCTURE_NOT_FOUND, "회의록을 찾을 수 없습니다"));

        // 권한 체크: 작성자만 수정 가능
        validateWriterPermission(existingMeeting.writerId(), command.requesterId(), "회의록을 수정할 권한이 없습니다");

        StudyMeetingVo updatedMeetingVo = StudyMeetingVo.of(
                command.meetingId(),
                existingMeeting.studyId(),
                command.title(),
                command.content(),
                command.participantNumber(),
                existingMeeting.writerId(),
                true,
                existingMeeting.createdAt(),
                null
        );

        StudyMeetingUpdatedVo updatedVo = studyMeetingCommandRepository.update(updatedMeetingVo);

        if (command.links() != null) {
            // 링크 전체 교체: 해당 회의(meetingId)의 기존 링크 모두 삭제 후, 전달된 링크를 전부 추가
            studyMeetingLinkCommandRepository.deleteByMeetingId(existingMeeting.id());

            if (!command.links().isEmpty()) {
                for (var link : command.links()) {
                    StudyMeetingLinkVo linkVo = StudyMeetingLinkVo.forCreation(
                            existingMeeting.id(),
                            command.requesterId(),
                            link.getTitle(),
                            link.getUrl()
                    );
                    studyMeetingLinkCommandRepository.save(linkVo);
                    log.info("MeetingDomain: Study meeting link updated - Title: {}, URL: {}", link.getTitle(), link.getUrl());
                }
            }
        }

        log.info("MeetingDomain: Study meeting updated successfully - ID: {}", updatedVo.id());
        return updatedVo;
    }

    /**
     * 스터디 회의록 삭제
     */
    public void deleteStudyMeeting(DeleteStudyMeetingCommand command) {
        log.info("MeetingDomain: Deleting study meeting - meetingId: {}, requesterId: {}",
                command.meetingId(), command.requesterId());

        StudyMeetingVo existingMeeting = studyMeetingQueryRepository.findById(command.meetingId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_INFRASTRUCTURE_NOT_FOUND, "회의록을 찾을 수 없습니다"));

        // 권한 체크: 작성자만 삭제 가능
        validateWriterPermission(existingMeeting.writerId(), command.requesterId(), "회의록을 삭제할 권한이 없습니다");

        studyMeetingLinkCommandRepository.deleteByMeetingId(existingMeeting.id());
        log.info("MeetingDomain: Study meeting links deleted - meetingId: {}", existingMeeting.id());

        studyMeetingCommandRepository.deleteByIdWithPermission(command.meetingId(), command.requesterId());

        log.info("MeetingDomain: Study meeting deleted successfully - ID: {}", command.meetingId());
    }

    /**
     * 스터디 회의록 상세 조회 (링크 포함)
     */
    public StudyMeetingDetailVo getStudyMeetingById(GetStudyMeetingByIdQuery query) {
        log.info("MeetingDomain: Getting study meeting by ID - meetingId: {}",
                query.meetingId());

        // 회의록 기본 정보 조회
        StudyMeetingVo meetingVo = studyMeetingQueryRepository.findById(query.meetingId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_INFRASTRUCTURE_NOT_FOUND, "회의록을 찾을 수 없습니다"));

        // 해당 미팅의 링크만 조회
        List<StudyMeetingLinkVo> links = studyMeetingLinkQueryRepository.findByMeetingId(meetingVo.id());
        log.info("MeetingDomain: Found {} links for meeting - meetingId: {}", links.size(), query.meetingId());

        // 링크 정보를 포함한 상세 VO 생성
        StudyMeetingDetailVo detailVo = StudyMeetingDetailVo.from(meetingVo, links);

        log.info("MeetingDomain: Study meeting detail retrieved successfully - ID: {}, Links: {}",
                detailVo.id(), detailVo.getLinkCount());
        return detailVo;
    }

    /**
     * 스터디 회의록 전체 목록 조회 (링크 개수 포함)
     */
    public StudyMeetingPageResultVo getAllStudyMeetings(GetAllStudyMeetingsQuery query) {
        log.info("MeetingDomain: Getting all study meetings - studyId: {}",
                query.studyId());

        // 회의록 목록 조회 (페이징)
        Page<StudyMeetingSummaryVo> meetings = studyMeetingQueryRepository.findByStudyId(
                query.studyId(), query.pageable());

        List<Long> meetingIds = meetings.getContent().stream()
                .map(StudyMeetingSummaryVo::id)
                .toList();

        // 현재 페이지의 회의록 ID들에 대해서만 링크를 일괄 조회한다.
        List<StudyMeetingLinkVo> allLinks = studyMeetingLinkQueryRepository.findByMeetingIds(meetingIds);
        log.info("MeetingDomain: Found {} total links for study - studyId: {}", allLinks.size(), query.studyId());

        // 링크 정보를 포함한 페이지 결과 생성
        StudyMeetingPageResultVo result = StudyMeetingPageResultVo.from(meetings, allLinks);

        log.info("MeetingDomain: Found {} study meetings with {} total links (avg: {:.1f} links per meeting)",
                meetings.getTotalElements(), result.totalLinkCount(), result.getAverageLinkCount());
        return result;
    }

    /**
     * 작성자 권한 검증
     */
    private void validateWriterPermission(Long writerId, Long requesterId, String errorMessage) {
        if (!writerId.equals(requesterId)) {
            log.warn("Permission denied - writerId: {}, requesterId: {}", writerId, requesterId);
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_UPDATE_UNAUTHORIZED, errorMessage);
        }
        log.debug("Writer permission validated - writerId: {}, requesterId: {}", writerId, requesterId);
    }

    /**
     * 스터디 접근 권한 검증
     * - 스터디 생성자 또는 승인된 참가자만 접근 가능
     */
    private void validateStudyAccess(Long studyId, Long requesterId) {
        if (studyId == null || requesterId == null) {
            log.warn("Invalid study access parameters - studyId: {}", studyId);
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED);
        }

        // 스터디 존재 및 생성자 확인
        var studyVoOptional = studyQueryRepository.findById(studyId);
        if (studyVoOptional.isEmpty()) {
            log.warn("Study not found - studyId: {}", studyId);
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND, "스터디를 찾을 수 없습니다");
        }

        if (requesterId.equals(studyVoOptional.get().creatorId())) {
            log.debug("Study access granted - requester is study creator: studyId: {}, requesterId: {}", studyId, requesterId);
            return;
        }

        // 승인된 참가자인지 확인
        boolean isApprovedMember = studyParticipantQueryRepository
                .findByStudyIdAndMemberId(studyId, requesterId)
                .map(org.certis.studyplatform.study.domain.vo.StudyParticipantVo::isApproved)
                .orElse(false);

        if (!isApprovedMember) {
            log.warn("Study access denied - studyId: {}, requesterId: {} (not an approved member)", studyId, requesterId);
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED, "스터디의 승인된 멤버만 접근할 수 있습니다.");
        }

        log.debug("Study access validated - studyId: {}, requesterId: {}", studyId, requesterId);
    }
}
