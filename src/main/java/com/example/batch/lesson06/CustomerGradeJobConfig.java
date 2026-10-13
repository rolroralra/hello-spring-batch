package com.example.batch.lesson06;

import com.example.batch.common.JobSummaryListener;
import com.example.batch.common.JooqPagingItemReader;
import org.jooq.DSLContext;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;

import static com.example.batch.jooq.Tables.CUSTOMER;
import static com.example.batch.jooq.Tables.ORDERS;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.sum;
import static org.jooq.impl.DSL.val;

/**
 * 06 장. DB -> DB 처리 - 고객별 누적 결제 금액으로 등급을 다시 매긴다.
 *
 * <pre>
 * customerGradeJob
 *  └─ customerGradeStep (chunk = 10)
 *       Reader    : JooqPagingItemReader  (customer LEFT JOIN orders, GROUP BY, 페이지 단위 조회)
 *       Processor : CustomerGradeProcessor (등급 계산, 변경 없으면 filter)
 *       Writer    : CustomerGradeJooqWriter (jOOQ batch update)
 * </pre>
 *
 * 실행: ./gradlew bootRun --args='--spring.batch.job.name=customerGradeJob'
 */
@Configuration
public class CustomerGradeJobConfig {

    static final int CHUNK_SIZE = 10;
    static final String PAID = "PAID";

    @Bean
    public Job customerGradeJob(JobRepository jobRepository,
                                Step customerGradeStep,
                                JobSummaryListener jobSummaryListener) {
        return new JobBuilder("customerGradeJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobSummaryListener)
                .start(customerGradeStep)
                .build();
    }

    @Bean
    public Step customerGradeStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  JooqPagingItemReader<CustomerSales> customerSalesReader,
                                  DSLContext dsl) {
        return new StepBuilder("customerGradeStep", jobRepository)
                .<CustomerSales, GradeChange>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(customerSalesReader)
                .processor(new CustomerGradeProcessor())
                .writer(new CustomerGradeJooqWriter(dsl))
                .build();
    }

    /**
     * <pre>
     * SELECT c.id, c.name, c.grade, COALESCE(SUM(o.amount), 0)
     *   FROM customer c
     *   LEFT JOIN orders o ON o.customer_id = c.id AND o.status = 'PAID'
     *  GROUP BY c.id, c.name, c.grade
     *  ORDER BY c.id
     *  OFFSET ? LIMIT ?
     * </pre>
     */
    @Bean
    @StepScope
    public JooqPagingItemReader<CustomerSales> customerSalesReader(DSLContext dsl) {
        var totalPaid = coalesce(sum(ORDERS.AMOUNT), val(BigDecimal.ZERO));

        return new JooqPagingItemReader<>("customerSalesReader", CHUNK_SIZE, (offset, limit) ->
                dsl.select(CUSTOMER.ID, CUSTOMER.NAME, CUSTOMER.GRADE, totalPaid)
                        .from(CUSTOMER)
                        .leftJoin(ORDERS)
                        .on(ORDERS.CUSTOMER_ID.eq(CUSTOMER.ID).and(ORDERS.STATUS.eq(PAID)))
                        .groupBy(CUSTOMER.ID, CUSTOMER.NAME, CUSTOMER.GRADE)
                        .orderBy(CUSTOMER.ID)
                        .offset(offset)
                        .limit(limit)
                        .fetch(row -> new CustomerSales(
                                row.value1(), row.value2(), row.value3(), row.value4().longValue())));
    }
}
