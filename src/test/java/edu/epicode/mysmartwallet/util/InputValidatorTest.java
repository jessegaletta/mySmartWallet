package edu.epicode.mysmartwallet.util;

import edu.epicode.mysmartwallet.exception.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per la classe InputValidator.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
class InputValidatorTest {

    @Test
    @DisplayName("validatePositiveAmount accetta valore positivo")
    void testValidatePositiveAmountValid() {
        BigDecimal validAmount = new BigDecimal("100.00");
        assertDoesNotThrow(() -> InputValidator.validatePositiveAmount(validAmount));
    }

    @Test
    @DisplayName("validatePositiveAmount lancia eccezione per valore negativo")
    void testValidatePositiveAmountNegative() {
        BigDecimal negativeAmount = new BigDecimal("-50.00");
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> InputValidator.validatePositiveAmount(negativeAmount)
        );
        assertTrue(exception.getMessage().contains("positivo"));
    }

    @Test
    @DisplayName("validatePositiveAmount lancia eccezione per zero")
    void testValidatePositiveAmountZero() {
        BigDecimal zero = BigDecimal.ZERO;
        assertThrows(InvalidInputException.class, () -> InputValidator.validatePositiveAmount(zero));
    }

    @Test
    @DisplayName("validatePositiveAmount lancia eccezione per null")
    void testValidatePositiveAmountNull() {
        assertThrows(InvalidInputException.class, () -> InputValidator.validatePositiveAmount(null));
    }

    @Test
    @DisplayName("validateNotEmpty accetta stringa valida")
    void testValidateNotEmptyValid() {
        assertDoesNotThrow(() -> InputValidator.validateNotEmpty("Testo valido", "campo"));
    }

    @Test
    @DisplayName("validateNotEmpty lancia eccezione per stringa vuota")
    void testValidateNotEmptyEmptyString() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> InputValidator.validateNotEmpty("", "nomeCampo")
        );
        assertTrue(exception.getMessage().contains("nomeCampo"));
        assertTrue(exception.getMessage().contains("vuoto"));
    }

    @Test
    @DisplayName("validateNotEmpty lancia eccezione per stringa null")
    void testValidateNotEmptyNull() {
        assertThrows(InvalidInputException.class, () -> InputValidator.validateNotEmpty(null, "campo"));
    }

    @Test
    @DisplayName("validateNotEmpty lancia eccezione per stringa con soli spazi")
    void testValidateNotEmptyWhitespace() {
        assertThrows(InvalidInputException.class, () -> InputValidator.validateNotEmpty("   ", "campo"));
    }

    @Test
    @DisplayName("parseAmount converte formato valido")
    void testParseAmountValid() throws InvalidInputException {
        BigDecimal result = InputValidator.parseAmount("100.50");
        assertEquals(new BigDecimal("100.50"), result);
    }

    @Test
    @DisplayName("parseAmount converte formato con virgola")
    void testParseAmountWithComma() throws InvalidInputException {
        BigDecimal result = InputValidator.parseAmount("100,50");
        assertEquals(new BigDecimal("100.50"), result);
    }

    @Test
    @DisplayName("parseAmount gestisce spazi iniziali e finali")
    void testParseAmountWithWhitespace() throws InvalidInputException {
        BigDecimal result = InputValidator.parseAmount("  50.00  ");
        assertEquals(new BigDecimal("50.00"), result);
    }

    @Test
    @DisplayName("parseAmount lancia eccezione per formato invalido")
    void testParseAmountInvalid() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> InputValidator.parseAmount("abc")
        );
        assertTrue(exception.getMessage().contains("non valido"));
    }

    @Test
    @DisplayName("parseAmount lancia eccezione per stringa vuota")
    void testParseAmountEmpty() {
        assertThrows(InvalidInputException.class, () -> InputValidator.parseAmount(""));
    }

    @Test
    @DisplayName("parseAmount lancia eccezione per null")
    void testParseAmountNull() {
        assertThrows(InvalidInputException.class, () -> InputValidator.parseAmount(null));
    }

    @Test
    @DisplayName("validateId accetta ID positivo")
    void testValidateIdValid() {
        assertDoesNotThrow(() -> InputValidator.validateId(1));
    }

    @Test
    @DisplayName("validateId lancia eccezione per ID zero")
    void testValidateIdZero() {
        assertThrows(InvalidInputException.class, () -> InputValidator.validateId(0));
    }

    @Test
    @DisplayName("validateId lancia eccezione per ID negativo")
    void testValidateIdNegative() {
        assertThrows(InvalidInputException.class, () -> InputValidator.validateId(-1));
    }

    @Test
    @DisplayName("validateNotNull accetta oggetto non null")
    void testValidateNotNullValid() {
        assertDoesNotThrow(() -> InputValidator.validateNotNull("test", "campo"));
    }

    @Test
    @DisplayName("validateNotNull lancia eccezione per null")
    void testValidateNotNullWithNull() {
        assertThrows(InvalidInputException.class, () -> InputValidator.validateNotNull(null, "campo"));
    }
}
