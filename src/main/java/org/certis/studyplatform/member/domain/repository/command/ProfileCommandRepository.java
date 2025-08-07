package org.certis.studyplatform.member.domain.repository.command;

import org.certis.studyplatform.member.domain.Profile;
import java.util.Optional;

/**
 * Profile Command Repository Interface
 *
 * Clean Architecture Domain Layer의 Repository 인터페이스
 * 프로필 도메인의 쓰기 작업(Command)을 담당
 *
 * 특징:
 * - Domain Layer에 위치 (Infrastructure를 모름)
 * - 순수 도메인 객체만 사용
 * - Infrastructure Layer에서 구현
 * - JPA 등 ORM 기술로 구현 예정
 */
public interface ProfileCommandRepository {

    /**
     * 프로필 저장 (생성/수정)
     *
     * @param profile 저장할 프로필 도메인 객체
     * @return 저장된 프로필 도메인 객체
     */
    Profile save(Profile profile);

    /**
     * 회원 ID로 프로필 조회 (Command용)
     *
     * @param memberId 회원 ID
     * @return 프로필 도메인 객체 Optional
     */
    Optional<Profile> findByMemberId(Long memberId);

    /**
     * 회원 ID로 프로필 삭제
     *
     * @param memberId 삭제할 프로필의 회원 ID
     */
    void deleteByMemberId(Long memberId);

    /**
     * 프로필 존재 여부 확인
     *
     * @param memberId 회원 ID
     * @return 존재 여부
     */
    boolean existsByMemberId(Long memberId);

    /**
     * 모든 프로필 삭제 (테스트용)
     */
    void deleteAll();
}
