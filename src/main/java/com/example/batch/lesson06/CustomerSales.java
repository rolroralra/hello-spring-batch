package com.example.batch.lesson06;

/**
 * Reader 가 읽어오는 아이템: 고객 + 누적 결제 금액.
 */
public record CustomerSales(long customerId, String name, String currentGrade, long totalPaidAmount) {
}
