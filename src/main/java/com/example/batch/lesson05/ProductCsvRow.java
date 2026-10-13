package com.example.batch.lesson05;

/**
 * CSV 한 줄(code,name,price,stock)을 그대로 담는 레코드. (입력 모델)
 */
public record ProductCsvRow(String code, String name, long price, int stock) {
}
