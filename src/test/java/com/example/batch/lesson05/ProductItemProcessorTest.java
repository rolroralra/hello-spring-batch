package com.example.batch.lesson05;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.validator.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductItemProcessorTest {

    private final ProductItemProcessor processor = new ProductItemProcessor();

    @Test
    void 코드는_대문자로_이름은_공백을_제거해서_변환한다() {
        Product product = processor.process(new ProductCsvRow(" p-001 ", "  무선 마우스 ", 25_000, 10));

        assertThat(product).isEqualTo(new Product("P-001", "무선 마우스", 25_000, 10));
    }

    @Test
    void 가격이_0원이면_null_을_반환해_필터링한다() {
        assertThat(processor.process(new ProductCsvRow("p-013", "사은품", 0, 100))).isNull();
    }

    @Test
    void 가격이나_재고가_음수이면_ValidationException() {
        assertThatThrownBy(() -> processor.process(new ProductCsvRow("p-1", "음수 가격", -1, 1)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> processor.process(new ProductCsvRow("p-2", "음수 재고", 1, -1)))
                .isInstanceOf(ValidationException.class);
    }
}
