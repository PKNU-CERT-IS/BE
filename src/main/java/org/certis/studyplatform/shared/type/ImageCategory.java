package org.certis.studyplatform.shared.type;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;


@Getter
@RequiredArgsConstructor
public enum ImageCategory {
    BLOG("blog","blog-image"),
    BOARD("board","board-image"),
    STUDY("study","study-image"),
    PROJECT("project","project-image");

    private final String requestType; // 프론트에서 보내는 값
    private final String s3Folder; // S3에 저장될 폴더 명

    public static ImageCategory from(String type){ // 적절한 타입인지 검증
        for(ImageCategory category : values()){
            if(category.requestType.equalsIgnoreCase(type)){
                return category;
            }
        }
        throw new InfrastructureException(ExceptionStatus.FILE_INFRASTRUCTURE_STORAGE_ERROR,"적절하지 않은 분류의 이미지 입니다");
    }

}
