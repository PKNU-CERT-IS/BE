package org.certis.studyplatform.file.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.file.application.port.FileStoragePort;
import org.certis.studyplatform.file.domain.vo.FileVo;
import org.certis.studyplatform.shared.service.S3AttachmentService;
import org.certis.studyplatform.shared.type.ImageCategory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileUploadAdapter implements FileStoragePort {

    private  final S3AttachmentService s3AttachmentService;
    @Override
    public FileVo upload(MultipartFile file, ImageCategory category) {
        log.info("Infra: Uploading file. Category: {}", category);

        String uploadedUrl = s3AttachmentService.uploadImageByCategory(file, category);

        return FileVo.of(uploadedUrl,file.getOriginalFilename());
    }
}
