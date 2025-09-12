package org.certis.studyplatform.shared.security;

import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// CurrentUser 정보 mock Test용
@Component
public class MockCurrentUserProvider {
    public CurrentUser getMockCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            String username = auth.getName();
            switch (username) {
                case "user1":
                    return new CurrentUser(1L, "user1", "user1@certis.org", "유저1", "UPSOLVER");
                case "user2":
                    return new CurrentUser(2L, "user2", "user2@certis.org", "유저2", "PLAYER");
                case "staff":
                    return new CurrentUser(3L, "staff", "staff@certis.org", "스태프", "STAFF");
                case "admin":
                    return new CurrentUser(99L, "admin", "admin@certis.org", "관리자", "ADMIN");
                default:
                    return new CurrentUser(1L, username, username + "@certis.org", "테스트 사용자", "UPSOLVER");
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
