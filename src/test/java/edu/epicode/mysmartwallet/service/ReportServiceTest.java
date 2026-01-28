package edu.epicode.mysmartwallet.service;

import edu.epicode.mysmartwallet.exception.InvalidInputException;
import edu.epicode.mysmartwallet.model.Account;
import edu.epicode.mysmartwallet.model.Transaction;
import edu.epicode.mysmartwallet.model.TransactionType;
import edu.epicode.mysmartwallet.repository.DataStorage;
import edu.epicode.mysmartwallet.util.MoneyUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per la classe ReportService.
 * Verifica le funzionalità di reportistica e aggregazione dati.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
class ReportServiceTest {

    private ReportService reportService;
    private Account testAccount;
    private int accountId;

    @BeforeEach
    void setUp() throws InvalidInputException {
        // Reset singleton per ogni test
        DataStorage.resetInstance();

        DataStorage dataStorage = DataStorage.getInstance();

        // Crea account di test
        testAccount = new Account(1, "Conto Test", 1, MoneyUtil.of("1000.00"));
        dataStorage.getAccountRepository().save(testAccount);
        accountId = testAccount.getId();

        // Aggiungi transazioni di test
        addTestTransactions();

        // Crea il report service
        reportService = new ReportService();
    }

    @AfterEach
    void tearDown() {
        DataStorage.resetInstance();
    }

    private void addTestTransactions() throws InvalidInputException {
        // EXPENSE: 50.00 categoria 10
        Transaction expense1 = new Transaction.Builder()
                .withId(1)
                .withAccountId(accountId)
                .withType(TransactionType.EXPENSE)
                .withDate(LocalDate.of(2024, 1, 15))
                .withAmount(MoneyUtil.of("50.00"))
                .withDescription("Spesa supermercato")
                .withCategoryId(10)
                .build();
        testAccount.addTransaction(expense1);

        // EXPENSE: 100.00 categoria 10
        Transaction expense2 = new Transaction.Builder()
                .withId(2)
                .withAccountId(accountId)
                .withType(TransactionType.EXPENSE)
                .withDate(LocalDate.of(2024, 1, 20))
                .withAmount(MoneyUtil.of("100.00"))
                .withDescription("Spesa alimentari")
                .withCategoryId(10)
                .build();
        testAccount.addTransaction(expense2);

        // EXPENSE: 30.00 categoria 20
        Transaction expense3 = new Transaction.Builder()
                .withId(3)
                .withAccountId(accountId)
                .withType(TransactionType.EXPENSE)
                .withDate(LocalDate.of(2024, 2, 5))
                .withAmount(MoneyUtil.of("30.00"))
                .withDescription("Trasporto")
                .withCategoryId(20)
                .build();
        testAccount.addTransaction(expense3);

        // INCOME: 500.00 categoria 30
        Transaction income1 = new Transaction.Builder()
                .withId(4)
                .withAccountId(accountId)
                .withType(TransactionType.INCOME)
                .withDate(LocalDate.of(2024, 1, 1))
                .withAmount(MoneyUtil.of("500.00"))
                .withDescription("Stipendio")
                .withCategoryId(30)
                .build();
        testAccount.addTransaction(income1);

        // INCOME: 200.00 categoria 30
        Transaction income2 = new Transaction.Builder()
                .withId(5)
                .withAccountId(accountId)
                .withType(TransactionType.INCOME)
                .withDate(LocalDate.of(2024, 2, 1))
                .withAmount(MoneyUtil.of("200.00"))
                .withDescription("Bonus")
                .withCategoryId(30)
                .build();
        testAccount.addTransaction(income2);
    }

    @Test
    @DisplayName("getTotalExpenses calcola correttamente il totale delle uscite")
    void testGetTotalExpenses() {
        BigDecimal totalExpenses = reportService.getTotalExpenses(accountId);

        // 50 + 100 + 30 = 180
        assertEquals(MoneyUtil.of("180.00"), totalExpenses);
    }

    @Test
    @DisplayName("getTotalExpenses restituisce zero per account senza uscite")
    void testGetTotalExpensesNoExpenses() throws InvalidInputException {
        // Crea account senza transazioni
        Account emptyAccount = new Account(2, "Conto Vuoto", 1, MoneyUtil.of("500.00"));
        DataStorage.getInstance().getAccountRepository().save(emptyAccount);

        BigDecimal totalExpenses = reportService.getTotalExpenses(emptyAccount.getId());

        assertEquals(BigDecimal.ZERO, totalExpenses);
    }

    @Test
    @DisplayName("getTotalIncome calcola correttamente il totale delle entrate")
    void testGetTotalIncome() {
        BigDecimal totalIncome = reportService.getTotalIncome(accountId);

        // 500 + 200 = 700
        assertEquals(MoneyUtil.of("700.00"), totalIncome);
    }

    @Test
    @DisplayName("getTotalIncome restituisce zero per account senza entrate")
    void testGetTotalIncomeNoIncome() throws InvalidInputException {
        Account emptyAccount = new Account(3, "Conto Solo Spese", 1, MoneyUtil.of("100.00"));
        DataStorage.getInstance().getAccountRepository().save(emptyAccount);

        // Aggiungi solo una spesa
        Transaction expense = new Transaction.Builder()
                .withId(10)
                .withAccountId(3)
                .withType(TransactionType.EXPENSE)
                .withDate(LocalDate.now())
                .withAmount(MoneyUtil.of("25.00"))
                .withDescription("Spesa")
                .withCategoryId(10)
                .build();
        emptyAccount.addTransaction(expense);

        BigDecimal totalIncome = reportService.getTotalIncome(emptyAccount.getId());

        assertEquals(BigDecimal.ZERO, totalIncome);
    }

    @Test
    @DisplayName("getExpensesByCategory raggruppa correttamente le spese")
    void testGetExpensesByCategory() {
        Map<Integer, BigDecimal> expensesByCategory = reportService.getExpensesByCategory(accountId);

        // Categoria 10: 50 + 100 = 150
        assertEquals(MoneyUtil.of("150.00"), expensesByCategory.get(10));

        // Categoria 20: 30
        assertEquals(MoneyUtil.of("30.00"), expensesByCategory.get(20));

        // Solo 2 categorie con spese
        assertEquals(2, expensesByCategory.size());
    }

    @Test
    @DisplayName("getExpensesByCategory restituisce mappa vuota per account senza spese")
    void testGetExpensesByCategoryEmpty() throws InvalidInputException {
        Account incomeOnly = new Account(4, "Solo Entrate", 1, MoneyUtil.of("0.00"));
        DataStorage.getInstance().getAccountRepository().save(incomeOnly);

        Transaction income = new Transaction.Builder()
                .withId(20)
                .withAccountId(4)
                .withType(TransactionType.INCOME)
                .withDate(LocalDate.now())
                .withAmount(MoneyUtil.of("100.00"))
                .withDescription("Entrata")
                .withCategoryId(30)
                .build();
        incomeOnly.addTransaction(income);

        Map<Integer, BigDecimal> result = reportService.getExpensesByCategory(incomeOnly.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getTransactionsByPeriod filtra correttamente per periodo")
    void testGetTransactionsByPeriod() {
        LocalDate from = LocalDate.of(2024, 1, 10);
        LocalDate to = LocalDate.of(2024, 1, 25);

        List<Transaction> transactions = reportService.getTransactionsByPeriod(accountId, from, to);

        // Transazioni nel periodo: 15/01 (spesa), 20/01 (spesa)
        assertEquals(2, transactions.size());
        assertTrue(transactions.stream().allMatch(
                t -> !t.getDate().isBefore(from) && !t.getDate().isAfter(to)
        ));
    }

    @Test
    @DisplayName("getTransactionsByPeriod include estremi del periodo")
    void testGetTransactionsByPeriodInclusive() {
        LocalDate from = LocalDate.of(2024, 1, 15);
        LocalDate to = LocalDate.of(2024, 1, 15);

        List<Transaction> transactions = reportService.getTransactionsByPeriod(accountId, from, to);

        // Solo la transazione del 15/01
        assertEquals(1, transactions.size());
        assertEquals(LocalDate.of(2024, 1, 15), transactions.get(0).getDate());
    }

    @Test
    @DisplayName("getTransactionsByPeriod restituisce lista ordinata per data")
    void testGetTransactionsByPeriodSorted() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);

        List<Transaction> transactions = reportService.getTransactionsByPeriod(accountId, from, to);

        for (int i = 1; i < transactions.size(); i++) {
            assertTrue(transactions.get(i).getDate().compareTo(
                    transactions.get(i - 1).getDate()) >= 0);
        }
    }

    @Test
    @DisplayName("getLargestExpense trova la spesa maggiore")
    void testGetLargestExpense() {
        Optional<Transaction> largest = reportService.getLargestExpense(accountId);

        assertTrue(largest.isPresent());
        assertEquals(MoneyUtil.of("100.00"), largest.get().getAmount());
        assertEquals("Spesa alimentari", largest.get().getDescription());
    }

    @Test
    @DisplayName("getLargestExpense restituisce Optional vuoto per account senza spese")
    void testGetLargestExpenseNoExpenses() throws InvalidInputException {
        Account noExpenses = new Account(5, "Solo Income", 1, MoneyUtil.of("0.00"));
        DataStorage.getInstance().getAccountRepository().save(noExpenses);

        Transaction income = new Transaction.Builder()
                .withId(30)
                .withAccountId(5)
                .withType(TransactionType.INCOME)
                .withDate(LocalDate.now())
                .withAmount(MoneyUtil.of("1000.00"))
                .withDescription("Stipendio")
                .withCategoryId(30)
                .build();
        noExpenses.addTransaction(income);

        Optional<Transaction> largest = reportService.getLargestExpense(noExpenses.getId());

        assertTrue(largest.isEmpty());
    }

    @Test
    @DisplayName("getAverageExpense calcola correttamente la media")
    void testGetAverageExpense() {
        BigDecimal average = reportService.getAverageExpense(accountId);

        // (50 + 100 + 30) / 3 = 60
        assertEquals(MoneyUtil.of("60.00"), average);
    }

    @Test
    @DisplayName("getTransactionsByType filtra correttamente per tipo")
    void testGetTransactionsByType() {
        List<Transaction> expenses = reportService.getTransactionsByType(accountId, TransactionType.EXPENSE);
        List<Transaction> incomes = reportService.getTransactionsByType(accountId, TransactionType.INCOME);

        assertEquals(3, expenses.size());
        assertEquals(2, incomes.size());
        assertTrue(expenses.stream().allMatch(t -> t.getType() == TransactionType.EXPENSE));
        assertTrue(incomes.stream().allMatch(t -> t.getType() == TransactionType.INCOME));
    }

    @Test
    @DisplayName("searchByDescription trova transazioni per parola chiave")
    void testSearchByDescription() {
        List<Transaction> results = reportService.searchByDescription(accountId, "spesa");

        // "Spesa supermercato" e "Spesa alimentari"
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("searchByDescription e' case insensitive")
    void testSearchByDescriptionCaseInsensitive() {
        List<Transaction> resultsUpper = reportService.searchByDescription(accountId, "SPESA");
        List<Transaction> resultsLower = reportService.searchByDescription(accountId, "spesa");

        assertEquals(resultsUpper.size(), resultsLower.size());
    }

    @Test
    @DisplayName("getTransactionCountByType conta correttamente per tipo")
    void testGetTransactionCountByType() {
        Map<TransactionType, Long> counts = reportService.getTransactionCountByType(accountId);

        assertEquals(3L, counts.get(TransactionType.EXPENSE));
        assertEquals(2L, counts.get(TransactionType.INCOME));
    }

    @Test
    @DisplayName("metodi restituiscono valori corretti per account inesistente")
    void testMethodsForNonExistentAccount() {
        int nonExistentId = 999;

        assertEquals(BigDecimal.ZERO, reportService.getTotalExpenses(nonExistentId));
        assertEquals(BigDecimal.ZERO, reportService.getTotalIncome(nonExistentId));
        assertTrue(reportService.getExpensesByCategory(nonExistentId).isEmpty());
        assertTrue(reportService.getTransactionsByPeriod(nonExistentId,
                LocalDate.now().minusMonths(1), LocalDate.now()).isEmpty());
        assertTrue(reportService.getLargestExpense(nonExistentId).isEmpty());
    }
}
