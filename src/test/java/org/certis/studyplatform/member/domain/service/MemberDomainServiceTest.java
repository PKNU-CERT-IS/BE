package org.certis.studyplatform.member.domain.service;

import org.certis.studyplatform.member.application.object.command.CreateMemberCommand;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.repository.command.MemberCommandRepository;
import org.certis.studyplatform.member.domain.repository.command.MemberContactCommandRepository;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.domain.mapper.MemberDomainMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberDomainService 테스트")
class MemberDomainServiceTest {

    @Mock
    private MemberCommandRepository memberCommandRepository;

    @Mock
    private MemberContactCommandRepository memberContactCommandRepository;

    @Mock
    private MemberDomainMapper memberDomainMapper;

    @InjectMocks
    private MemberDomainService memberDomainService;

    private CreateMemberCommand validCommand;

    @BeforeEach
    void setUp() {
        validCommand = new CreateMemberCommand(
                "테스트 사용자",
                "20240001",
                MemberGrade.FRESHMAN,
                MemberRole.NONE,
                "컴퓨터공학과",
                null,
                null,
                "test@example.com",
                "010-1234-5678",
                null,
                OffsetDateTime.now().minusYears(20),
                "MALE"
        );
    }

    @Test
    @DisplayName("회원 생성 시 member_penalty 엔티티도 함께 생성되는지 확인")
    void createMember_ShouldCreateMemberPenalty() {
        // Given
        MemberCreatedVo createdMember = MemberCreatedVo.of(1L, "20240001");
        when(memberCommandRepository.createMember(any())).thenReturn(createdMember);
        
        // Mock VO 생성 메서드들
        when(memberDomainMapper.toNameVo(any())).thenReturn(new NameVo("테스트 사용자"));
        when(memberDomainMapper.toStudentNumberVo(any())).thenReturn(new StudentNumberVo("20240001"));
        when(memberDomainMapper.toGradeVo(any())).thenReturn(new GradeVo(MemberGrade.FRESHMAN));
        when(memberDomainMapper.toRoleVo(any())).thenReturn(new RoleVo(MemberRole.NONE));
        when(memberDomainMapper.toMajorVo(any())).thenReturn(new MajorVo("컴퓨터공학과"));
        when(memberDomainMapper.toEmailVo(any())).thenReturn(new EmailVo("test@example.com"));

        // When
        MemberCreatedVo result = memberDomainService.createMember(validCommand);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.id().value());
        
        // member_penalty 생성이 호출되었는지 확인
        verify(memberCommandRepository, times(1)).createPenalty(any(MemberIdVo.class));
        
        // member_contact 생성도 호출되었는지 확인
        verify(memberContactCommandRepository, times(1)).createContact(any());
    }
}
