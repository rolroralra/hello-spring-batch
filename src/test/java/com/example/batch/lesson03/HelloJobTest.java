package com.example.batch.lesson03;

import com.example.batch.support.BatchIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class HelloJobTest extends BatchIntegrationTestSupport {

    @Autowired
    private Job helloJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(helloJob);
    }

    @Test
    void helloJob_은_두_개의_Step_을_순서대로_실행한다() throws Exception {
        JobExecution execution = jobOperatorTestUtils.getJobOperator().startNextInstance(helloJob);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions())
                .extracting(StepExecution::getStepName)
                .containsExactly("helloStep", "customerCountStep");
    }
}
