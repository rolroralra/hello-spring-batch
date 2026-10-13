package com.example.batch.lesson06;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerGradeProcessorTest {

    private final CustomerGradeProcessor processor = new CustomerGradeProcessor();

    @Test
    void 등급이_바뀌면_GradeChange_를_반환한다() {
        GradeChange change = processor.process(new CustomerSales(1L, "김민준", "BRONZE", 600_000));

        assertThat(change).isEqualTo(new GradeChange(1L, "BRONZE", CustomerGrade.GOLD));
    }

    @Test
    void 등급이_그대로면_null_을_반환해_Writer_로_보내지_않는다() {
        assertThat(processor.process(new CustomerSales(1L, "김민준", "GOLD", 600_000))).isNull();
    }
}
