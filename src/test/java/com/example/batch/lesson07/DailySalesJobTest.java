package com.example.batch.lesson07;

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
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static com.example.batch.jooq.Tables.DAILY_SALES_SUMMARY;
import static org.assertj.core.api.Assertions.assertThat;

class DailySalesJobTest extends BatchIntegrationTestSupport {

    @Autowired
    private Job dailySalesJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(dailySalesJob);
    }

    @Value("${batch.report-dir}")
    private Path reportDir;

    @Test
    void 주문이_있는_날은_집계하고_리포트를_만든다() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 10, 1);

        JobExecution execution = jobOperatorTestUtils.startJob(params(targetDate));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions())
                .extracting(StepExecution::getStepName)
                .containsExactly("aggregateStep", "reportStep");

        var summary = dsl.fetchSingle(DAILY_SALES_SUMMARY, DAILY_SALES_SUMMARY.SALES_DATE.eq(targetDate));
        assertThat(summary.getOrderCount()).isEqualTo(38);
        assertThat(summary.getTotalAmount()).isEqualTo(3_102_000L);

        // ExecutionContextPromotionListener 가 Job ExecutionContext 로 올려준 값
        assertThat(execution.getExecutionContext().getInt("orderCount")).isEqualTo(38);
        assertThat(execution.getExecutionContext().getLong("totalAmount")).isEqualTo(3_102_000L);

        Path report = reportDir.resolve("daily-sales-2026-10-01.txt");
        assertThat(Files.readString(report)).contains("38건", "3,102,000원");
    }

    @Test
    void 주문이_없는_날은_noOrdersStep_으로_분기한다() throws Exception {
        JobExecution execution = jobOperatorTestUtils.startJob(params(LocalDate.of(2026, 10, 8)));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions())
                .extracting(StepExecution::getStepName)
                .containsExactly("noOrdersStep");
        assertThat(dsl.fetchCount(DAILY_SALES_SUMMARY)).isZero();
    }

    private static JobParameters params(LocalDate targetDate) {
        return new JobParametersBuilder()
                .addLocalDate("targetDate", targetDate)
                .toJobParameters();
    }
}
