package org.certis.studyplatform.member.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.Profile;
import org.certis.studyplatform.member.domain.service.ProfileDomainService;
import org.certis.studyplatform.member.presentation.dto.request.ProfileUpdateRequestDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile Command Service
 *
 * CQRS Command 측면의 통합 서비스 (Application Layer)
 * 현재 로그인한 사용자의 프로필 관련 쓰기 작업을 담당
 * Domain Service를 통해 비즈니스 로직 수행
 *
 * ✅ RequestDTO → Command → Domain Service 패턴 적용
 *
 * 책임:
 * - 내 프로필 수정 Command 처리
 * - 트랜잭션 관리
 * - 도메인 이벤트 발행
 * - RequestDTO → Domain 변환
 * - Cross-cutting concerns (로깅, 보안, 캐싱 등)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileCommandService {

    private final ProfileDomainService profileDomainService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 내 프로필 정보 수정
     *
     * @param memberId 현재 로그인한 회원 ID
     * @param request 프로필 수정 요청 DTO
     */
    @Transactional
    public void updateMyProfile(Long memberId, ProfileUpdateRequestDto request) {
        log.info("Application: Updating my profile for member ID: {}, fields: name={}, description={}, profileImage={}",
                memberId,
                request.getName() != null,
                request.getDescription() != null,
                request.getProfileImage() != null);

        // 1. Domain Service를 통한 비즈니스 로직 수행
        profileDomainService.updateMyProfile(memberId, profile -> {
            // 이름이 제공된 경우에만 업데이트
            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                profile.updateName(request.getName());
            }

            // 설명 업데이트 (null 허용 - 설명 제거 가능)
            profile.updateDescription(request.getDescription());

            // 프로필 이미지 업데이트 (null 허용 - 이미지 제거 가능)
            profile.updateProfileImageUrl(request.getProfileImage());
        });

        // 2. 도메인 이벤트 발행 (향후 구현)
        // eventPublisher.publishEvent(new MyProfileUpdatedEvent(memberId));

        // 3. Application 레벨 부가 작업
        // - 캐시 무효화
        // - 검색 인덱스 업데이트
        // - 알림 발송 등

        log.info("Application: My profile updated successfully for member ID: {}", memberId);
    }
}