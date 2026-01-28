package edu.epicode.mysmartwallet.model;

import edu.epicode.mysmartwallet.exception.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per la classe Transaction e il suo Builder.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
class TransactionTest {

    @Test
    @DisplayName("Builder crea Transaction con tutti i campi correttamente impostati")
    void testBuilderCreatesCorrectTransaction() throws InvalidInputException {
        LocalDate date = LocalDate.of(2024, 1, 15);
        BigDecimal amount = new BigDecimal("100.00");

        Transaction transaction = new Transaction.Builder()
                .withId(1)
                .withDate(date)
                .withAmount(amount)
                .withType(TransactionType.EXPENSE)
                .withDescription("Spesa test")
                .withCategoryId(10)
                .withAccountId(1)
                .build();

        assertEquals(1, transaction.getId());
        assertEquals(date, transaction.getDate());
        assertEquals(amount, transaction.getAmount());
        assertEquals(TransactionType.EXPENSE, transaction.getType());
        assertEquals("Spesa test", transaction.getDescription());
        assertEquals(10, transaction.getCategoryId());
        assertEquals(1, transaction.getAccountId());
    }

    @Test
    @DisplayName("Builder lancia eccezione per amount negativo")
    void testBuilderThrowsExceptionForNegativeAmount() {
        BigDecimal negativeAmount = new BigDecimal("-50.00");

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Transaction.Builder()
                        .withId(1)
                        .withDate(LocalDate.now())
                        .withAmount(negativeAmount)
                        .withType(TransactionType.EXPENSE)
                        .build()
        );

        assertTrue(exception.getMessage().contains("positivo"));
    }

    @Test
    @DisplayName("Builder lancia eccezione per amount zero")
    void testBuilderThrowsExceptionForZeroAmount() {
        assertThrows(
                InvalidInputException.class,
                () -> new Transaction.Builder()
                        .withId(1)
                        .withDate(LocalDate.now())
                        .withAmount(BigDecimal.ZERO)
                        .withType(TransactionType.INCOME)
                        .build()
        );
    }

    @Test
    @DisplayName("Builder lancia eccezione per amount null")
    void testBuilderThrowsExceptionForNullAmount() {
        assertThrows(
                InvalidInputException.class,
                () -> new Transaction.Builder()
                        .withId(1)
                        .withDate(LocalDate.now())
                        .withAmount(null)
                        .withType(TransactionType.INCOME)
                        .build()
        );
    }

    @Test
    @DisplayName("Builder lancia eccezione per date null")
    void testBuilderThrowsExceptionForNullDate() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Transaction.Builder()
                        .withId(1)
                        .withDate(null)
                        .withAmount(new BigDecimal("100.00"))
                        .withType(TransactionType.EXPENSE)
                        .build()
        );

        assertTrue(exception.getMessage().contains("data"));
    }

    @Test
    @DisplayName("Builder lancia eccezione per type null")
    void testBuilderThrowsExceptionForNullType() {
        assertThrows(
                InvalidInputException.class,
                () -> new Transaction.Builder()
                        .withId(1)
                        .withDate(LocalDate.now())
                        .withAmount(new BigDecimal("100.00"))
                        .withType(null)
                        .build()
        );
    }

    @Test
    @DisplayName("Transaction e' immutabile - getDate non modifica stato")
    void testImmutabilityGetDate() throws InvalidInputException {
        LocalDate originalDate = LocalDate.of(2024, 1, 15);

        Transaction transaction = new Transaction.Builder()
                .withId(1)
                .withDate(originalDate)
                .withAmount(new BigDecimal("100.00"))
                .withType(TransactionType.EXPENSE)
                .build();

        LocalDate retrievedDate = transaction.getDate();
        assertEquals(originalDate, retrievedDate);
        assertEquals(originalDate, transaction.getDate());
    }

    @Test
    @DisplayName("Transaction e' immutabile - getAmount non modifica stato")
    void testImmutabilityGetAmount() throws InvalidInputException {
        BigDecimal originalAmount = new BigDecimal("100.00");

        Transaction transaction = new Transaction.Builder()
                .withId(1)
                .withDate(LocalDate.now())
                .withAmount(originalAmount)
                .withType(TransactionType.INCOME)
                .build();

        BigDecimal retrievedAmount = transaction.getAmount();
        assertEquals(originalAmount, retrievedAmount);
        assertEquals(originalAmount, transaction.getAmount());
    }

    @Test
    @DisplayName("Transaction e' immutabile - chiamate successive getters restituiscono stessi valori")
    void testImmutabilityMultipleGetterCalls() throws InvalidInputException {
        Transaction transaction = new Transaction.Builder()
                .withId(5)
                .withDate(LocalDate.of(2024, 6, 15))
                .withAmount(new BigDecimal("250.00"))
                .withType(TransactionType.TRANSFER)
                .withDescription("Trasferimento test")
                .withCategoryId(20)
                .withAccountId(2)
                .build();

        // Chiamate multiple ai getter devono restituire gli stessi valori
        assertEquals(transaction.getId(), transaction.getId());
        assertEquals(transaction.getDate(), transaction.getDate());
        assertEquals(transaction.getAmount(), transaction.getAmount());
        assertEquals(transaction.getType(), transaction.getType());
        assertEquals(transaction.getDescription(), transaction.getDescription());
        assertEquals(transaction.getCategoryId(), transaction.getCategoryId());
        assertEquals(transaction.getAccountId(), transaction.getAccountId());
    }

    @Test
    @DisplayName("Builder supporta costruzione fluente")
    void testFluentBuilderPattern() throws InvalidInputException {
        Transaction.Builder builder = new Transaction.Builder();

        Transaction transaction = builder
                .withId(1)
                .withDate(LocalDate.now())
                .withAmount(new BigDecimal("75.50"))
                .withType(TransactionType.EXPENSE)
                .withDescription("Test fluent")
                .withCategoryId(5)
                .withAccountId(1)
                .build();

        assertNotNull(transaction);
        assertEquals("Test fluent", transaction.getDescription());
    }

    @Test
    @DisplayName("toString contiene informazioni principali")
    void testToString() throws InvalidInputException {
        Transaction transaction = new Transaction.Builder()
                .withId(1)
                .withDate(LocalDate.of(2024, 1, 15))
                .withAmount(new BigDecimal("50.00"))
                .withType(TransactionType.EXPENSE)
                .withDescription("Spesa test")
                .withCategoryId(10)
                .withAccountId(1)
                .build();

        String str = transaction.toString();

        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("Spesa test"));
    }
}
