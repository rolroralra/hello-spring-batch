package com.example.batch.lesson07;

import org.jooq.Condition;

import java.time.LocalDate;

import static com.example.batch.jooq.Tables.ORDERS;

/**
 * 07 장에서 여러 곳에서 쓰는 jOOQ 조건식. (jOOQ 의 Condition 은 일반 Java 객체라 재사용이 쉽다)
 */
final class DailySalesQueries {

    static final String PAID = "PAID";

    private DailySalesQueries() {
    }

    /**
     * ordered_at 이 targetDate 하루 안에 있고, 결제 완료(PAID)인 주문
     */
    static Condition paidOrdersOn(LocalDate targetDate) {
        return ORDERS.STATUS.eq(PAID)
                .and(ORDERS.ORDERED_AT.ge(targetDate.atStartOfDay()))
                .and(ORDERS.ORDERED_AT.lt(targetDate.plusDays(1).atStartOfDay()));
    }
}
