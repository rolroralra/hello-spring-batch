package com.example.batch.lesson06;

/**
 * Processor 의 결과: 등급이 바뀌어야 하는 고객.
 */
public record GradeChange(long customerId, String beforeGrade, CustomerGrade afterGrade) {
}
