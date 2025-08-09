package org.certis.studyplatform.member.domain.repository.command;

import org.certis.studyplatform.member.domain.vo.*;

import java.util.Optional;

/**
 * Member Command Repository (Write Operations Only) - VO 기반
 *
 * CQRS Command 측면의 Repository (VO 기반)
 * Domain 객체 대신 VO들을 통한 쓰기 작업 및 Command 실행에 필요한 최소한의 읽기 작업만 제공
 *
 * 책임:
 * - 회원 생성/수정/삭제 (CUD Operations) - VO 기반
 * - Command 실행을 위한 최소한의 존재성 확인
 * - 데이터 무결성 및 비즈니스 규칙 검증
 *
 * 특징:
 * - Domain 객체 대신 VO들만 사용
 * - 비즈니스 로직은 Service Layer에서 VO 레벨로 처리
 * - Infrastructure와는 VO ↔ Entity 매퍼를 통해 통신
 * - MemberInfrastructureMapper를 통한 Entity 변환
 */
public interface MemberCommandRepository {

    /**
     * 회원 생성
     *
     * @param memberCreationVo 생성할 회원 정보 VO
     * @return 생성된 회원 정보 VO (ID, 학번 포함)
     */
    MemberCreatedVo createMember(MemberCreationVo memberCreationVo);

    /**
     * 회원 정보 수정
     *
     * @param memberId 수정할 회원 ID
     * @param memberUpdateVo 수정할 회원 정보 VO
     * @return 수정된 회원 정보 VO
     */
    MemberUpdatedVo updateMember(MemberIdVo memberId, MemberUpdateVo memberUpdateVo);

    /**
     * 회원 프로필 수정
     *
     * @param memberId 수정할 회원 ID
     * @param profileUpdateVo 수정할 프로필 정보 VO
     * @return 수정된 회원 정보 VO
     */
    MemberVo updateProfile(MemberIdVo memberId, ProfileUpdateVo profileUpdateVo);

    /**
     * 회원 삭제 (Soft Delete)
     * 실제로는 deleted_at 필드를 현재 시간으로 설정
     *
     * @param memberId 삭제할 회원 ID
     */
    void deleteById(MemberIdVo memberId);

    /**
     * 회원 ID로 조회 (Command 실행을 위한 최소한의 읽기)
     *
     * 용도: Update Command에서 기존 회원 조회
     *
     * @param memberId 회원 ID
     * @return 회원 정보 VO (Optional)
     */
    Optional<MemberVo> findById(MemberIdVo memberId);

    /**
     * 학번 중복 체크 (Command 실행을 위한 존재성 확인)
     *
     * 용도: Create/Update Command에서 학번 중복 검증
     *
     * @param studentNumber 확인할 학번
     * @return 중복 여부 (true: 중복됨, false: 중복되지 않음)
     */
    boolean existsByStudentNumber(StudentNumberVo studentNumber);

    /**
     * 회원 존재 여부 확인
     *
     * @param memberId 확인할 회원 ID
     * @return 존재 여부 (true: 존재함, false: 존재하지 않음)
     */
    boolean existsById(MemberIdVo memberId);

    /**
     * 회원 활성화 (Soft Delete 해제)
     * deleted_at 필드를 null로 설정
     *
     * @param memberId 활성화할 회원 ID
     * @return 활성화된 회원 정보 VO
     */
    MemberVo activateMember(MemberIdVo memberId);

    /**
     * 특정 조건으로 회원 수 조회 (Command 검증용)
     *
     * @param grade 학년 (선택)
     * @param role 역할 (선택)
     * @return 조건에 맞는 회원 수
     */
    long countByConditions(GradeVo grade, RoleVo role);
}