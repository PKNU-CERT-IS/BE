package org.certis.studyplatform.member.domain.repository.command;

import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.certis.studyplatform.member.domain.vo.ProfileVo;

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
     * @param profileVo 저장할 프로필 도메인 객체
     * @return 저장된 프로필 도메인 객체
     */
    ProfileVo save(ProfileVo profileVo);


    /**
     * 회원 ID로 프로필 삭제
     *
     * @param memberIdVo 삭제할 프로필의 회원 ID
     */
    void deleteByMemberId(MemberIdVo memberIdVo);

    /**
     * 모든 프로필 삭제 (테스트용)
     */
    void deleteAll();
}
