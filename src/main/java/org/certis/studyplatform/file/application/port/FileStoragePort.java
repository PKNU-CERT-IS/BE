package org.certis.studyplatform.file.application.port;

import org.certis.studyplatform.file.domain.vo.FileVo;
import org.certis.studyplatform.shared.type.ImageCategory;
import org.springframework.web.multipart.MultipartFile;

// 별도의 복잡한 비즈니스 로직(도메인 서비스)이 필요하지 않은 단순 파일 전송 기능이므로,
// 어플리케이션 서비스에서 인프라 포트(Port)를 직접 호출하도록 설계했습니다. 이를 통해 불필요한 레이어 진입을 줄이고 코드의 직관성을 높였습니다.
public interface FileStoragePort {
    FileVo upload(MultipartFile file, ImageCategory category);
}
