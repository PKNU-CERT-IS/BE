package org.certis.studyplatform.study.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.GracePeriodService;
import org.certis.studyplatform.study.domain.service.StudyDomainService;
import org.certis.studyplatform.study.domain.service.StudyParticipantDomainService;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.study.application.object.command.CreateStudyCommand;
import org.certis.studyplatform.study.application.object.command.DeleteStudyCommand;
import org.certis.studyplatform.study.application.object.command.EndStudyCommand;
import org.certis.studyplatform.study.application.object.command.UpdateStudyCommand;
import org.certis.studyplatform.shared.service.S3FileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Study Command Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 쓰기 작업 처리 (CQRS Command Side)
 *
 * Command 객체를 Domain Service로 전달하여 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyCommandService {

    private final StudyDomainService studyDomainService;
    private final StudyParticipantDomainService studyParticipantDomainService;
    private final S3FileService s3FileService;
    private final GracePeriodService gracePeriodService;

    /**
     * 프로젝트 생성
     */

    //TODO: 추후에 Transactional 결합도 낮추기
    // 1. 생성자 존재 확인
    // 2. 프로젝트 생성
    // 3. 생성자를 참가자로 등록
    @Transactional
    public StudyVo createStudy(CreateStudyCommand command) {
        log.info("Command: Creating study - {}", command.title());

        // Command 객체를 Domain Service로 전달
        StudyVo createdVo = studyDomainService.createStudy(command);

        studyParticipantDomainService.registerStudyCreatorAsParticipant(createdVo.id(), command.creatorId());

        log.info("Command: Study created successfully - ID: {}", createdVo.id());
        return createdVo;
    }

    /**
     * 프로젝트 수정
     */
    @Transactional
    public StudyVo updateStudy(UpdateStudyCommand command) {
        log.info("Command: Updating study - ID: {}", command.id());

        // Command 객체를 Domain Service로 전달
        StudyVo updatedVo = studyDomainService.updateStudy(command);

        log.info("Command: Study updated successfully - ID: {}", updatedVo.id());
        return updatedVo;
    }

    /**
     * 프로젝트 삭제
     */
    @Transactional
    public void deleteStudy(DeleteStudyCommand command) {
        log.info("Command: Deleting study - ID: {}", command.id());

        // Command 객체를 Domain Service로 전달
        studyDomainService.deleteStudy(command);

        log.info("Command: Study deleted successfully - ID: {}", command.id());
    }

    /**
     * 스터디 종료
     */
    @Transactional
    public StudyVo endStudy(EndStudyCommand command) {
        log.info("Command: Ending study - ID: {}", command.studyId());

        // Command 객체를 Domain Service로 전달 (파일명은 Domain Service에서 처리)
        StudyVo endedVo = studyDomainService.endStudy(command);

        // 조기 종료 시 유예기간 재조정
        if (endedVo.startDate() != null && endedVo.endDate() != null) {
            // 원래 예정된 종료일과 실제 종료일을 비교하여 조기 종료인지 확인
            OffsetDateTime originalEndDate = endedVo.startDate().plusWeeks(4); // 기본 4주 스터디 가정
            OffsetDateTime actualEndDate = endedVo.endDate();
            
            // 실제 종료일이 원래 예정일보다 빠른 경우 조기 종료로 간주
            if (actualEndDate.isBefore(originalEndDate)) {
                log.info("Command: Study terminated early - adjusting grace period for study ID: {}", endedVo.id());
                try {
                    gracePeriodService.adjustGracePeriodForEarlyTerminatedStudy(
                        endedVo.id(), 
                        endedVo.startDate(), 
                        actualEndDate
                    );
                } catch (Exception e) {
                    log.error("Command: Failed to adjust grace period for early terminated study - ID: {}, error: {}", 
                        endedVo.id(), e.getMessage(), e);
                    // 유예기간 재조정 실패가 스터디 종료를 중단시키지 않도록 처리
                }
            }
        }

        // 파일 업로드 (있는 경우) - Domain Service에서 스터디 정보를 가져온 후 처리
        if (command.files() != null && !command.files().isEmpty()) {
            for (MultipartFile file : command.files()) {
                if (!file.isEmpty()) {
                    try {
                        // 파일명 생성: 스터디제목_ID_생성자이름_원본파일명
                        String originalFilename = file.getOriginalFilename();
                        String fileExtension = "";
                        if (originalFilename != null && originalFilename.contains(".")) {
                            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                        }
                        
                        String customFilename = String.format("%s_%d_%s%s", 
                                sanitizeFilename(endedVo.title()), 
                                endedVo.id(), 
                                sanitizeFilename(endedVo.creatorName()),
                                fileExtension);
                        
                        String fileUrl = s3FileService.uploadFileWithCustomName(file, "study-end-attachments", customFilename);
                        log.info("Study end attachment uploaded successfully: {}", fileUrl);
                    } catch (Exception e) {
                        log.error("Failed to upload study end attachment: {}", file.getOriginalFilename(), e);
                        throw new RuntimeException("스터디 종료 첨부파일 업로드에 실패했습니다: " + file.getOriginalFilename(), e);
                    }
                }
            }
        }

        log.info("Command: Study ended successfully - ID: {}", endedVo.id());
        return endedVo;
    }

    /**
     * 파일명에서 특수문자 제거 및 안전한 파일명 생성
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unknown";
        return filename.replaceAll("[^a-zA-Z0-9가-힣]", "_")
                      .replaceAll("_{2,}", "_")
                      .replaceAll("^_|_$", "");
    }
}