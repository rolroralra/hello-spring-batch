package com.example.batch.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Job 이 끝나면 Step 별 처리 건수를 요약해서 로그로 남긴다.
 * 모든 실습 Job 에서 공통으로 사용한다.
 */
@Component
public class JobSummaryListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobSummaryListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(">>> [{}] 시작 - parameters={}",
                jobExecution.getJobInstance().getJobName(), jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("<<< [{}] 종료 - status={}, exitStatus={}, elapsed={}ms",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus(),
                jobExecution.getExitStatus().getExitCode(),
                elapsedMillis(jobExecution));

        for (StepExecution step : jobExecution.getStepExecutions()) {
            log.info("    - step={} status={} read={} filter={} write={} skip={} commit={} rollback={}",
                    step.getStepName(), step.getStatus(),
                    step.getReadCount(), step.getFilterCount(), step.getWriteCount(),
                    step.getSkipCount(), step.getCommitCount(), step.getRollbackCount());
        }
    }

    private long elapsedMillis(JobExecution jobExecution) {
        if (jobExecution.getStartTime() == null || jobExecution.getEndTime() == null) {
            return -1;
        }
        return Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis();
    }
}
