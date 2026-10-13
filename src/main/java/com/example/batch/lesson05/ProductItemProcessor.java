package com.example.batch.lesson05;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.validator.ValidationException;

import java.util.Locale;

/**
 * CSV 행을 검증/정제해서 저장할 상품으로 변환한다.
 *
 * <ul>
 *   <li>code 는 앞뒤 공백 제거 후 대문자로 통일</li>
 *   <li>가격이 0 원인 상품(사은품)은 적재 대상이 아니므로 null 을 반환해 걸러낸다(filter)</li>
 *   <li>가격/재고가 음수이면 잘못된 데이터이므로 예외를 던진다</li>
 * </ul>
 */
public class ProductItemProcessor implements ItemProcessor<ProductCsvRow, Product> {

    @Override
    public Product process(ProductCsvRow row) {
        if (row.price() < 0 || row.stock() < 0) {
            throw new ValidationException("가격/재고는 음수일 수 없습니다: " + row);
        }
        if (row.price() == 0) {
            return null; // null 반환 = 이 아이템은 Writer 로 넘기지 않는다. (filterCount 증가)
        }
        return new Product(
                row.code().trim().toUpperCase(Locale.ROOT),
                row.name().trim(),
                row.price(),
                row.stock());
    }
}
