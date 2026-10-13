package com.example.batch.lesson07;

import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.example.batch.jooq.Tables.DAILY_SALES_SUMMARY;
import static com.example.batch.jooq.Tables.ORDERS;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.currentLocalDateTime;
import static org.jooq.impl.DSL.sum;

/**
 * targetDate 의 매출을 집계해서 daily_sales_summary 에 UPSERT 한다.
 *
 * <p>INSERT ... ON CONFLICT DO UPDATE 를 사용하므로 같은 날짜로 여러 번 실행해도 결과가 같다. (멱등성)
 * 집계 결과는 Step ExecutionContext 에 넣고, ExecutionContextPromotionListener 가 Job ExecutionContext 로 올려준다.
 */
@Component
@StepScope
public class DailySalesAggregateTasklet implements Tasklet {

    public static final String ORDER_COUNT_KEY = "orderCount";
    public static final String TOTAL_AMOUNT_KEY = "totalAmount";

    private static final Logger log = LoggerFactory.getLogger(DailySalesAggregateTasklet.class);

    private final DSLContext dsl;
    private final LocalDate targetDate;

    public DailySalesAggregateTasklet(DSLContext dsl,
                                      @Value("#{jobParameters['targetDate']}") LocalDate targetDate) {
        this.dsl = dsl;
        this.targetDate = targetDate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var summary = dsl.select(count(), coalesce(sum(ORDERS.AMOUNT), BigDecimal.ZERO))
                .from(ORDERS)
                .where(DailySalesQueries.paidOrdersOn(targetDate))
                .fetchSingle();

        int orderCount = summary.value1();
        long totalAmount = summary.value2().longValue();

        dsl.insertInto(DAILY_SALES_SUMMARY)
                .set(DAILY_SALES_SUMMARY.SALES_DATE, targetDate)
                .set(DAILY_SALES_SUMMARY.ORDER_COUNT, orderCount)
                .set(DAILY_SALES_SUMMARY.TOTAL_AMOUNT, totalAmount)
                .onConflict(DAILY_SALES_SUMMARY.SALES_DATE)
                .doUpdate()
                .set(DAILY_SALES_SUMMARY.ORDER_COUNT, orderCount)
                .set(DAILY_SALES_SUMMARY.TOTAL_AMOUNT, totalAmount)
                .set(DAILY_SALES_SUMMARY.CREATED_AT, currentLocalDateTime())
                .execute();

        ExecutionContext stepContext = contribution.getStepExecution().getExecutionContext();
        stepContext.putInt(ORDER_COUNT_KEY, orderCount);
        stepContext.putLong(TOTAL_AMOUNT_KEY, totalAmount);

        log.info("[{}] 집계 완료 - 주문 {}건, 매출 {}원", targetDate, orderCount, totalAmount);
        return RepeatStatus.FINISHED;
    }
}
