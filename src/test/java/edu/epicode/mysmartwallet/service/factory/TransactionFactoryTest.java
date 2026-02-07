package edu.epicode.mysmartwallet.service.factory;

import edu.epicode.mysmartwallet.exception.InvalidInputException;
import edu.epicode.mysmartwallet.model.Transaction;
import edu.epicode.mysmartwallet.model.TransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per la classe TransactionFactory (Factory Pattern).
 * Verifica la creazione di transazioni INCOME, EXPENSE, TRANSFER_OUT e TRANSFER_IN.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
class TransactionFactoryTest {

    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        TransactionFactory.resetIdCounter();
        testDate = LocalDate.now();
    }

    @AfterEach
    void tearDown() {
        TransactionFactory.resetIdCounter();
    }

    @Test
    @DisplayName("createIncome() crea transazione INCOME corretta")
    void testCreateIncome() throws InvalidInputException {
        Transaction income = TransactionFactory.createIncome(
                new BigDecimal("100.00"),
                "Stipendio",
                1,  // categoryId
                1,  // accountId
                testDate,
                null, null, null
        );

        assertNotNull(income);
        assertEquals(TransactionType.INCOME, income.getType());
        assertEquals(new BigDecimal("100.00"), income.getAmount());
        assertEquals("Stipendio", income.getDescription());
    }

    @Test
    @DisplayName("createExpense() crea transazione EXPENSE corretta")
    void testCreateExpense() throws InvalidInputException {
        Transaction expense = TransactionFactory.createExpense(
                new BigDecimal("50.00"),
                "Spesa alimentari",
                10, // categoryId
                1,  // accountId
                testDate,
                null, null, null
        );

        assertNotNull(expense);
        assertEquals(TransactionType.EXPENSE, expense.getType());
        assertEquals(new BigDecimal("50.00"), expense.getAmount());
    }

    @Test
    @DisplayName("createTransfer() crea coppia di transazioni TRANSFER_OUT/TRANSFER_IN")
    void testCreateTransfer() throws InvalidInputException {
        List<Transaction> transfers = TransactionFactory.createTransfer(
                new BigDecimal("200.00"),  // fromAmount
                new BigDecimal("200.00"),  // toAmount
                "Trasferimento",
                1,  // fromAccountId
                2,  // toAccountId
                5,  // categoryId
                testDate,
                null, null, null  // no conversione
        );

        assertEquals(2, transfers.size());

        // Prima transazione: uscita (TRANSFER_OUT)
        Transaction outgoing = transfers.get(0);
        assertEquals(TransactionType.TRANSFER_OUT, outgoing.getType());
        assertEquals(1, outgoing.getAccountId());
        assertTrue(outgoing.getDescription().contains("uscita"));

        // Seconda transazione: entrata (TRANSFER_IN)
        Transaction incoming = transfers.get(1);
        assertEquals(TransactionType.TRANSFER_IN, incoming.getType());
        assertEquals(2, incoming.getAccountId());
        assertTrue(incoming.getDescription().contains("entrata"));
    }

    @Test
    @DisplayName("createTransfer() lancia eccezione se fromAccountId == toAccountId")
    void testCreateTransferSameAccount() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> TransactionFactory.createTransfer(
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"),
                        "Trasferimento",
                        1, 1, 5, testDate,
                        null, null, null
                )
        );

        assertTrue(exception.getMessage().contains("uguali"));
    }

    @Test
    @DisplayName("Factory lancia eccezione per descrizione vuota")
    void testEmptyDescription() {
        assertThrows(InvalidInputException.class,
                () -> TransactionFactory.createExpense(
                        new BigDecimal("50.00"),
                        "",
                        1, 1, testDate,
                        null, null, null
                ));
    }

    @Test
    @DisplayName("ID transazioni sono univoci e incrementali")
    void testUniqueIds() throws InvalidInputException {
        Transaction t1 = TransactionFactory.createIncome(
                new BigDecimal("10.00"), "T1", 1, 1, testDate,
                null, null, null);
        Transaction t2 = TransactionFactory.createExpense(
                new BigDecimal("20.00"), "T2", 1, 1, testDate,
                null, null, null);

        assertEquals(1, t1.getId());
        assertEquals(2, t2.getId());
    }
}
