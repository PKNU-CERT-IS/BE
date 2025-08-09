package org.certis.studyplatform.member.application.object.query;

/**
 * Get Member By ID Query
 * 
 * Application Layer → Domain Service로 전달되는 Query Object
 * 
 * 특징:
 * - Primitive Type으로 구성 (VO 변환 전)
 * - Domain Service에서 VO로 변환하여 검증 수행
 * - 불변 Record 구조로 데이터 무결성 보장
 * 
 * 데이터 흐름:
 * 1. Controller → Facade Service (PathVariable → Query 변환)
 * 2. Facade → Query Service (Query 그대로 전달)
 * 3. Query Service → Domain Service (Query 그대로 전달)
 * 4. Domain Service에서 Query → VO 변환 (검증 수행)
 * 5. Repository 호출 → Infrastructure에서 Entity → VO 변환
 * 6. 결과 VO 반환
 */
public record GetMemberByIdQuery(
        Long id  // → MemberIdVo (양수 검증)
) {
    /**
     * 생성 시점 기본 검증 (Domain 검증 전 빠른 실패)
     * - null 체크 등 기본적인 검증만 수행
     * - 상세한 비즈니스 검증은 VO 생성 시점에서 수행
     */
    public GetMemberByIdQuery {
        if (id == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("회원 ID는 양수여야 합니다");
        }
    }
}