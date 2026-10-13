package com.example.batch.lesson06;

import com.example.batch.support.BatchIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;

import static com.example.batch.jooq.Tables.CUSTOMER;
import static com.example.batch.jooq.Tables.ORDERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.sum;
import static org.jooq.impl.DSL.val;

class CustomerGradeJobTest extends BatchIntegrationTestSupport {

    @Autowired
    private Job customerGradeJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(customerGradeJob);
    }

    @Test
    void 모든_고객의_등급이_누적_결제_금액과_일치하게_된다() throws Exception {
        JobExecution execution = jobOperatorTestUtils.getJobOperator().startNextInstance(customerGradeJob);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Map<Long, Long> paidByCustomer = dsl.select(CUSTOMER.ID, coalesce(sum(ORDERS.AMOUNT), val(BigDecimal.ZERO)))
                .from(CUSTOMER)
                .leftJoin(ORDERS).on(ORDERS.CUSTOMER_ID.eq(CUSTOMER.ID).and(ORDERS.STATUS.eq("PAID")))
                .groupBy(CUSTOMER.ID)
                .fetchMap(r -> r.value1(), r -> r.value2().longValue());

        Map<Long, String> gradeByCustomer = dsl.select(CUSTOMER.ID, CUSTOMER.GRADE)
                .from(CUSTOMER)
                .fetchMap(r -> r.value1(), r -> r.value2());

        assertThat(gradeByCustomer).hasSize(20);
        gradeByCustomer.forEach((id, grade) ->
                assertThat(grade)
                        .as("customer %d", id)
                        .isEqualTo(CustomerGrade.of(paidByCustomer.get(id)).name()));
    }

    @Test
    void 등급이_그대로인_고객은_filter_되고_바뀐_고객만_write_된다() throws Exception {
        JobExecution execution = jobOperatorTestUtils.getJobOperator().startNextInstance(customerGradeJob);

        StepExecution step = execution.getStepExecutions().iterator().next();
        assertThat(step.getReadCount()).isEqualTo(20);
        assertThat(step.getFilterCount() + step.getWriteCount()).isEqualTo(20);
        assertThat(step.getWriteCount()).isPositive();

        // 한 번 더 돌리면 이미 모두 맞는 등급이므로 전부 filter 된다. (멱등성)
        JobExecution second = jobOperatorTestUtils.getJobOperator().startNextInstance(customerGradeJob);
        StepExecution secondStep = second.getStepExecutions().iterator().next();
        assertThat(secondStep.getWriteCount()).isZero();
        assertThat(secondStep.getFilterCount()).isEqualTo(20);
    }
}
