package org.certis.studyplatform.member.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.object.command.UpdateProfileCommand;
import org.certis.studyplatform.member.domain.service.ProfileDomainService;
import org.certis.studyplatform.member.domain.vo.ProfileVo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
     * 프로필 정보 수정 (Command 객체 사용, VO 반환)
     *
     * @param command 프로필 수정 Command 객체
     * @return 수정된 프로필 VO
     */
    @Transactional
    public ProfileVo updateMyProfile(UpdateProfileCommand command) {
        log.info("Executing update profile command - member ID: {}, fields: name={}, description={}, profileImage={}",
                command.memberId(),
                command.name() != null,
                command.description() != null,
                command.profileImage() != null);

        // 1. Domain Service를 통한 비즈니스 로직 수행 (VO 반환)
        ProfileVo updatedProfile = profileDomainService.updateMyProfile(command);

        // 2. 도메인 이벤트 발행 (향후 구현)
        // eventPublisher.publishEvent(new MyProfileUpdatedEvent(command.memberId()));

        // 3. Application 레벨 부가 작업
        // - 캐시 무효화
        // - 검색 인덱스 업데이트
        // - 알림 발송 등

        log.info("Profile update command executed successfully - member ID: {}", command.memberId());
        return updatedProfile;
    }

    /**
     * 프로필 이미지 업로드
     *
     * @param memberId 회원 ID
     * @param file 업로드할 이미지 파일
     * @return 업로드된 이미지 URL
     */
    @Transactional
    public String uploadProfileImage(Long memberId, MultipartFile file) {
        log.info("Uploading profile image - member ID: {}", memberId);

        // S3에 이미지 업로드
        String imageUrl = profileDomainService.uploadProfileImage(memberId, file);

        log.info("Profile image uploaded successfully - member ID: {}, URL: {}", memberId, imageUrl);
        return imageUrl;
    }
}