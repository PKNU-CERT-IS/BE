package org.certis.studyplatform.shared.idempotency;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "certis.idempotency")
public class IdempotencyProperties {

    private DomainFlag board = new DomainFlag();
    private DomainFlag study = new DomainFlag();
    private DomainFlag project = new DomainFlag();

    private long processingStaleSeconds = 90;
    private long recordTtlSeconds = 86400;
    private long lockLeaseSeconds = 30;

    @Getter
    @Setter
    public static class DomainFlag {
        private boolean enabled = false;
    }
}
