package org.certis.studyplatform.study.application.mapper;

import org.certis.studyplatform.study.presentation.dto.request.*;
import org.certis.studyplatform.study.application.object.command.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Study Application Command Mapper
 *
 * Presentation Layer DTO를 Application Layer Command 객체로 변환
 */
@Component
public class StudyApplicationCommandMapper {

    /**
     * StudyCreateRequestDto를 CreateStudyCommand로 변환
     */
    public CreateStudyCommand toCreateStudyCommand(StudyCreateRequestDto dto, Long creatorId) {
        // attachedFiles 리스트를 변환합니다. (null-safe 처리 포함)
        List<CreateStudyAttachedCommand> attachedCommands =
                (dto.getAttachedFiles() == null) ? Collections.emptyList() :
                        dto.getAttachedFiles().stream()
                                .map(this::toCreateStudyAttachedCommand) // 람다식(메서드 참조)을 사용한 변환
                                .collect(Collectors.toList());

        return CreateStudyCommand.of(
                dto.getTitle(),
                dto.getDescription(),
                dto.getContent(),
                dto.getCategory(),
                dto.getSubCategory(),
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getGithubUrl(),
                dto.getExternalUrl(),
                dto.getThumbnailUrl(),
                attachedCommands,
                dto.getMaxParticipants(),
                creatorId
        );
    }

    /**
     * StudyAttachedCreateRequestDto를 CreateStudyAttachedCommand로 변환 (이 메서드는 변경 없음)
     */
    public CreateStudyAttachedCommand toCreateStudyAttachedCommand(StudyAttachedCreateRequestDto dto) {
        return CreateStudyAttachedCommand.of(
                dto.getName(),
                dto.getType(),
                dto.getSize(),
                dto.getUrl()
        );
    }

    /**
     * StudyUpdateRequestDto를 UpdateStudyCommand로 변환
     */
    public UpdateStudyCommand toUpdateStudyCommand(StudyUpdateRequestDto dto, Long requesterId) {
        // attachedFiles 리스트를 변환합니다. (null-safe 처리 포함)
        List<CreateStudyAttachedCommand> attachedCommands =
                (dto.getAttachedFiles() == null) ? Collections.emptyList() :
                        dto.getAttachedFiles().stream()
                                .map(this::toCreateStudyAttachedCommand) // 람다식(메서드 참조)을 사용한 변환
                                .collect(Collectors.toList());

        return UpdateStudyCommand.of(
            dto.getStudyId(),
            dto.getTitle(),
            dto.getDescription(),
            dto.getContent(),
            dto.getCategory(),
            dto.getSubCategory(),
            dto.getStartDate(),
            dto.getEndDate(),
            dto.getGithubUrl(),
            dto.getExternalUrl(),
            dto.getThumbnailUrl(),
            attachedCommands,
            dto.getMaxParticipants(),
            requesterId
        );
    }

    /**
     * DeleteStudyCommand 생성
     */
    public DeleteStudyCommand toDeleteStudyCommand(Long studyId, Long requesterId) {
        return DeleteStudyCommand.of(studyId, requesterId);
    }

    /**
     * StudyJoinRequestDto → CreateStudyParticipantCommand 변환
     */
    public CreateStudyParticipantCommand toCreateStudyParticipantCommand(
            StudyJoinRequestDto requestDto, Long memberId) {
        return new CreateStudyParticipantCommand(
                requestDto.getStudyId(),
                memberId
        );
    }

    /**
     * StudyJoinCancelRequestDto → CancelStudyParticipantCommand 변환
     */
    public CancelStudyParticipantCommand toCancelStudyParticipantCommand(
            StudyJoinCancelRequestDto requestDto, Long memberId) {
        return new CancelStudyParticipantCommand(
                requestDto.getStudyId(),
                memberId
        );
    }

    /**
     * StudyJoinApproveRequestDto → UpdateStudyParticipantStatusCommand 변환 (승인용)
     */
    public UpdateStudyParticipantStatusCommand toApproveStudyParticipantCommand(
            StudyJoinApproveRequestDto requestDto, Long requesterId) {
        return new UpdateStudyParticipantStatusCommand(
                requestDto.getParticipantId(),
                org.certis.studyplatform.study.domain.StudyParticipantStatus.APPROVED,
                requesterId
        );
    }

    /**
     * StudyJoinRejectRequestDto → UpdateStudyParticipantStatusCommand 변환 (거절용)
     */
    public UpdateStudyParticipantStatusCommand toRejectStudyParticipantCommand(
            StudyJoinRejectRequestDto requestDto, Long requesterId) {
        return new UpdateStudyParticipantStatusCommand(
                requestDto.getParticipantId(),
                org.certis.studyplatform.study.domain.StudyParticipantStatus.REJECTED,
                requesterId
        );
    }
}