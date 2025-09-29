package org.certis.studyplatform.study.domain.repository;

import org.certis.studyplatform.study.domain.vo.StudySearchCriteriaVo;
import org.certis.studyplatform.study.domain.vo.StudySearchResultVo;
import org.certis.studyplatform.study.domain.vo.StudySummaryVo;
import org.certis.studyplatform.study.domain.vo.StudyEndSubmissionInfoVo;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.study.domain.vo.StudyAttachedVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Study Query Repository Interface
 *
 * CQRS Query 측면의 Repository (Read 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 jOOQ로 구현
 *
 * ✅ CQRS 패턴 준수:
 * - 모든 조회 관련 메서드 포함
 * - Command 작업 시 필요한 검증용 조회 메서드도 포함
 */
public interface StudyQueryRepository {

    /**
     * 스터디 상세 조회
     *
     * @param studyId 조회할 스터디 ID
     * @return 스터디 상세 정보 (StudyVo)
     */
    Optional<StudyVo> findStudyDetailById(Long studyId);

    /**
     * 스터디 목록 조회 (페이징)
     *
     * @param criteria 검색 조건
     * @param pageable 페이징 정보
     * @return 스터디 검색 결과
     */
    StudySearchResultVo findStudies(StudySearchCriteriaVo criteria, Pageable pageable);

    /**
     * 회원이 생성한 스터디 목록 조회
     *
     * @param memberId 생성자 ID
     * @param pageable 페이징 정보
     * @return 스터디 검색 결과
     */
    StudySearchResultVo findStudiesByMemberId(Long memberId, Pageable pageable);

    /**
     * 카테고리별 스터디 목록 조회
     *
     * @param category 카테고리
     * @param pageable 페이징 정보
     * @return 스터디 검색 결과
     */
    StudySearchResultVo findStudiesByCategory(String category, Pageable pageable);

    /**
     * 진행 중인 스터디 목록 조회
     *
     * @param pageable 페이징 정보
     * @return 스터디 검색 결과
     */
    StudySearchResultVo findActiveStudies(Pageable pageable);

    /**
     * 키워드로 스터디 검색
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 스터디 검색 결과
     */
    StudySearchResultVo findStudiesByKeyword(String keyword, Pageable pageable);

    /**
     * 스킬로 스터디 검색
     *
     * @param skills 스킬 목록
     * @param pageable 페이징 정보
     * @return 스터디 검색 결과
     */
    StudySearchResultVo findStudiesBySkills(List<String> skills, Pageable pageable);

    // ================= Domain Service 지원 메소드 =================

    /**
     * ✅ 스터디 단건 조회 (Domain Service용)
     *
     * @param studyId 스터디 ID
     * @return StudyVo
     */
    Optional<StudyVo> findById(Long studyId);

    /**
     * APPROVED 상태이면서 started_at이 지정된 시간 이전인 스터디 ID 목록 조회
     * 
     * @param currentTime 현재 시간
     * @return 스터디 ID 목록
     */
    List<Long> findApprovedStudiesStartedBefore(OffsetDateTime currentTime);

    /**
     * 스터디 엔티티 직접 조회 (상태 업데이트용)
     * 
     * @param studyId 스터디 ID
     * @return 스터디 엔티티
     */
    java.util.Optional<org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity> findEntityById(Long studyId);


    /**
     * ✅ 스터디 종료되지 않은 스터디 조회 (Domain Service용)
     *
     * @param studyId 스터디 ID
     * @return StudyVo
     */
    Optional<StudyVo> findByIdAndDeletedAtIsNull(Long studyId);

    /**
     * ✅ 스터디 제목 존재 여부 확인
     *
     * @param title 스터디 제목
     * @return 존재 여부
     */
    boolean existsByTitle(String title);

    /**
     * ✅ 스터디 제목 중복 확인 (자신 제외)
     *
     * @param title 스터디 제목
     * @param studyId 제외할 스터디 ID
     * @return 중복 여부
     */
    boolean existsByTitleAndIdNot(String title, Long studyId);

    /**
     * 특정 멤버가 생성한 완료된 스터디 목록 조회 (페이징)
     * 완료 조건: ended_at < 현재시간 AND deleted_at IS NULL AND member_id = ?
     */
    Page<StudySummaryVo> findCompletedStudiesByMember(Long memberId, Pageable pageable);


    /**
     * 특정 멤버가 생성한 완료된 스터디 목록 조회 (전체)
     */
    List<StudySummaryVo> findCompletedStudiesListByMember(Long memberId);
    Optional<StudyEndSubmissionInfoVo> getEndSubmissionInfo(Long studyId);

    /**
     * 종료 제출 상태가 INPROGRESS인 스터디 목록 조회 (관리자용)
     */
    java.util.List<StudyEndSubmissionInfoVo> findEndSubmissionsInProgress();

    /**
     * 스터디 첨부파일 조회
     *
     * @param studyId 조회할 스터디 ID
     * @return 첨부파일 VO 목록
     */
    List<StudyAttachedVo> findAttachmentsByStudyId(Long studyId);
}