package org.certis.studyplatform.shared.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

@Configuration
public class JooqConfig {

    /**
     * jOOQ 전용 DSLContext
     * 별도 DataSource 사용으로 트랜잭션 충돌 방지
     */
    @Bean
    public DSLContext dslContext(@Qualifier("jooqDataSource") DataSource jooqDataSource) {
        DefaultConfiguration config = new DefaultConfiguration();
        config.set(SQLDialect.POSTGRES);
        config.set(jooqDataSource);

        // 트랜잭션 관리 비활성화 (autoCommit=true 활용)
        // SpringTransactionProvider 사용하지 않음

        return DSL.using(config);
    }

    @Bean
    public TransactionAwareDataSourceProxy jooqTransactionAwareDataSource(
            @Qualifier("jooqDataSource") DataSource jooqDataSource) {
        return new TransactionAwareDataSourceProxy(jooqDataSource);
    }
}