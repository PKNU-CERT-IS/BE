package org.certis.studyplatform.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// CurrentUser 정보 mock Test용
@Slf4j
@Component
public class MockCurrentUserProvider {
    public CurrentUser getMockCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            // If the test already placed a CurrentUser into the SecurityContext, use it as-is
            Object principal = auth.getPrincipal();
            if (principal instanceof CurrentUser) {
                return (CurrentUser) principal;
            }

            // Fallback to mapping by username if only name is present
            if (auth.getName() != null) {
            String username = auth.getName();
            log.info("📌 MockCurrentUserProvider username={}", username);

            switch (username) {
                case "user1":
                    return new CurrentUser(1L, "user1", "user1@certis.org", "유저1", "UPSOLVER");
                case "user2":
                    return new CurrentUser(2L, "user2", "user2@certis.org", "유저2", "PLAYER");
                case "leader":
                    return new CurrentUser(5L, "leader", "ledaer@certis.org", "유저3", "LEADER");
                case "staff":
                    return new CurrentUser(3L, "staff", "staff@certis.org", "스태프", "STAFF");
                case "wrong":
                    return new CurrentUser(0L, "wrong", "wrong@certis.org", "잘못된유저", "NONE");
                case "admin":
                    return new CurrentUser(99L, "admin", "admin@certis.org", "관리자", "ADMIN");
                default:
                    return new CurrentUser(1L, username, username + "@certis.org", "테스트 사용자", "UPSOLVER");
            }
            }
        }
        return new CurrentUser(
                1L,
                "testAdmin",
                "test@admin.com",
                "테스트 관리자",
                "ADMIN"
        );
    }
}
