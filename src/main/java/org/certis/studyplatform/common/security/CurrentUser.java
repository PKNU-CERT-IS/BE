package org.certis.studyplatform.common.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 현재 로그인한 사용자 정보를 담는 클래스
 *
 * Spring Security UserDetails 인터페이스 구현
 * @AuthenticationPrincipal 어노테이션으로 Controller에서 주입받아 사용
 *
 * 사용 예시:
 * @GetMapping("/me")
 * public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal CurrentUser currentUser) {
 *     Long memberId = currentUser.getId();
 *     // ...
 * }
 */
@Getter
@RequiredArgsConstructor
public class CurrentUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String name;
    private final String role;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;

    /**
     * 기본 생성자 (활성화된 계정)
     */
    public CurrentUser(Long id, String username, String email, String name, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.name = name;
        this.role = role != null ? role : "MEMBER";
        this.enabled = true;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
    }

    /**
     * Spring Security에서 요구하는 권한 정보 반환
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /**
     * 비밀번호는 보안상 반환하지 않음
     */
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "CurrentUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}