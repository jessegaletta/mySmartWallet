package edu.epicode.mysmartwallet.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per la classe MoneyUtil.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
class MoneyUtilTest {

    @Test
    @DisplayName("of(String) crea BigDecimal corretto")
    void testOfString() {
        BigDecimal result = MoneyUtil.of("100.50");
        assertEquals(new BigDecimal("100.50"), result);
    }

    @Test
    @DisplayName("of(double) crea BigDecimal corretto")
    void testOfDouble() {
        BigDecimal result = MoneyUtil.of(100.5);
        assertEquals(new BigDecimal("100.50"), result);
    }

    @Test
    @DisplayName("add somma correttamente due importi")
    void testAdd() {
        BigDecimal a = MoneyUtil.of("100.00");
        BigDecimal b = MoneyUtil.of("50.50");
        BigDecimal result = MoneyUtil.add(a, b);
        assertEquals(new BigDecimal("150.50"), result);
    }

    @Test
    @DisplayName("subtract sottrae correttamente due importi")
    void testSubtract() {
        BigDecimal a = MoneyUtil.of("100.00");
        BigDecimal b = MoneyUtil.of("30.50");
        BigDecimal result = MoneyUtil.subtract(a, b);
        assertEquals(new BigDecimal("69.50"), result);
    }

    @Test
    @DisplayName("multiply moltiplica correttamente due importi")
    void testMultiply() {
        BigDecimal a = MoneyUtil.of("10.00");
        BigDecimal b = MoneyUtil.of("3.00");
        BigDecimal result = MoneyUtil.multiply(a, b);
        assertEquals(new BigDecimal("30.00"), result);
    }

    @Test
    @DisplayName("divide divide correttamente con arrotondamento")
    void testDivide() {
        BigDecimal a = MoneyUtil.of("100.00");
        BigDecimal b = MoneyUtil.of("3.00");
        BigDecimal result = MoneyUtil.divide(a, b);
        assertEquals(new BigDecimal("33.33"), result);
    }

    @Test
    @DisplayName("divide lancia eccezione per divisione per zero")
    void testDivideByZero() {
        BigDecimal a = MoneyUtil.of("100.00");
        BigDecimal b = BigDecimal.ZERO;
        assertThrows(ArithmeticException.class, () -> MoneyUtil.divide(a, b));
    }

    @Test
    @DisplayName("isPositive ritorna true per importi positivi")
    void testIsPositiveTrue() {
        assertTrue(MoneyUtil.isPositive(MoneyUtil.of("100.00")));
    }

    @Test
    @DisplayName("isPositive ritorna false per zero")
    void testIsPositiveZero() {
        assertFalse(MoneyUtil.isPositive(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("isPositive ritorna false per importi negativi")
    void testIsPositiveNegative() {
        assertFalse(MoneyUtil.isPositive(MoneyUtil.of("-100.00")));
    }

    @Test
    @DisplayName("isNegative ritorna true per importi negativi")
    void testIsNegativeTrue() {
        assertTrue(MoneyUtil.isNegative(MoneyUtil.of("-50.00")));
    }

    @Test
    @DisplayName("isNegative ritorna false per importi positivi")
    void testIsNegativeFalse() {
        assertFalse(MoneyUtil.isNegative(MoneyUtil.of("50.00")));
    }

    @Test
    @DisplayName("isGreaterThan confronta correttamente due importi")
    void testIsGreaterThan() {
        BigDecimal a = MoneyUtil.of("100.00");
        BigDecimal b = MoneyUtil.of("50.00");
        assertTrue(MoneyUtil.isGreaterThan(a, b));
        assertFalse(MoneyUtil.isGreaterThan(b, a));
        assertFalse(MoneyUtil.isGreaterThan(a, a));
    }

    @Test
    @DisplayName("format formatta correttamente l'importo con simbolo")
    void testFormat() {
        BigDecimal amount = MoneyUtil.of("1234.56");
        String result = MoneyUtil.format(amount, "€");
        assertEquals("1234.56 €", result);
    }

    @Test
    @DisplayName("format gestisce importi con diversi simboli valuta")
    void testFormatDifferentCurrencies() {
        BigDecimal amount = MoneyUtil.of("100.00");
        assertEquals("100.00 $", MoneyUtil.format(amount, "$"));
        assertEquals("100.00 £", MoneyUtil.format(amount, "£"));
    }
}
