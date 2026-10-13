package com.example.batch.lesson04;

import com.example.batch.common.JobSummaryListener;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 04 장. JobParameters 와 메타데이터.
 *
 * <ul>
 *   <li>targetDate 파라미터(필수)를 받아 해당 날짜의 주문 통계를 출력한다.</li>
 *   <li>incrementer 가 없으므로 같은 targetDate 로 두 번 실행하면 "이미 완료된 JobInstance" 로 거부된다.</li>
 * </ul>
 *
 * 실행: ./gradlew bootRun --args='--spring.batch.job.name=parameterJob targetDate=2026-10-01,java.time.LocalDate'
 */
@Configuration
public class ParameterJobConfig {

    @Bean
    public Job parameterJob(JobRepository jobRepository,
                            Step orderStatStep,
                            JobSummaryListener jobSummaryListener) {
        return new JobBuilder("parameterJob", jobRepository)
                // 필수 파라미터 검증: targetDate 가 없으면 Job 이 시작조차 되지 않는다.
                .validator(new DefaultJobParametersValidator(new String[]{"targetDate"}, new String[]{}))
                .listener(jobSummaryListener)
                .start(orderStatStep)
                .build();
    }

    @Bean
    public Step orderStatStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              OrderStatTasklet orderStatTasklet) {
        return new StepBuilder("orderStatStep", jobRepository)
                .tasklet(orderStatTasklet, transactionManager)
                .build();
    }
}
