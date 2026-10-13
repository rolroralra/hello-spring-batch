package com.example.batch.lesson05;

import com.example.batch.support.BatchIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;

import static com.example.batch.jooq.Tables.PRODUCT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductImportJobTest extends BatchIntegrationTestSupport {

    private static final JobParameters PRODUCTS_CSV = new JobParametersBuilder()
            .addString("inputFile", "classpath:input/products.csv")
            .toJobParameters();

    @Autowired
    private Job productImportJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(productImportJob);
    }

    @Test
    void CSV_를_읽어서_사은품을_제외한_상품을_적재한다() throws Exception {
        JobExecution execution = jobOperatorTestUtils.startJob(PRODUCTS_CSV);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution step = execution.getStepExecutions().iterator().next();
        assertThat(step.getReadCount()).isEqualTo(22);
        assertThat(step.getFilterCount()).isEqualTo(2);   // 가격 0원 사은품 2개
        assertThat(step.getWriteCount()).isEqualTo(20);
        assertThat(step.getCommitCount()).isEqualTo(5);   // 22건 / chunk 5 = 5번 커밋

        assertThat(dsl.fetchCount(PRODUCT)).isEqualTo(20);
        assertThat(dsl.fetchValue(PRODUCT.NAME, PRODUCT.CODE.eq("P-003"))).isEqualTo("27인치 모니터");
    }

    @Test
    void 같은_파일은_한_번만_적재된다() throws Exception {
        jobOperatorTestUtils.startJob(PRODUCTS_CSV);

        assertThatThrownBy(() -> jobOperatorTestUtils.startJob(PRODUCTS_CSV))
                .isInstanceOf(JobInstanceAlreadyCompleteException.class);
    }

    @Test
    void Step_하나만_따로_실행할_수도_있다() {
        JobExecution execution = jobOperatorTestUtils.startStep("productImportStep", PRODUCTS_CSV, new ExecutionContext());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(dsl.fetchCount(PRODUCT)).isEqualTo(20);
    }
}
