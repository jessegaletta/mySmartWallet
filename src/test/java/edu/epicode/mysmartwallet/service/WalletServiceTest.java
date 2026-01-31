package edu.epicode.mysmartwallet.service;

import edu.epicode.mysmartwallet.exception.InsufficientFundsException;
import edu.epicode.mysmartwallet.exception.InvalidInputException;
import edu.epicode.mysmartwallet.exception.ItemNotFoundException;
import edu.epicode.mysmartwallet.model.Account;
import edu.epicode.mysmartwallet.model.Currency;
import edu.epicode.mysmartwallet.model.TransactionType;
import edu.epicode.mysmartwallet.model.category.StandardCategory;
import edu.epicode.mysmartwallet.repository.DataStorage;
import edu.epicode.mysmartwallet.service.factory.TransactionFactory;
import edu.epicode.mysmartwallet.service.strategy.FixedExchangeStrategy;
import edu.epicode.mysmartwallet.util.MoneyUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per WalletService.
 * Verifica le operazioni principali sui conti e le transazioni.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
class WalletServiceTest {

    private WalletService walletService;
    private int testCategoryId;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        // Reset singletons
        DataStorage.resetInstance();
        CurrencyManager.resetInstance();
        TransactionFactory.resetIdCounter();

        // Inizializza valuta EUR manualmente
        Currency eur = new Currency(1, "EUR", "Euro", "€");
        eur.addRate(LocalDate.now(), BigDecimal.ONE);
        DataStorage.getInstance().getCurrencyRepository().save(eur);

        // Crea WalletService con strategia a tasso fisso (parità)
        walletService = new WalletService(new FixedExchangeStrategy());

        // Crea categoria di test
        testCategoryId = DataStorage.getInstance().getCategoryRepository().generateNextId();
        StandardCategory testCategory = new StandardCategory(
                testCategoryId, "Test Category", "Categoria per test", null);
        DataStorage.getInstance().getCategoryRepository().save(testCategory);

        testDate = LocalDate.now();
    }

    @AfterEach
    void tearDown() {
        DataStorage.resetInstance();
        CurrencyManager.resetInstance();
        TransactionFactory.resetIdCounter();
    }

    @Test
    @DisplayName("createAccount() crea un nuovo conto correttamente")
    void testCreateAccount() throws InvalidInputException, ItemNotFoundException {
        Account account = walletService.createAccount("Conto Test", "EUR", MoneyUtil.of("1000.00"));

        assertNotNull(account);
        assertEquals("Conto Test", account.getName());
        assertEquals(MoneyUtil.of("1000.00"), account.getBalance());
    }

    @Test
    @DisplayName("createAccount() lancia eccezione per valuta inesistente")
    void testCreateAccountInvalidCurrency() {
        assertThrows(ItemNotFoundException.class, () ->
                walletService.createAccount("Conto", "XXX", MoneyUtil.of("100.00")));
    }

    @Test
    @DisplayName("addTransaction() aggiunge entrata correttamente")
    void testAddTransactionIncome() throws Exception {
        Account account = walletService.createAccount("Conto", "EUR", MoneyUtil.of("100.00"));

        walletService.addTransaction(account.getId(), TransactionType.INCOME,
                MoneyUtil.of("50.00"), "Stipendio", testCategoryId, testDate,
                null, null, null);

        Account updated = walletService.getAccount(account.getId());
        assertEquals(MoneyUtil.of("150.00"), updated.getBalance());
        assertEquals(1, updated.getTransactions().size());
    }

    @Test
    @DisplayName("addTransaction() aggiunge uscita correttamente")
    void testAddTransactionExpense() throws Exception {
        Account account = walletService.createAccount("Conto", "EUR", MoneyUtil.of("100.00"));

        walletService.addTransaction(account.getId(), TransactionType.EXPENSE,
                MoneyUtil.of("30.00"), "Spesa", testCategoryId, testDate,
                null, null, null);

        Account updated = walletService.getAccount(account.getId());
        assertEquals(MoneyUtil.of("70.00"), updated.getBalance());
    }

    @Test
    @DisplayName("transfer() trasferisce tra conti stessa valuta")
    void testTransferSameCurrency() throws Exception {
        Account from = walletService.createAccount("Conto A", "EUR", MoneyUtil.of("500.00"));
        Account to = walletService.createAccount("Conto B", "EUR", MoneyUtil.of("100.00"));

        walletService.transfer(from.getId(), to.getId(), MoneyUtil.of("200.00"),
                null, "Trasferimento test", testCategoryId, testDate);

        Account updatedFrom = walletService.getAccount(from.getId());
        Account updatedTo = walletService.getAccount(to.getId());

        assertEquals(MoneyUtil.of("300.00"), updatedFrom.getBalance());
        assertEquals(MoneyUtil.of("300.00"), updatedTo.getBalance());
    }

    @Test
    @DisplayName("transfer() lancia InsufficientFundsException se fondi insufficienti")
    void testTransferInsufficientFunds() throws Exception {
        Account from = walletService.createAccount("Conto A", "EUR", MoneyUtil.of("50.00"));
        Account to = walletService.createAccount("Conto B", "EUR", MoneyUtil.of("100.00"));

        assertThrows(InsufficientFundsException.class, () ->
                walletService.transfer(from.getId(), to.getId(), MoneyUtil.of("100.00"),
                        null, "Trasferimento", testCategoryId, testDate));
    }

    @Test
    @DisplayName("getTotalBalance() calcola saldo totale correttamente")
    void testGetTotalBalance() throws Exception {
        walletService.createAccount("Conto A", "EUR", MoneyUtil.of("500.00"));
        walletService.createAccount("Conto B", "EUR", MoneyUtil.of("300.00"));

        BigDecimal total = walletService.getTotalBalance("EUR", testDate);

        assertEquals(MoneyUtil.of("800.00"), total);
    }

    @Test
    @DisplayName("getAllAccounts() restituisce tutti i conti")
    void testGetAllAccounts() throws Exception {
        walletService.createAccount("Conto A", "EUR", MoneyUtil.of("100.00"));
        walletService.createAccount("Conto B", "EUR", MoneyUtil.of("200.00"));

        assertEquals(2, walletService.getAllAccounts().size());
    }

    @Test
    @DisplayName("getAccount() lancia ItemNotFoundException per ID inesistente")
    void testGetAccountNotFound() {
        assertThrows(ItemNotFoundException.class, () ->
                walletService.getAccount(999));
    }
}
