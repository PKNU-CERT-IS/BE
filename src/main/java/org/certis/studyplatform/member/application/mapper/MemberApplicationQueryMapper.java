package org.certis.studyplatform.member.application.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.application.object.query.GetMemberByIdQuery;
import org.certis.studyplatform.member.application.object.query.GetMembersQuery;
import org.certis.studyplatform.member.application.object.query.SearchMembersQuery;
import org.certis.studyplatform.member.presentation.dto.request.MemberSearchRequestDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Member Application Query Mapper
 *
 * ✅ DTO → Query Object 변환 담당
 * ✅ Application Layer의 Query 전용 매퍼
 * ✅ 네이밍 컨벤션: MemberApplicationQueryMapper
 */
@Component
@RequiredArgsConstructor
public class MemberApplicationQueryMapper {

    /**
     * DTO → GetMemberByIdQuery 변환
     */
    public GetMemberByIdQuery toGetMemberByIdQuery(Long memberId) {
        return new GetMemberByIdQuery(memberId);
    }

    /**
     * DTO → SearchMembersQuery 변환
     */
    public SearchMembersQuery toSearchMembersQuery(MemberSearchRequestDto requestDto, Pageable pageable) {
        return new SearchMembersQuery(
            requestDto.getSafeKeyword(),
            requestDto.getSafeGrade(),
            requestDto.getSafeRole(),
            null, // TODO: MemberSearchRequestDto에 skills 필드 추가 필요
            pageable
        );
    }

    /**
     * DTO → SearchMembersQuery 변환 (기본 페이징 사용)
     */
    public SearchMembersQuery toSearchMembersQuery(MemberSearchRequestDto requestDto) {
        // 기본 페이징 설정 (페이지 0, 크기 20)
        Pageable defaultPageable = PageRequest.of(0, 20);
        return new SearchMembersQuery(
            requestDto.getSafeKeyword(),
            requestDto.getSafeGrade(),
            requestDto.getSafeRole(),
            null, // TODO: MemberSearchRequestDto에 skills 필드 추가 필요
            defaultPageable
        );
    }

    /**
     * DTO → SearchMembersQuery 변환 (키워드 + 페이징)
     */
    public SearchMembersQuery toSearchMembersQuery(String keyword, Pageable pageable) {
        return new SearchMembersQuery(keyword, null, null, null, pageable);
    }

    /**
     * DTO → GetMembersQuery 변환
     */
    public GetMembersQuery toGetMembersQuery(Pageable pageable) {
        return GetMembersQuery.builder()
                .pageable(pageable)
                .build();
    }

    /**
     * DTO → GetMembersQuery 변환 (페이징 파라미터)
     */
    public GetMembersQuery toGetMembersQuery(Integer page, Integer size, String sortBy, String sortDirection) {
        // 페이징 파라미터로 Pageable을 생성하는 로직이 필요하지만,
        // 현재는 기본값으로 빈 쿼리를 생성
        return GetMembersQuery.builder()
                .build();
    }

    /**
     * DTO → GetMemberSummariesQuery 변환
     */
    public GetMembersQuery toGetMemberSummariesQuery(Pageable pageable) {
        return GetMembersQuery.builder()
                .pageable(pageable)
                .build();
    }
}
