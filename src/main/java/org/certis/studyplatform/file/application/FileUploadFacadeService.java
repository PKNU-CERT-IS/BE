package org.certis.studyplatform.file.application;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.file.application.port.FileStoragePort;
import org.certis.studyplatform.file.domain.vo.FileVo;
import org.certis.studyplatform.file.presentation.dto.response.FileUploadResponseDto;
import org.certis.studyplatform.shared.type.ImageCategory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileUploadFacadeService {
    private final FileStoragePort fileStoragePort;

    public FileUploadResponseDto uploadImage(MultipartFile file, ImageCategory category){

        FileVo fileVo = fileStoragePort.upload(file,category);

        return FileUploadResponseDto.builder()
                .imageUrl(fileVo.url())
                .build();
    }
}
