package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SkillsVo 테스트")
class SkillsVoTest {

    @Nested
    @DisplayName("특수문자 허용 범위 확장 테스트")
    class SpecialCharacterValidationTest {

        @Test
        @DisplayName("기본 허용 문자들이 정상적으로 처리됨")
        void basicAllowedCharacters_ShouldPass() {
            // Given: 기본 허용 문자들
            List<String> skills = List.of(
                    "Java", "Python", "JavaScript", "React", "Spring Boot",
                    "MySQL", "PostgreSQL", "Docker", "Kubernetes", "Git"
            );

            // When & Then
            assertThatCode(() -> SkillsVo.of(skills))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("확장된 특수문자들이 정상적으로 처리됨")
        void extendedSpecialCharacters_ShouldPass() {
            // Given: 확장된 특수문자들을 포함한 기술 스택 (20개 이하로 제한)
            List<String> skills = List.of(
                    "C++", "C#", "Node.js", "ASP.NET", "React.js",
                    "Vue.js", "Angular 2+", "TypeScript", "HTML/CSS",
                    "REST API", "GraphQL", "JWT", "OAuth 2.0",
                    "AWS S3", "Google Cloud", "Azure DevOps",
                    "Docker & Kubernetes", "CI/CD", "TDD & BDD",
                    "Agile/Scrum"
            );

            // When & Then
            assertThatCode(() -> SkillsVo.of(skills))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("괄호를 포함한 기술 스택이 정상적으로 처리됨")
        void parenthesesCharacters_ShouldPass() {
            // Given: 괄호를 포함한 기술 스택
            List<String> skills = List.of(
                    "React (Hooks)", "Vue.js (Composition API)",
                    "Angular (v2+)", "Node.js (Express)",
                    "Python (Django)", "Java (Spring Boot)"
            );

            // When & Then
            assertThatCode(() -> SkillsVo.of(skills))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("슬래시와 콜론을 포함한 기술 스택이 정상적으로 처리됨")
        void slashAndColonCharacters_ShouldPass() {
            // Given: 슬래시와 콜론을 포함한 기술 스택
            List<String> skills = List.of(
                    "HTML/CSS", "JavaScript/TypeScript",
                    "Frontend/Backend", "DevOps/SRE",
                    "AWS: EC2, S3, Lambda", "Google: Cloud Run, BigQuery"
            );

            // When & Then
            assertThatCode(() -> SkillsVo.of(skills))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("앰퍼샌드와 퍼센트를 포함한 기술 스택이 정상적으로 처리됨")
        void ampersandAndPercentCharacters_ShouldPass() {
            // Given: 앰퍼샌드와 퍼센트를 포함한 기술 스택
            List<String> skills = List.of(
                    "Design & Development", "Frontend & Backend",
                    "100% Responsive Design", "Performance & Optimization"
            );

            // When & Then
            assertThatCode(() -> SkillsVo.of(skills))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("물결표와 느낌표를 포함한 기술 스택이 정상적으로 처리됨")
        void tildeAndExclamationCharacters_ShouldPass() {
            // Given: 물결표와 느낌표를 포함한 기술 스택
            List<String> skills = List.of(
                    "~/.bashrc", "~/.gitconfig",
                    "Awesome! React", "Great! Performance"
            );

            // When & Then
            assertThatCode(() -> SkillsVo.of(skills))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("여전히 허용되지 않는 문자는 예외 발생")
        void disallowedCharacters_ShouldThrowException() {
            // Given: 허용되지 않는 문자들을 포함한 기술 스택
            List<String> skills = List.of(
                    "Java<script>", "SQL\"injection\"", "React'XSS'",
                    "Node.js\n", "Python\t", "Docker\r"
            );

            // When & Then
            assertThatThrownBy(() -> SkillsVo.of(skills))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("기술 스택 항목에 허용되지 않는 문자가 포함되어 있습니다");
        }

        @Test
        @DisplayName("한글 기술 스택이 정상적으로 처리됨")
        void koreanSkills_ShouldPass() {
            // Given: 한글 기술 스택
            List<String> skills = List.of(
                    "자바", "파이썬", "자바스크립트", "리액트",
                    "스프링 부트", "마이SQL", "도커", "쿠버네티스"
            );

            // When & Then
            assertThatCode(() -> SkillsVo.of(skills))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("혼합 언어 기술 스택이 정상적으로 처리됨")
        void mixedLanguageSkills_ShouldPass() {
            // Given: 혼합 언어 기술 스택
            List<String> skills = List.of(
                    "Java & Spring Boot", "Python/Django",
                    "JavaScript + React", "Node.js & Express",
                    "MySQL + Redis", "Docker & Kubernetes",
                    "AWS (S3, EC2)", "Google Cloud Platform"
            );

            // When & Then
            assertThatCode(() -> SkillsVo.of(skills))
                    .doesNotThrowAnyException();
        }
    }
}
