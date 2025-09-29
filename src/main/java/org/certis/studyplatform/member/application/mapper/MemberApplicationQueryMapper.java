package org.certis.studyplatform.member.application.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.application.object.query.GetMemberByIdQuery;
import org.certis.studyplatform.member.application.object.query.GetMembersQuery;
import org.certis.studyplatform.member.application.object.query.SearchMembersQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
		int safePage = page == null || page < 0 ? 0 : page;
		int safeSize = size == null || size <= 0 ? 10 : size;

		// 기본 정렬: createdAt DESC
		String sortProperty = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy.trim();
		Sort.Direction direction = (sortDirection == null || sortDirection.isBlank())
				? Sort.Direction.DESC
				: ("asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC);

		// 다중 정렬 키 지원 (콤마 구분)
		Sort sort;
		if (sortProperty.contains(",")) {
			String[] props = sortProperty.split(",");
			Sort temp = Sort.unsorted();
			for (String p : props) {
				String prop = p.trim();
				if (!prop.isEmpty()) {
					temp = temp.and(Sort.by(direction, prop));
				}
			}
			sort = temp.isUnsorted() ? Sort.by(direction, "createdAt") : temp;
		} else {
			sort = Sort.by(direction, sortProperty);
		}

		Pageable pageable = PageRequest.of(safePage, safeSize, sort);
		return GetMembersQuery.builder()
				.pageable(pageable)
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
