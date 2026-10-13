package com.example.batch.lesson06;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerGradeTest {

    @ParameterizedTest(name = "{0}원 -> {1}")
    @CsvSource({
            "0, BRONZE",
            "199999, BRONZE",
            "200000, SILVER",
            "499999, SILVER",
            "500000, GOLD",
            "999999, GOLD",
            "1000000, VIP",
            "99999999, VIP"
    })
    void 누적_금액_경계값으로_등급을_결정한다(long amount, CustomerGrade expected) {
        assertThat(CustomerGrade.of(amount)).isEqualTo(expected);
    }

    @Test
    void 음수_금액은_허용하지_않는다() {
        assertThatThrownBy(() -> CustomerGrade.of(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
