package com.example.batch.lesson04;

import com.example.batch.support.BatchIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParameterJobTest extends BatchIntegrationTestSupport {

    @Autowired
    private Job parameterJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(parameterJob);
    }

    @Test
    void targetDate_를_주면_정상_완료된다() throws Exception {
        JobExecution execution = jobOperatorTestUtils.startJob(params(LocalDate.of(2026, 10, 1)));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getJobParameters().getLocalDate("targetDate")).isEqualTo("2026-10-01");
    }

    @Test
    void 같은_파라미터로_다시_실행하면_이미_완료된_JobInstance_라서_거부된다() throws Exception {
        jobOperatorTestUtils.startJob(params(LocalDate.of(2026, 10, 1)));

        assertThatThrownBy(() -> jobOperatorTestUtils.startJob(params(LocalDate.of(2026, 10, 1))))
                .isInstanceOf(JobInstanceAlreadyCompleteException.class);
    }

    @Test
    void 다른_파라미터는_새로운_JobInstance_로_실행된다() throws Exception {
        JobExecution first = jobOperatorTestUtils.startJob(params(LocalDate.of(2026, 10, 1)));
        JobExecution second = jobOperatorTestUtils.startJob(params(LocalDate.of(2026, 10, 2)));

        assertThat(second.getJobInstance().getInstanceId())
                .isNotEqualTo(first.getJobInstance().getInstanceId());
    }

    @Test
    void 필수_파라미터가_없으면_Validator_가_실행을_막는다() {
        assertThatThrownBy(() -> jobOperatorTestUtils.startJob(new JobParameters()))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessageContaining("targetDate");
    }

    private static JobParameters params(LocalDate targetDate) {
        return new JobParametersBuilder()
                .addLocalDate("targetDate", targetDate)
                .toJobParameters();
    }
}
