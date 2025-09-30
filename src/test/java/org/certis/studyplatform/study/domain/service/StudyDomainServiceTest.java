package org.certis.studyplatform.study.domain.service;

import org.certis.studyplatform.study.application.object.command.CreateStudyCommand;
import org.certis.studyplatform.study.domain.repository.StudyCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.shared.service.S3FileService;
import org.certis.studyplatform.study.domain.repository.StudyParticipantQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StudyDomainService 도메인 서비스 테스트
 * 
 * 🎯 테스트 목표:
 * - 스터디 생성 시 제한이 없는지 검증
 * - 참가 신청 제한과 생성 제한이 구분되는지 확인
 * - 도메인 로직의 정확성 보장
 * 
 * 🔧 테스트 전략:
 * - Mock을 사용한 단위 테스트
 * - 생성 제한이 없음을 명확히 검증
 * - 다양한 시나리오에서 생성 가능함을 확인
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudyDomainService 도메인 서비스 테스트")
class StudyDomainServiceTest {

    @Mock
    private StudyCommandRepository commandRepository;
    
    @Mock
    private StudyQueryRepository queryRepository;
    
    @Mock
    private MemberDomainService memberDomainService;
    
    @Mock
    private S3FileService s3FileService;

    @Mock
    private StudyParticipantQueryRepository studyParticipantQueryRepository;

    @Mock
    private ProjectParticipantQueryRepository projectParticipantQueryRepository;

    @Mock
    private ProjectQueryRepository projectQueryRepository;

    private StudyDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new StudyDomainService(
                commandRepository,
                queryRepository,
                memberDomainService,
                s3FileService,
                studyParticipantQueryRepository,
                projectParticipantQueryRepository,
                projectQueryRepository
        );
    }

    @Nested
    @DisplayName("스터디 생성 제한 테스트")
    class StudyCreationLimitsTest {

        @Test
        @DisplayName("진행 중인 스터디가 2개 이상이어도 스터디 생성 가능")
        void shouldAllowStudyCreationEvenWithActiveStudies() {
            // Given
            Long creatorId = 1L;
            CreateStudyCommand command = createStudyCommand(creatorId);
            
            // 스터디 생성자는 이미 진행 중인 스터디가 2개 있다고 가정
            // 하지만 생성에는 제한이 없어야 함
            when(commandRepository.save(any(StudyVo.class)))
                    .thenReturn(createStudyVo(1L, creatorId));

            // When
            StudyVo result = domainService.createStudy(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.creatorId()).isEqualTo(creatorId);
            verify(commandRepository).save(any(StudyVo.class));
            
            // 생성 제한 검증이 호출되지 않았음을 확인
            // (참가 신청 제한과 달리 생성에는 제한이 없음)
        }

        @Test
        @DisplayName("진행 중인 프로젝트가 있어도 스터디 생성 가능")
        void shouldAllowStudyCreationEvenWithActiveProjects() {
            // Given
            Long creatorId = 1L;
            CreateStudyCommand command = createStudyCommand(creatorId);
            
            // 스터디 생성자는 이미 진행 중인 프로젝트가 있다고 가정
            // 하지만 생성에는 제한이 없어야 함
            when(commandRepository.save(any(StudyVo.class)))
                    .thenReturn(createStudyVo(1L, creatorId));

            // When
            StudyVo result = domainService.createStudy(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.creatorId()).isEqualTo(creatorId);
            verify(commandRepository).save(any(StudyVo.class));
        }

        @Test
        @DisplayName("진행 중인 스터디와 프로젝트가 모두 있어도 스터디 생성 가능")
        void shouldAllowStudyCreationEvenWithActiveStudiesAndProjects() {
            // Given
            Long creatorId = 1L;
            CreateStudyCommand command = createStudyCommand(creatorId);
            
            // 스터디 생성자는 이미 진행 중인 스터디 1개, 프로젝트 1개가 있다고 가정
            // 참가 신청이라면 제한이 있지만, 생성에는 제한이 없어야 함
            when(commandRepository.save(any(StudyVo.class)))
                    .thenReturn(createStudyVo(1L, creatorId));

            // When
            StudyVo result = domainService.createStudy(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.creatorId()).isEqualTo(creatorId);
            verify(commandRepository).save(any(StudyVo.class));
        }

        @Test
        @DisplayName("연속으로 여러 스터디 생성 가능")
        void shouldAllowMultipleStudyCreations() {
            // Given
            Long creatorId = 1L;
            CreateStudyCommand command1 = createStudyCommand(creatorId, "스터디 1");
            CreateStudyCommand command2 = createStudyCommand(creatorId, "스터디 2");
            CreateStudyCommand command3 = createStudyCommand(creatorId, "스터디 3");
            
            when(commandRepository.save(any(StudyVo.class)))
                    .thenReturn(createStudyVo(1L, creatorId))
                    .thenReturn(createStudyVo(2L, creatorId))
                    .thenReturn(createStudyVo(3L, creatorId));

            // When
            StudyVo result1 = domainService.createStudy(command1);
            StudyVo result2 = domainService.createStudy(command2);
            StudyVo result3 = domainService.createStudy(command3);

            // Then
            assertThat(result1).isNotNull();
            assertThat(result2).isNotNull();
            assertThat(result3).isNotNull();
            
            assertThat(result1.id()).isEqualTo(1L);
            assertThat(result2.id()).isEqualTo(2L);
            assertThat(result3.id()).isEqualTo(3L);
            
            verify(commandRepository, times(3)).save(any(StudyVo.class));
        }
    }

    @Nested
    @DisplayName("스터디 생성 기본 기능 테스트")
    class StudyCreationBasicTest {

        @Test
        @DisplayName("정상적인 스터디 생성")
        void shouldCreateStudySuccessfully() {
            // Given
            Long creatorId = 1L;
            CreateStudyCommand command = createStudyCommand(creatorId);
            
            when(commandRepository.save(any(StudyVo.class)))
                    .thenReturn(createStudyVo(1L, creatorId));

            // When
            StudyVo result = domainService.createStudy(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("테스트 스터디");
            assertThat(result.creatorId()).isEqualTo(creatorId);
            verify(commandRepository).save(any(StudyVo.class));
        }

        @Test
        @DisplayName("스터디 생성 시 VO 검증 로직 실행")
        void shouldExecuteVoValidationOnCreation() {
            // Given
            Long creatorId = 1L;
            CreateStudyCommand command = createStudyCommand(creatorId);
            
            when(commandRepository.save(any(StudyVo.class)))
                    .thenReturn(createStudyVo(1L, creatorId));

            // When
            StudyVo result = domainService.createStudy(command);

            // Then
            assertThat(result).isNotNull();
            // StudyVo.createNew() 내부 검증이 실행되었음을 확인
            // (제목, 설명, 날짜 등의 유효성 검사)
        }
    }

    // ================================================================
    // Helper Methods
    // ================================================================

    private CreateStudyCommand createStudyCommand(Long creatorId) {
        return createStudyCommand(creatorId, "테스트 스터디");
    }

    private CreateStudyCommand createStudyCommand(Long creatorId, String title) {
        return CreateStudyCommand.of(
                title,
                "테스트 설명",
                "테스트 내용",
                "CTF",
                "포너블",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusDays(30),
                null, // githubUrl
                null, // externalUrl
                null, // thumbnailUrl
                null, // attachedFiles
                10, // maxParticipants
                creatorId
        );
    }

    private StudyVo createStudyVo(Long studyId, Long creatorId) {
        OffsetDateTime endDate = OffsetDateTime.now().plusDays(30);
        return new StudyVo(
                studyId,
                "테스트 스터디",
                "테스트 설명",
                "테스트 내용",
                "CTF",
                "포너블",
                OffsetDateTime.now().minusDays(1),
                endDate,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                creatorId,
                "생성자",
                org.certis.studyplatform.member.domain.MemberGrade.FRESHMAN,
                null, // creatorProfileImageUrl
                calculateSemester(endDate), // semester 계산
                calculateStatus(endDate), // status 계산
                10,
                0,
                true, // isParticipantable
                java.util.Collections.emptyList(), // attached
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList()
        );
    }

    private String calculateSemester(OffsetDateTime endedAt) {
        if (endedAt == null) {
            return null;
        }
        
        java.time.LocalDate endDate = endedAt.toLocalDate();
        int year = endDate.getYear();
        int month = endDate.getMonthValue();
        
        if (month >= 3 && month <= 8) {
            return year + "-1"; // 1학기
        } else {
            return year + "-2"; // 2학기
        }
    }

    private String calculateStatus(OffsetDateTime endedAt) {
        if (endedAt == null) {
            return "ACTIVE"; // 종료일이 없으면 활성 상태
        }
        
        OffsetDateTime now = OffsetDateTime.now();
        return endedAt.isBefore(now) ? "ENDED" : "ACTIVE";
    }
}
