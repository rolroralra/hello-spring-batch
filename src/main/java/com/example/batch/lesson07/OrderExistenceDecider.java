package com.example.batch.lesson07;

import org.jooq.DSLContext;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static com.example.batch.jooq.Tables.ORDERS;

/**
 * targetDate 에 결제 완료 주문이 있는지에 따라 흐름을 나눈다.
 *
 * <p>Decider 는 Step 이 아니므로 메타데이터(BATCH_STEP_EXECUTION)에 기록되지 않고,
 * 반환한 FlowExecutionStatus 문자열로 다음 Step 을 고른다.
 */
@Component
public class OrderExistenceDecider implements JobExecutionDecider {

    public static final FlowExecutionStatus HAS_ORDERS = new FlowExecutionStatus("HAS_ORDERS");
    public static final FlowExecutionStatus NO_ORDERS = new FlowExecutionStatus("NO_ORDERS");

    private final DSLContext dsl;

    public OrderExistenceDecider(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        LocalDate targetDate = jobExecution.getJobParameters().getLocalDate("targetDate");

        boolean exists = dsl.fetchExists(ORDERS, DailySalesQueries.paidOrdersOn(targetDate));
        return exists ? HAS_ORDERS : NO_ORDERS;
    }
}
