package com.example.batch.lesson07;

import com.example.batch.common.JobSummaryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 07 장. 여러 Step 과 흐름 제어(Flow) - 일별 매출 집계.
 *
 * <pre>
 * dailySalesJob (targetDate 필수)
 *  └─ orderExistenceDecider
 *       ├─ NO_ORDERS  -> noOrdersStep   (경고 로그만 남기고 종료)
 *       └─ HAS_ORDERS -> aggregateStep  (집계 + UPSERT, 결과를 ExecutionContext 에 저장)
 *                          -> reportStep (ExecutionContext 값으로 리포트 파일 생성)
 * </pre>
 *
 * 실행: ./gradlew bootRun --args='--spring.batch.job.name=dailySalesJob targetDate=2026-10-01,java.time.LocalDate'
 */
@Configuration
public class DailySalesJobConfig {

    private static final Logger log = LoggerFactory.getLogger(DailySalesJobConfig.class);

    @Bean
    public Job dailySalesJob(JobRepository jobRepository,
                             OrderExistenceDecider orderExistenceDecider,
                             Step aggregateStep,
                             Step reportStep,
                             Step noOrdersStep,
                             JobSummaryListener jobSummaryListener) {
        return new JobBuilder("dailySalesJob", jobRepository)
                .validator(new DefaultJobParametersValidator(new String[]{"targetDate"}, new String[]{}))
                .listener(jobSummaryListener)
                .start(orderExistenceDecider)
                    .on(OrderExistenceDecider.NO_ORDERS.getName()).to(noOrdersStep)
                .from(orderExistenceDecider)
                    .on(OrderExistenceDecider.HAS_ORDERS.getName()).to(aggregateStep).next(reportStep)
                .end()
                .build();
    }

    @Bean
    public Step aggregateStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              DailySalesAggregateTasklet dailySalesAggregateTasklet) {
        return new StepBuilder("aggregateStep", jobRepository)
                .tasklet(dailySalesAggregateTasklet, transactionManager)
                .listener(promotionListener())
                .build();
    }

    @Bean
    public Step reportStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           DailySalesReportTasklet dailySalesReportTasklet) {
        return new StepBuilder("reportStep", jobRepository)
                .tasklet(dailySalesReportTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step noOrdersStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("noOrdersStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.warn("집계할 주문이 없습니다. parameters={}",
                            contribution.getStepExecution().getJobParameters());
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * Step ExecutionContext 의 지정한 키를 Step 이 끝날 때 Job ExecutionContext 로 복사한다.
     */
    private ExecutionContextPromotionListener promotionListener() {
        var listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[]{
                DailySalesAggregateTasklet.ORDER_COUNT_KEY,
                DailySalesAggregateTasklet.TOTAL_AMOUNT_KEY});
        return listener;
    }
}
