package org.certis.studyplatform.file.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.file.application.FileUploadFacadeService;
import org.certis.studyplatform.file.presentation.dto.response.FileUploadResponseDto;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.certis.studyplatform.shared.type.ImageCategory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final FileUploadFacadeService fileUploadFacadeService;

    @PostMapping("/image")
    public ResponseEntity<GlobalResponseHandler<FileUploadResponseDto>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type
    ) {
        // 1. Enum 변환
        ImageCategory category = ImageCategory.from(type);

        // 2. UseCase 호출 (DTO 반환됨)
        FileUploadResponseDto responseDto = fileUploadFacadeService.uploadImage(file, category);

        // 3. 응답 반환
        return GlobalResponseHandler.success(ResponseStatus.FILE_UPLOAD_SUCCESS, responseDto);
    }

}
