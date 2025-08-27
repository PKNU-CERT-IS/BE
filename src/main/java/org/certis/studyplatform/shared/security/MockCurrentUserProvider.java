package org.certis.studyplatform.shared.security;

import org.springframework.stereotype.Component;

// CurrentUser 정보 mock Test용
@Component
public class MockCurrentUserProvider {
    public CurrentUser getMockCurrentUser() {
        return new CurrentUser(
                1L,
                "testAdmin",
                "test@admin.com",
                "테스트 관리자",
                "ADMIN"
        );
    }
}
