package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {
    private final PricingCalculator calculator = new PricingCalculator();

    @ParameterizedTest
    @CsvSource({
        "10, 10, regular, , 100.00",
        "10, 11, regular, , 104.50",
        "10, 1, vip, , 9.00",
        "10, 1, wholesale, , 8.50",
        "10, 1, unknown, unknown, 10.00",
        "10, 1, VIP, save10, 10.00",
        "10, 1, , , 10.00",
        "100, 1, regular, SAVE10, 90.00",
        "100, 1, regular, SAVE20PERCENT, 80.00",
        "10, 1, regular, SAVE10, 0.00",
        "9, 1, regular, SAVE10, 0.00",
        "-10, 1, regular, , 0.00",
        "10, -1, regular, , 0.00",
        "10, 0, regular, , 0.00",
        "1000, 1, regular, , 1000.00",
        "1000.01, 1, regular, , 980.01",
        "999.99, 1, regular, , 999.99",
        "1010, 1, regular, SAVE10, 1000.00",
        "1100, 1, vip, , 990.00",
        "100, 11, wholesale, SAVE20PERCENT, 710.60",
        "100, 11, vip, SAVE10, 930.50",
        "2000, 1, wholesale, SAVE20PERCENT, 1332.80",
        "1250, 1, regular, SAVE20PERCENT, 1000.00",
        "1250.01, 1, regular, SAVE20PERCENT, 980.01",
        "1010.01, 1, regular, SAVE10, 980.01",
        "0, 11, wholesale, SAVE20PERCENT, 0.00",
        "1.005, 1, regular, , 1.01",
        "1.004, 1, regular, , 1.00"
    })
    void preservesDiscountsAndBoundaries(String price, int quantity, String type, String coupon, String expected) {
        BigDecimal total = calculator.calculateOrderTotal(
                List.of(new PricingCalculator.LineItem(new BigDecimal(price), quantity)), type, coupon);
        assertThat(total).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void addsLinesBeforeRoundingAndAppliesBulkPerLine() {
        assertThat(calculator.calculateOrderTotal(List.of(
                new PricingCalculator.LineItem(new BigDecimal("0.004"), 1),
                new PricingCalculator.LineItem(new BigDecimal("0.004"), 1)), null, null))
                .isEqualTo(new BigDecimal("0.01"));
        assertThat(calculator.calculateOrderTotal(List.of(
                new PricingCalculator.LineItem(BigDecimal.TEN, 6),
                new PricingCalculator.LineItem(BigDecimal.TEN, 5)), null, null))
                .isEqualByComparingTo("110");
    }

    @Test
    void emptyOrderHasZeroTotalEvenWithCoupon() {
        assertThat(calculator.calculateOrderTotal(List.of(), "vip", "SAVE10"))
                .isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void nullItemsAndPricesRetainTheirOriginalFailure() {
        assertThatThrownBy(() -> calculator.calculateOrderTotal(null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> calculator.calculateOrderTotal(
                List.of(new PricingCalculator.LineItem(null, 1)), null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void clampsOnlyAfterSummingLinesAndApplyingCoupon() {
        assertThat(calculator.calculateOrderTotal(List.of(
                new PricingCalculator.LineItem(new BigDecimal("-5"), 1),
                new PricingCalculator.LineItem(new BigDecimal("20"), 1)), "regular", "SAVE10"))
                .isEqualTo(new BigDecimal("5.00"));
    }
}
