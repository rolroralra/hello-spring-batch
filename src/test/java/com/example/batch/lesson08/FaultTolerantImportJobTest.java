package com.example.batch.lesson08;

import com.example.batch.support.BatchIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;

import static com.example.batch.jooq.Tables.PRODUCT;
import static com.example.batch.jooq.Tables.PRODUCT_IMPORT_ERROR;
import static org.assertj.core.api.Assertions.assertThat;

class FaultTolerantImportJobTest extends BatchIntegrationTestSupport {

    private static final JobParameters ERROR_CSV = new JobParametersBuilder()
            .addString("inputFile", "classpath:input/products-with-errors.csv")
            .toJobParameters();

    @Autowired
    private Job faultTolerantImportJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(faultTolerantImportJob);
    }

    @Test
    void 잘못된_데이터는_Skip_하고_나머지는_적재한다() throws Exception {
        JobExecution execution = jobOperatorTestUtils.startJob(ERROR_CSV);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution step = execution.getStepExecutions().iterator().next();
        assertThat(step.getReadSkipCount()).isEqualTo(2);    // abc 가격, 컬럼 부족
        assertThat(step.getProcessSkipCount()).isEqualTo(2); // 음수 가격, 음수 재고
        assertThat(step.getWriteSkipCount()).isEqualTo(1);   // 중복 코드
        assertThat(step.getWriteCount()).isEqualTo(8);

        assertThat(dsl.fetchCount(PRODUCT)).isEqualTo(8);
    }

    @Test
    void Skip_된_데이터는_별도_트랜잭션으로_에러_테이블에_남는다() throws Exception {
        jobOperatorTestUtils.startJob(ERROR_CSV);

        var phases = dsl.select(PRODUCT_IMPORT_ERROR.PHASE)
                .from(PRODUCT_IMPORT_ERROR)
                .orderBy(PRODUCT_IMPORT_ERROR.ID)
                .fetch(PRODUCT_IMPORT_ERROR.PHASE);

        assertThat(phases).containsExactlyInAnyOrder("READ", "READ", "PROCESS", "PROCESS", "WRITE");
    }
}
