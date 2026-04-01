package org.certis.studyplatform.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberRole 권한 검증 테스트")
class MemberRoleTest {

    @Nested
    @DisplayName("Level 검증")
    class LevelValidation {
        
        @Test
        @DisplayName("Level이 낮을수록 높은 등급인지 확인")
        void levelLowerMeansHigherRank() {
            // Given & When & Then
            assertThat(MemberRole.ADMIN.getLevel()).isLessThan(MemberRole.CHAIRMAN.getLevel());
            assertThat(MemberRole.CHAIRMAN.getLevel()).isLessThan(MemberRole.VICECHAIRMAN.getLevel());
            assertThat(MemberRole.VICECHAIRMAN.getLevel()).isLessThan(MemberRole.STAFF.getLevel());
            assertThat(MemberRole.STAFF.getLevel()).isLessThan(MemberRole.PLAYER.getLevel());
            assertThat(MemberRole.PLAYER.getLevel()).isLessThan(MemberRole.UPSOLVER.getLevel());
            assertThat(MemberRole.UPSOLVER.getLevel()).isLessThanOrEqualTo(MemberRole.NONE.getLevel());
        }
    }

    @Nested
    @DisplayName("isStaffOrAbove 검증")
    class IsStaffOrAboveValidation {
        
        @Test
        @DisplayName("ADMIN은 STAFF 이상 권한을 가짐")
        void adminIsStaffOrAbove() {
            // Given & When & Then
            assertThat(MemberRole.isStaffOrAbove(MemberRole.ADMIN)).isTrue();
        }
        
        @Test
        @DisplayName("CHAIRMAN은 STAFF 이상 권한을 가짐")
        void chairmanIsStaffOrAbove() {
            // Given & When & Then
            assertThat(MemberRole.isStaffOrAbove(MemberRole.CHAIRMAN)).isTrue();
        }
        
        @Test
        @DisplayName("VICECHAIRMAN은 STAFF 이상 권한을 가짐")
        void viceChairmanIsStaffOrAbove() {
            // Given & When & Then
            assertThat(MemberRole.isStaffOrAbove(MemberRole.VICECHAIRMAN)).isTrue();
        }
        
        @Test
        @DisplayName("STAFF는 STAFF 이상 권한을 가짐")
        void staffIsStaffOrAbove() {
            // Given & When & Then
            assertThat(MemberRole.isStaffOrAbove(MemberRole.STAFF)).isTrue();
        }
        
        @Test
        @DisplayName("PLAYER는 STAFF 이상 권한을 가지지 않음")
        void playerIsNotStaffOrAbove() {
            // Given & When & Then
            assertThat(MemberRole.isStaffOrAbove(MemberRole.PLAYER)).isFalse();
        }
        
        @Test
        @DisplayName("UPSOLVER는 STAFF 이상 권한을 가지지 않음")
        void upsolverIsNotStaffOrAbove() {
            // Given & When & Then
            assertThat(MemberRole.isStaffOrAbove(MemberRole.UPSOLVER)).isFalse();
        }
        
        @Test
        @DisplayName("NONE은 STAFF 이상 권한을 가지지 않음")
        void noneIsNotStaffOrAbove() {
            // Given & When & Then
            assertThat(MemberRole.isStaffOrAbove(MemberRole.NONE)).isFalse();
        }
    }

    @Nested
    @DisplayName("canChangeRole 검증")
    class CanChangeRoleValidation {
        
        @Test
        @DisplayName("ADMIN은 모든 역할을 변경할 수 있음")
        void adminCanChangeAllRoles() {
            // Given & When & Then
            assertThat(MemberRole.ADMIN.canChangeRole(MemberRole.CHAIRMAN)).isTrue();
            assertThat(MemberRole.ADMIN.canChangeRole(MemberRole.VICECHAIRMAN)).isTrue();
            assertThat(MemberRole.ADMIN.canChangeRole(MemberRole.STAFF)).isTrue();
            assertThat(MemberRole.ADMIN.canChangeRole(MemberRole.PLAYER)).isTrue();
            assertThat(MemberRole.ADMIN.canChangeRole(MemberRole.UPSOLVER)).isTrue();
            assertThat(MemberRole.ADMIN.canChangeRole(MemberRole.NONE)).isTrue();
        }
        
        @Test
        @DisplayName("CHAIRMAN은 VICECHAIRMAN 이하의 역할을 변경할 수 있음")
        void chairmanCanChangeLowerRoles() {
            // Given & When & Then
            assertThat(MemberRole.CHAIRMAN.canChangeRole(MemberRole.VICECHAIRMAN)).isTrue();
            assertThat(MemberRole.CHAIRMAN.canChangeRole(MemberRole.STAFF)).isTrue();
            assertThat(MemberRole.CHAIRMAN.canChangeRole(MemberRole.PLAYER)).isTrue();
            assertThat(MemberRole.CHAIRMAN.canChangeRole(MemberRole.UPSOLVER)).isTrue();
            assertThat(MemberRole.CHAIRMAN.canChangeRole(MemberRole.NONE)).isTrue();
        }
        
        @Test
        @DisplayName("CHAIRMAN은 같은 등급인 CHAIRMAN 역할도 변경할 수 있음")
        void chairmanCanChangeSameRole() {
            // Given & When & Then
            assertThat(MemberRole.CHAIRMAN.canChangeRole(MemberRole.CHAIRMAN)).isTrue();
        }
        
        @Test
        @DisplayName("CHAIRMAN은 ADMIN 역할을 변경할 수 없음")
        void chairmanCannotChangeAdmin() {
            // Given & When & Then
            assertThat(MemberRole.CHAIRMAN.canChangeRole(MemberRole.ADMIN)).isFalse();
        }
        
        @Test
        @DisplayName("STAFF는 PLAYER 이하의 역할을 변경할 수 있음")
        void staffCanChangeLowerRoles() {
            // Given & When & Then
            assertThat(MemberRole.STAFF.canChangeRole(MemberRole.PLAYER)).isTrue();
            assertThat(MemberRole.STAFF.canChangeRole(MemberRole.UPSOLVER)).isTrue();
            assertThat(MemberRole.STAFF.canChangeRole(MemberRole.NONE)).isTrue();
        }
        
        @Test
        @DisplayName("STAFF는 같은 등급인 STAFF 역할도 변경할 수 있음")
        void staffCanChangeSameRole() {
            // Given & When & Then
            assertThat(MemberRole.STAFF.canChangeRole(MemberRole.STAFF)).isTrue();
        }
        
        @Test
        @DisplayName("STAFF는 STAFF 이상의 역할을 변경할 수 없음")
        void staffCannotChangeHigherRoles() {
            // Given & When & Then
            assertThat(MemberRole.STAFF.canChangeRole(MemberRole.ADMIN)).isFalse();
            assertThat(MemberRole.STAFF.canChangeRole(MemberRole.CHAIRMAN)).isFalse();
            assertThat(MemberRole.STAFF.canChangeRole(MemberRole.VICECHAIRMAN)).isFalse();
        }
        
        @Test
        @DisplayName("PLAYER는 NONE 역할만 변경할 수 있음")
        void playerCanOnlyChangeNoneRole() {
            // Given & When & Then
            assertThat(MemberRole.PLAYER.canChangeRole(MemberRole.ADMIN)).isFalse();
            assertThat(MemberRole.PLAYER.canChangeRole(MemberRole.CHAIRMAN)).isFalse();
            assertThat(MemberRole.PLAYER.canChangeRole(MemberRole.VICECHAIRMAN)).isFalse();
            assertThat(MemberRole.PLAYER.canChangeRole(MemberRole.STAFF)).isFalse();
            assertThat(MemberRole.PLAYER.canChangeRole(MemberRole.UPSOLVER)).isTrue(); // 같은 등급
            assertThat(MemberRole.PLAYER.canChangeRole(MemberRole.NONE)).isTrue();
        }
        
        @Test
        @DisplayName("PLAYER는 같은 등급인 PLAYER 역할도 변경할 수 있음")
        void playerCanChangeSameRole() {
            // Given & When & Then
            assertThat(MemberRole.PLAYER.canChangeRole(MemberRole.PLAYER)).isTrue();
        }
        
        @Test
        @DisplayName("UPSOLVER는 같은 등급인 UPSOLVER 역할도 변경할 수 있음")
        void upsolverCanChangeSameRole() {
            // Given & When & Then
            assertThat(MemberRole.UPSOLVER.canChangeRole(MemberRole.UPSOLVER)).isTrue();
        }
    }

    @Nested
    @DisplayName("Authority String 변환 검증")
    class AuthorityStringValidation {
        
        @Test
        @DisplayName("toAuthorityString은 ROLE_ 접두사를 추가함")
        void toAuthorityStringAddsRolePrefix() {
            // Given & When & Then
            assertThat(MemberRole.ADMIN.toAuthorityString()).isEqualTo("ROLE_ADMIN");
            assertThat(MemberRole.STAFF.toAuthorityString()).isEqualTo("ROLE_STAFF");
            assertThat(MemberRole.PLAYER.toAuthorityString()).isEqualTo("ROLE_PLAYER");
        }
        
        @Test
        @DisplayName("fromAuthorityString은 ROLE_ 접두사를 제거함")
        void fromAuthorityStringRemovesRolePrefix() {
            // Given & When & Then
            assertThat(MemberRole.fromAuthorityString("ROLE_ADMIN")).isEqualTo(MemberRole.ADMIN);
            assertThat(MemberRole.fromAuthorityString("ROLE_STAFF")).isEqualTo(MemberRole.STAFF);
            assertThat(MemberRole.fromAuthorityString("ROLE_PLAYER")).isEqualTo(MemberRole.PLAYER);
        }
    }
}
