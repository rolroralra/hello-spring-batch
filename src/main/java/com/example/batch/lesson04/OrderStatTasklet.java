package com.example.batch.lesson04;

import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.example.batch.jooq.Tables.ORDERS;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.sum;

/**
 * targetDate 의 주문 건수 / 결제 금액 합계를 출력하는 Tasklet.
 *
 * <p>{@code @StepScope} 덕분에 Step 이 실행되는 시점에 빈이 만들어지고,
 * 그때 JobParameters 를 {@code @Value} 로 주입받을 수 있다. (Late Binding)
 */
@Component
@StepScope
public class OrderStatTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(OrderStatTasklet.class);

    private final DSLContext dsl;
    private final LocalDate targetDate;

    public OrderStatTasklet(DSLContext dsl,
                            @Value("#{jobParameters['targetDate']}") LocalDate targetDate) {
        this.dsl = dsl;
        this.targetDate = targetDate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        // SELECT status, count(*), coalesce(sum(amount), 0) FROM orders WHERE ... GROUP BY status
        var result = dsl.select(ORDERS.STATUS, count(), coalesce(sum(ORDERS.AMOUNT), BigDecimal.ZERO))
                .from(ORDERS)
                .where(ORDERS.ORDERED_AT.ge(targetDate.atStartOfDay()))
                .and(ORDERS.ORDERED_AT.lt(targetDate.plusDays(1).atStartOfDay()))
                .groupBy(ORDERS.STATUS)
                .orderBy(ORDERS.STATUS)
                .fetch();

        log.info("[{}] 주문 통계", targetDate);
        result.forEach(row -> log.info("  status={} count={} amount={}", row.value1(), row.value2(), row.value3()));
        if (result.isEmpty()) {
            log.info("  주문이 없습니다.");
        }
        return RepeatStatus.FINISHED;
    }
}
