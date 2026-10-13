package com.example.batch.lesson06;

import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import static com.example.batch.jooq.Tables.CUSTOMER;

/**
 * jOOQ batch update: chunk 안의 UPDATE 문들을 JDBC batch 로 한 번에 전송한다.
 */
public class CustomerGradeJooqWriter implements ItemWriter<GradeChange> {

    private static final Logger log = LoggerFactory.getLogger(CustomerGradeJooqWriter.class);

    private final DSLContext dsl;

    public CustomerGradeJooqWriter(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public void write(Chunk<? extends GradeChange> chunk) {
        var updates = chunk.getItems().stream()
                .map(change -> dsl.update(CUSTOMER)
                        .set(CUSTOMER.GRADE, change.afterGrade().name())
                        .where(CUSTOMER.ID.eq(change.customerId())))
                .toList();

        dsl.batch(updates).execute();

        chunk.forEach(change -> log.info("고객 {} 등급 변경: {} -> {}",
                change.customerId(), change.beforeGrade(), change.afterGrade()));
    }
}
