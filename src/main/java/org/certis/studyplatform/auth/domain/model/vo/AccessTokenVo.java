package org.certis.studyplatform.auth.domain.model.vo;

import java.time.LocalDateTime;
import java.util.Objects;

public record AccessTokenVo(String value, LocalDateTime expiredAt) {

}
