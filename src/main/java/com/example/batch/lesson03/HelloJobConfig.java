package com.example.batch.lesson03;

import com.example.batch.common.JobSummaryListener;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import static com.example.batch.jooq.Tables.CUSTOMER;

/**
 * 03 장. 첫 번째 Job - Tasklet 기반 Step 두 개로 구성된 가장 단순한 Job.
 *
 * <pre>
 * helloJob
 *  ├─ helloStep          : "Hello, Spring Batch!" 출력
 *  └─ customerCountStep  : jOOQ 로 고객 수 조회 후 출력
 * </pre>
 *
 * 실행: ./gradlew bootRun --args='--spring.batch.job.name=helloJob'
 */
@Configuration
public class HelloJobConfig {

    private static final Logger log = LoggerFactory.getLogger(HelloJobConfig.class);

    @Bean
    public Job helloJob(JobRepository jobRepository,
                        Step helloStep,
                        Step customerCountStep,
                        JobSummaryListener jobSummaryListener) {
        return new JobBuilder("helloJob", jobRepository)
                // 실행할 때마다 run.id 파라미터를 1씩 증가시켜 "새로운 JobInstance" 로 실행되게 한다.
                // 주의(Spring Batch 6): incrementer 가 있으면 실행 시 전달한 JobParameters 는 무시된다.
                .incrementer(new RunIdIncrementer())
                .listener(jobSummaryListener)
                .start(helloStep)
                .next(customerCountStep)
                .build();
    }

    @Bean
    public Step helloStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("helloStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("Hello, Spring Batch!");
                    // FINISHED: 이 Tasklet 은 한 번만 실행하고 끝낸다. (CONTINUABLE 이면 반복 실행)
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step customerCountStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  DSLContext dsl) {
        return new StepBuilder("customerCountStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    int customerCount = dsl.fetchCount(CUSTOMER);
                    log.info("현재 등록된 고객 수: {}명", customerCount);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
