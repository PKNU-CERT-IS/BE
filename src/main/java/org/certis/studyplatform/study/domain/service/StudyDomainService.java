package org.certis.studyplatform.study.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.application.object.query.GetMemberByIdQuery;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.certis.studyplatform.study.application.object.command.CreateStudyCommand;
import org.certis.studyplatform.study.application.object.command.DeleteStudyCommand;
import org.certis.studyplatform.study.application.object.command.EndStudyCommand;
import org.certis.studyplatform.study.application.object.command.UpdateStudyCommand;
import org.certis.studyplatform.study.application.object.query.GetAllStudiesQuery;
import org.certis.studyplatform.study.application.object.query.GetCompletedStudiesByMemberQuery;
import org.certis.studyplatform.study.application.object.query.GetStudyByIdQuery;
import org.certis.studyplatform.study.application.object.query.SearchStudiesQuery;
import org.certis.studyplatform.study.domain.repository.StudyCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudySearchCriteriaVo;
import org.certis.studyplatform.study.domain.vo.StudySearchResultVo;
import org.certis.studyplatform.study.domain.vo.StudySummaryVo;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.certis.studyplatform.shared.service.S3FileService;
import org.certis.studyplatform.study.application.object.command.CreateStudyAttachedCommand;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.certis.studyplatform.study.application.object.command.CreateStudyAttachedCommand;

/**
 * Study Domain Service
 *
 * Clean Architecture Domain Layer
 * 스터디 비즈니스 로직 처리
 *
 * CQRS 패턴: Command/Query 객체를 받아서 VO를 생성하여 Repository로 전달
 * ✅ ReadModel 제거로 인한 단순화: Repository에서 직접 VO 반환
 * ✅ StudyVo 내부 검증 로직 활용: 도메인 객체가 자체 유효성을 보장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyDomainService {

    private final StudyCommandRepository commandRepository;
    private final StudyQueryRepository queryRepository;
    private final MemberDomainService memberDomainService;
    private final S3FileService s3FileService;

    // ================================================================
    // COMMAND OPERATIONS
    // ================================================================

    /**
     * 스터디 생성
     */
    public StudyVo createStudy(CreateStudyCommand command) {
        log.info("Domain: Creating study from command - {}", command.title());

//         [FIX] Add validation to ensure the creator exists before proceeding.
//        if (!memberQueryRepositoryImpl.existsById(command.creatorId())) {
//            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_NOT_FOUND,
//                    "스터디 생성자를 찾을 수 없습니다: " + command.creatorId());
//        }

        // 중복 검사 (Repository 의존성이 필요한 검증만 수행)
//        validateStudyTitleDuplication(command.title());

        // StudyVo.createNew() 사용 - 생성 시 자동으로 나머지 검증 수행
        StudyVo studyVo = StudyVo.createNew(
                command.title(),
                command.description(),
                command.content(),
                command.category(),
                command.subCategory(),
                command.startDate(),
                command.endDate(),
                command.creatorId(),
                null, // creatorName
                null, // creatorGrade
                command.maxParticipants()
        );

        // Command Repository를 통한 저장 (VO 전달)
        StudyVo savedStudyVo = commandRepository.save(studyVo);

        // 첨부파일은 CommandService에서 S3 업로드 선행 및 저장 처리함

        log.info("Domain: Study created successfully - ID: {}", savedStudyVo.id());

        return savedStudyVo;
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
     * 스터디 수정
     */
    public StudyVo updateStudy(UpdateStudyCommand command) {
        log.info("Domain: Updating study from command - ID: {}", command.id());

        // 기존 스터디 조회 (Repository에서 직접 StudyVo 반환)
        StudyVo existingStudy = queryRepository.findById(command.id())
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "스터디를 찾을 수 없습니다: " + command.id()));

        // 권한 검증: STAFF 이상이거나 작성자 본인인지 확인
        validateStudyUpdatePermission(command.requesterId(), existingStudy.creatorId());

        // 제목 중복 검사 (자신 제외)
        if (command.title() != null) {
            validateStudyTitleDuplicationForUpdate(command.title(), command.id());
        }

        // StudyVo.updateFrom() 사용 - 업데이트 시 자동으로 검증 수행
        StudyVo updatedStudyVo = StudyVo.updateFrom(
                existingStudy,
                command.title(),
                command.description(),
                command.content(),
                command.category(),
                command.subCategory(),
                command.startDate(),
                command.endDate(),
                command.maxParticipants()
        );

        // Command Repository를 통한 저장 (VO 전달)
        StudyVo savedStudyVo = commandRepository.save(updatedStudyVo);

        log.info("Domain: Study updated successfully - ID: {}", savedStudyVo.id());

        return savedStudyVo;
    }

    /**
     * 스터디 삭제
     */
    public void deleteStudy(DeleteStudyCommand command) {
        log.info("Domain: Deleting study from command - ID: {}", command.id());

        // 스터디 존재 여부 확인 (Repository에서 직접 StudyVo 반환)
        StudyVo existingStudy = queryRepository.findByIdAndDeletedAtIsNull(command.id())
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "스터디를 찾을 수 없습니다: " + command.id()));

        // 권한 검증: STAFF 이상이거나 작성자 본인인지 확인
        validateStudyDeletePermission(command.requesterId(), existingStudy.creatorId());

        // Command Repository를 통한 Soft Delete
        commandRepository.deleteById(existingStudy.id());

        log.info("Domain: Study deleted successfully - ID: {}", existingStudy.id());
    }

    /**
     * 스터디 첨부파일 업로드
     */
    public String uploadStudyAttachment(Long studyId, Long memberId, MultipartFile file) {
        log.info("Domain: Uploading study attachment for study ID: {}, member ID: {}", studyId, memberId);

        // S3에 첨부파일 업로드
        String attachmentUrl = commandRepository.uploadStudyAttachment(studyId, memberId, file);

        log.info("Domain: Study attachment uploaded successfully for study ID: {}, member ID: {}, URL: {}", studyId, memberId, attachmentUrl);
        return attachmentUrl;
    }

    /**
     * 스터디 첨부파일 업데이트 (위임: Infrastructure Repository)
     */
    public void updateStudyAttachments(Long studyId, Long requesterId, List<CreateStudyAttachedCommand> attachments) {
        commandRepository.updateStudyAttachments(studyId, requesterId, attachments);
    }

    // ================================================================
    // QUERY OPERATIONS
    // ================================================================

    /**
     * 스터디 단건 조회
     */
    public StudyVo getStudyById(GetStudyByIdQuery query) {
        log.info("Domain: Getting study from query - ID: {}", query.id());

        // ✅ Repository에서 직접 StudyVo 반환 (ReadModel 변환 불필요)
        StudyVo studyVo = queryRepository.findStudyDetailById(query.id())
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "스터디를 찾을 수 없습니다: " + query.id()));

        log.info("Domain: Study found - ID: {}", studyVo.id());
        return studyVo;
    }

    /**
     * 전체 스터디 조회 (페이징)
     */
    public Page<StudySummaryVo> getAllStudies(GetAllStudiesQuery query) {
        log.info("Domain: Getting all studies from query");

        StudySearchCriteriaVo emptyCriteria = StudySearchCriteriaVo.empty();
        StudySearchResultVo result = queryRepository.findStudies(emptyCriteria, query.pageable());

        // ✅ Repository에서 직접 StudySummaryVo 반환 (변환 불필요)
        Page<StudySummaryVo> studyPage = new PageImpl<>(result.studies(), query.pageable(), result.totalElements());

        log.info("Domain: Found {} studies", studyPage.getTotalElements());
        return studyPage;
    }

    /**
     * 복합 검색 조건으로 스터디 검색 (고급 검색 지원)
     */
    public Page<StudySummaryVo> searchStudiesByCriteria(SearchStudiesQuery query) {
        log.info("Domain: Searching studies from query - keyword: {}, category: {}, semester: {}, status: {}",
                query.keyword(), query.category(), query.semester(), query.status());

        // Query를 StudySearchCriteria로 변환 (고급 검색 필드 포함)
        StudySearchCriteriaVo criteria = StudySearchCriteriaVo.ofAdvanced(
                query.keyword(),
                query.category(),
                query.subCategory(),
                query.semester(),
                query.status()
        );

        StudySearchResultVo result = queryRepository.findStudies(criteria, query.pageable());

        // ✅ Repository에서 직접 StudySummaryVo 반환 (변환 불필요)
        Page<StudySummaryVo> studyPage = new PageImpl<>(result.studies(), query.pageable(), result.totalElements());

        log.info("Domain: Found {} studies by advanced criteria", studyPage.getTotalElements());
        return studyPage;
    }

    /**
     * 특정 멤버가 생성한 완료된 스터디 목록 조회
     * 완료 조건: endedAt이 현재 시간보다 이전이고, 삭제되지 않은 스터디
     */
    public Page<StudySummaryVo> getCompletedStudiesByMember(GetCompletedStudiesByMemberQuery query) {
        log.info("Domain: Getting completed studies by member - memberId: {}", query.memberId());

        try {
            // Repository에서 완료된 스터디 조회
            Page<StudySummaryVo> completedStudies = queryRepository.findCompletedStudiesByMember(
                    query.memberId(),
                    query.pageable()
            );

            log.info("Domain: Found {} completed studies for member: {}",
                    completedStudies.getTotalElements(), query.memberId());

            return completedStudies;

        } catch (Exception e) {
            log.error("Domain: Error getting completed studies for member: {}", query.memberId(), e);
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND);
        }
    }

    /**
     * 특정 멤버가 생성한 완료된 스터디 목록 조회 (리스트 버전)
     * 페이징 없이 전체 조회
     */
    public List<StudySummaryVo> getCompletedStudiesListByMember(Long memberId) {
        log.info("Domain: Getting completed studies list by member - memberId: {}", memberId);

        try {
            List<StudySummaryVo> completedStudies = queryRepository.findCompletedStudiesListByMember(memberId);

            log.info("Domain: Found {} completed studies for member: {}", completedStudies.size(), memberId);

            return completedStudies;

        } catch (Exception e) {
            log.error("Domain: Error getting completed studies list for member: {}", memberId, e);
            return List.of(); // 실패시 빈 리스트 반환
        }
    }


    // ================================================================
    // PRIVATE VALIDATION METHODS (Repository 의존성이 필요한 검증만)
    // ================================================================

    /**
     * 스터디 생성자 존재 확인
     */
    private void validateCreatorExists(Long creatorId) {
        try {
            memberDomainService.getMemberVo(new GetMemberByIdQuery(creatorId));
            log.debug("Domain: Study creator validation passed - {}", creatorId);
        } catch (DomainException e) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_INVALID_CREATOR,
                    "스터디 생성자를 찾을 수 없습니다: " + creatorId);
        }
    }

    /**
     * 스터디 수정 권한 검증
     * STAFF 이상의 관리자이거나 스터디 생성자 본인만 수정 가능
     */
    private void validateStudyUpdatePermission(Long requesterId, Long studyCreatorId) {
        log.debug("Domain: Validating study update permission - requesterId: {}, creatorId: {}",
                requesterId, studyCreatorId);

        // 작성자 본인인 경우 수정 허용
        if (requesterId.equals(studyCreatorId)) {
            log.debug("Domain: Update permission granted - requester is study creator");
            return;
        }

        // STAFF 이상 관리자 권한 확인
        try {
            MemberVo requesterMember = memberDomainService.getMemberVo(new GetMemberByIdQuery(requesterId));
            MemberRole requesterRole = requesterMember.role();

            if (MemberRole.isStaffOrAbove(requesterRole)) {
                log.debug("Domain: Update permission granted - requester is staff or above: {}", requesterRole);
                return;
            }
        } catch (DomainException e) {
            log.warn("Domain: Requester not found: {}", requesterId);
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_ACCESS_DENIED,
                    "스터디를 수정할 권한이 없습니다");
        }

        // 권한이 없는 경우
        log.warn("Domain: Update permission denied - requesterId: {}, creatorId: {}", requesterId, studyCreatorId);
        throw new DomainException(ExceptionStatus.STUDY_DOMAIN_ACCESS_DENIED,
                "스터디를 수정할 권한이 없습니다");
    }

    /**
     * 스터디 삭제 권한 검증
     * STAFF 이상의 관리자이거나 스터디 생성자 본인만 삭제 가능
     */
    private void validateStudyDeletePermission(Long requesterId, Long studyCreatorId) {
        log.debug("Domain: Validating study delete permission - requesterId: {}, creatorId: {}",
                requesterId, studyCreatorId);

        // 작성자 본인인 경우 삭제 허용
        if (requesterId.equals(studyCreatorId)) {
            log.debug("Domain: Delete permission granted - requester is study creator");
            return;
        }

        // STAFF 이상 관리자 권한 확인
        try {
            MemberVo requesterMember = memberDomainService.getMemberVo(new GetMemberByIdQuery(requesterId));
            MemberRole requesterRole = requesterMember.role();

            if (MemberRole.isStaffOrAbove(requesterRole)) {
                log.debug("Domain: Delete permission granted - requester is staff or above: {}", requesterRole);
                return;
            }
        } catch (DomainException e) {
            log.warn("Domain: Requester not found: {}", requesterId);
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_ACCESS_DENIED,
                    "스터디를 삭제할 권한이 없습니다");
        }

        // 권한이 없는 경우
        log.warn("Domain: Delete permission denied - requesterId: {}, creatorId: {}", requesterId, studyCreatorId);
        throw new DomainException(ExceptionStatus.STUDY_DOMAIN_ACCESS_DENIED,
                "스터디를 삭제할 권한이 없습니다");
    }


    /**
     * 스터디 제목 중복 검증 (생성 시)
     */
    private void validateStudyTitleDuplication(String title) {
        if (queryRepository.existsByTitle(title)) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_INVALID_TITLE,
                    "이미 존재하는 스터디 제목입니다: " + title);
        }
        log.debug("Domain: Study title duplication validation passed - {}", title);
    }

    /**
     * 스터디 종료
     */
    public StudyVo endStudy(EndStudyCommand command) {
        log.info("Domain: Ending study from command - ID: {}", command.studyId());

        // 기존 스터디 조회
        StudyVo existingStudy = queryRepository.findById(command.studyId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "스터디를 찾을 수 없습니다: " + command.studyId()));

        // 권한 검증: STAFF 이상이거나 스터디 생성자인지 확인
        validateStudyEndPermission(command.requesterId(), existingStudy.creatorId());

        // 이미 종료된 스터디인지 검증
        if (existingStudy.endDate() != null && existingStudy.endDate().isBefore(OffsetDateTime.now())) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION,
                    "이미 종료된 스터디입니다");
        }

        // 제출 단계: 종료는 승인 시 처리. 여기서는 변경 없이 반환.
        log.info("Domain: Study end submission initiated - ID: {}", existingStudy.id());
        return existingStudy;
    }

    // ===== Commands previously in CommandService moved behind command repository =====
    public void updateResultSubmission(Long studyId, OffsetDateTime submittedAt,
                                       ResultSubmitStatus status,
                                       String attachmentUrl) {
        commandRepository.updateResultSubmission(studyId, submittedAt, status, attachmentUrl);
    }

    public void approveEnd(Long studyId, OffsetDateTime endedAt,
                           ResultSubmitStatus status) {
        commandRepository.approveEnd(studyId, endedAt, status);
    }

    public void rejectEnd(Long studyId, ResultSubmitStatus status,
                          OffsetDateTime now) {
        commandRepository.rejectEnd(studyId, status, now);
    }

    public void bulkSoftDeleteById(Long studyId, OffsetDateTime deletedAt) {
        commandRepository.bulkSoftDeleteById(studyId, deletedAt);
    }

    public java.util.Optional<String> getResultAttachmentUrlById(Long studyId) {
        return commandRepository.getResultAttachmentUrlById(studyId);
    }

    /**
     * 종료 제출 정보 조회 (계층: Domain -> QueryRepository)
     */
    public org.certis.studyplatform.study.domain.vo.StudyEndSubmissionInfoVo getEndSubmissionInfo(Long studyId) {
        return queryRepository.getEndSubmissionInfo(studyId)
                .orElse(new org.certis.studyplatform.study.domain.vo.StudyEndSubmissionInfoVo(
                        studyId,
                        null, // status
                        null, // submittedAt
                        null, // attachmentUrl
                        null, // category
                        null, // subCategory
                        null, // title
                        null, // description
                        null, // creatorId
                        null, // creatorName
                        null, // creatorGrade
                        null, // startedAt
                        null, // endedAt
                        null, // currentParticipantNumber
                        null  // maxParticipantNumber
                ));
    }

    public java.util.List<org.certis.studyplatform.study.domain.vo.StudyEndSubmissionInfoVo> getEndSubmissionsInProgress() {
        return queryRepository.findEndSubmissionsInProgress();
    }

    /**
     * 스터디 종료 권한 검증
     */
    private void validateStudyEndPermission(Long requesterId, Long creatorId) {
        // 요청자 정보 조회
        MemberVo requester = memberDomainService.getMemberVo(new GetMemberByIdQuery(requesterId));
        
        // STAFF 이상이거나 스터디 생성자인지 확인
        if (!MemberRole.isStaffOrAbove(requester.role()) && !requesterId.equals(creatorId)) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION,
                    "스터디 종료 권한이 없습니다. 스터디 생성자이거나 STAFF 이상이어야 합니다.");
        }
        
        log.debug("Domain: Study end permission validation passed - requesterId: {}, creatorId: {}", 
                requesterId, creatorId);
    }

    /**
     * 스터디 제목 중복 검증 (수정 시 - 자신 제외)
     */
    private void validateStudyTitleDuplicationForUpdate(String title, Long studyId) {
        if (queryRepository.existsByTitleAndIdNot(title, studyId)) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_INVALID_TITLE,
                    "이미 존재하는 스터디 제목입니다: " + title);
        }
        log.debug("Domain: Study title duplication validation passed for update - {}", title);
    }
}