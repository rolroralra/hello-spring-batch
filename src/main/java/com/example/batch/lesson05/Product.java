package com.example.batch.lesson05;

/**
 * DB(product 테이블)에 저장할 상품. (출력 모델)
 */
public record Product(String code, String name, long price, int stock) {
}
