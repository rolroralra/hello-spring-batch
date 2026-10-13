package com.example.batch.support;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * Job 통합 테스트 공통 설정.
 *
 * <ul>
 *   <li>spring.batch.job.enabled=false : 컨텍스트가 뜰 때 Job 이 자동 실행되지 않게 한다.</li>
 *   <li>{@code @SpringBatchTest} : JobOperatorTestUtils, JobRepositoryTestUtils 빈을 등록한다.</li>
 *   <li>매 테스트 전에 메타데이터를 지우고 schema.sql / data.sql 로 도메인 데이터를 초기화한다.</li>
 * </ul>
 *
 * <p>컨텍스트에 Job 이 여러 개이므로 각 테스트 클래스의 {@code @BeforeEach} 에서
 * {@code jobOperatorTestUtils.setJob(...)} 으로 테스트할 Job 을 지정한다.
 *
 * <p><b>주의</b>: {@code @SpringBatchTest} 의 JobScopeTestExecutionListener 는 테스트 클래스에서
 * "JobExecution 을 반환하는 메서드"를 찾아 인자 없이 호출한다.
 * 그래서 테스트 클래스(와 부모 클래스)에 JobExecution 을 반환하는 헬퍼 메서드를 만들면 안 된다.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "batch.report-dir=build/test-reports/batch"
})
@SpringBatchTest
@Import(TestcontainersConfiguration.class)
public abstract class BatchIntegrationTestSupport {

    @Autowired
    protected JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    protected JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    protected DSLContext dsl;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void resetDatabase() {
        jobRepositoryTestUtils.removeJobExecutions();
        new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql"),
                new ClassPathResource("data.sql"))
                .execute(dataSource);
    }
}
