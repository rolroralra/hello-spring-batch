package com.example.batch.lesson06;

import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * 누적 금액으로 새 등급을 계산한다. 등급이 그대로라면 null 을 반환해 Writer 로 보내지 않는다.
 */
public class CustomerGradeProcessor implements ItemProcessor<CustomerSales, GradeChange> {

    @Override
    public GradeChange process(CustomerSales item) {
        CustomerGrade newGrade = CustomerGrade.of(item.totalPaidAmount());
        if (newGrade.name().equals(item.currentGrade())) {
            return null;
        }
        return new GradeChange(item.customerId(), item.currentGrade(), newGrade);
    }
}
