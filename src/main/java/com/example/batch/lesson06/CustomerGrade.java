package com.example.batch.lesson06;

import java.util.Arrays;

/**
 * 고객 등급 정책: 누적 결제 금액(PAID 주문 합계) 기준.
 */
public enum CustomerGrade {

    VIP(1_000_000L),
    GOLD(500_000L),
    SILVER(200_000L),
    BRONZE(0L);

    private final long minimumAmount;

    CustomerGrade(long minimumAmount) {
        this.minimumAmount = minimumAmount;
    }

    public long minimumAmount() {
        return minimumAmount;
    }

    public static CustomerGrade of(long totalPaidAmount) {
        if (totalPaidAmount < 0) {
            throw new IllegalArgumentException("누적 결제 금액은 음수일 수 없습니다: " + totalPaidAmount);
        }
        // 선언 순서가 높은 등급 -> 낮은 등급이므로, 처음 만족하는 등급이 정답이다.
        return Arrays.stream(values())
                .filter(grade -> totalPaidAmount >= grade.minimumAmount)
                .findFirst()
                .orElse(BRONZE);
    }
}
