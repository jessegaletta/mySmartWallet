package edu.epicode.mysmartwallet.service.strategy;

import edu.epicode.mysmartwallet.exception.InsufficientFundsException;
import edu.epicode.mysmartwallet.exception.InvalidInputException;
import edu.epicode.mysmartwallet.exception.ItemNotFoundException;
import edu.epicode.mysmartwallet.exception.RateNotFoundException;
import edu.epicode.mysmartwallet.model.Account;
import edu.epicode.mysmartwallet.model.Currency;
import edu.epicode.mysmartwallet.repository.DataStorage;
import edu.epicode.mysmartwallet.service.CurrencyManager;
import edu.epicode.mysmartwallet.service.WalletService;
import edu.epicode.mysmartwallet.util.MoneyUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per le strategie di conversione valutaria.
 * Verifica il corretto funzionamento di HistoricalExchangeStrategy e FixedExchangeStrategy.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
class ExchangeStrategyTest {

    private Currency eur;
    private Currency usd;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        // Reset singletons per test puliti
        DataStorage.resetInstance();
        CurrencyManager.resetInstance();

        testDate = LocalDate.of(2024, 1, 15);

        // Crea EUR con tasso 1.0 (valuta base)
        eur = new Currency(1, "EUR", "Euro", "€");
        eur.addRate(testDate, BigDecimal.ONE);

        // Crea USD con tasso 1.10 rispetto a EUR
        usd = new Currency(2, "USD", "Dollaro Americano", "$");
        usd.addRate(testDate, MoneyUtil.of("1.10"));
    }

    @AfterEach
    void tearDown() {
        DataStorage.resetInstance();
        CurrencyManager.resetInstance();
    }

    // ==================== Test HistoricalExchangeStrategy ====================

    @Test
    @DisplayName("HistoricalExchangeStrategy: conversione EUR -> USD")
    void testHistoricalConvertEurToUsd() throws RateNotFoundException {
        ExchangeStrategy strategy = new HistoricalExchangeStrategy();
        BigDecimal amount = MoneyUtil.of("100.00");

        // EUR -> USD: 100 * (1.00 / 1.10) = 90.91 (arrotondamento solo sul risultato finale)
        BigDecimal result = strategy.convert(amount, eur, usd, testDate);

        assertEquals(MoneyUtil.of("90.91"), result);
    }

    @Test
    @DisplayName("HistoricalExchangeStrategy: conversione USD -> EUR")
    void testHistoricalConvertUsdToEur() throws RateNotFoundException {
        ExchangeStrategy strategy = new HistoricalExchangeStrategy();
        BigDecimal amount = MoneyUtil.of("100.00");

        // USD -> EUR: 100 * (1.10 / 1.00) = 110.00
        BigDecimal result = strategy.convert(amount, usd, eur, testDate);

        assertEquals(MoneyUtil.of("110.00"), result);
    }

    @Test
    @DisplayName("HistoricalExchangeStrategy: conversione stessa valuta ritorna stesso importo")
    void testHistoricalConvertSameCurrency() throws RateNotFoundException {
        ExchangeStrategy strategy = new HistoricalExchangeStrategy();
        BigDecimal amount = MoneyUtil.of("100.00");

        BigDecimal result = strategy.convert(amount, eur, eur, testDate);

        assertEquals(amount, result);
    }

    @Test
    @DisplayName("HistoricalExchangeStrategy: lancia eccezione se tasso non disponibile")
    void testHistoricalRateNotFound() {
        ExchangeStrategy strategy = new HistoricalExchangeStrategy();
        BigDecimal amount = MoneyUtil.of("100.00");

        // Crea valuta senza tassi
        Currency noRateCurrency = new Currency(3, "XXX", "Test", "X");

        assertThrows(RateNotFoundException.class,
                () -> strategy.convert(amount, noRateCurrency, eur, testDate));
    }

    // ==================== Test FixedExchangeStrategy ====================

    @Test
    @DisplayName("FixedExchangeStrategy: conversione con tasso fisso")
    void testFixedConvertWithRate() throws RateNotFoundException {
        BigDecimal fixedRate = MoneyUtil.of("1.50");
        ExchangeStrategy strategy = new FixedExchangeStrategy(fixedRate);
        BigDecimal amount = MoneyUtil.of("100.00");

        BigDecimal result = strategy.convert(amount, eur, usd, testDate);

        // 100 * 1.50 = 150.00
        assertEquals(MoneyUtil.of("150.00"), result);
    }

    @Test
    @DisplayName("FixedExchangeStrategy: conversione con tasso di parità (1.0)")
    void testFixedConvertWithParityRate() throws RateNotFoundException {
        ExchangeStrategy strategy = new FixedExchangeStrategy();
        BigDecimal amount = MoneyUtil.of("100.00");

        BigDecimal result = strategy.convert(amount, eur, usd, testDate);

        // Tasso 1.0, quindi importo invariato
        assertEquals(MoneyUtil.of("100.00"), result);
    }

    @Test
    @DisplayName("FixedExchangeStrategy: conversione stessa valuta ritorna stesso importo")
    void testFixedConvertSameCurrency() throws RateNotFoundException {
        BigDecimal fixedRate = MoneyUtil.of("2.00");
        ExchangeStrategy strategy = new FixedExchangeStrategy(fixedRate);
        BigDecimal amount = MoneyUtil.of("100.00");

        BigDecimal result = strategy.convert(amount, eur, eur, testDate);

        // Stessa valuta, nessuna conversione applicata
        assertEquals(amount, result);
    }

    @Test
    @DisplayName("FixedExchangeStrategy: getFixedRate restituisce il tasso configurato")
    void testFixedGetFixedRate() {
        BigDecimal expectedRate = MoneyUtil.of("1.25");
        FixedExchangeStrategy strategy = new FixedExchangeStrategy(expectedRate);

        assertEquals(expectedRate, strategy.getFixedRate());
    }

    // ==================== Test WalletService con FixedExchangeStrategy ====================

    /**
     * Questa sezione dimostra il vero valore di FixedExchangeStrategy:
     * testare la logica di WalletService isolando la conversione valutaria.
     *
     * Usando un tasso fisso, i test sono:
     * - Deterministici: stesso input = stesso output, sempre
     * - Indipendenti dai dati: non serve configurare tassi storici
     * - Focalizzati: testano la logica di business, non la conversione
     */
    @Nested
    @DisplayName("WalletService con FixedExchangeStrategy")
    class WalletServiceWithFixedStrategyTest {

        private WalletService walletService;
        private CurrencyManager currencyManager;

        @BeforeEach
        void setUpWalletService() {
            // Usa FixedExchangeStrategy con tasso 1.0 (parità)
            // Così possiamo testare la logica di trasferimento senza
            // preoccuparci dei calcoli di conversione
            walletService = new WalletService(new FixedExchangeStrategy());
            currencyManager = CurrencyManager.getInstance();

            // Registra le valute nel CurrencyManager
            currencyManager.addCurrency(eur);
            currencyManager.addCurrency(usd);
        }

        @Test
        @DisplayName("Transfer tra valute diverse con tasso fisso 1.0: importi uguali")
        void testTransferWithParityRate() throws ItemNotFoundException, InvalidInputException,
                InsufficientFundsException, RateNotFoundException {

            // Crea due conti con valute diverse
            Account eurAccount = walletService.createAccount("Conto EUR", "EUR", MoneyUtil.of("1000.00"));
            Account usdAccount = walletService.createAccount("Conto USD", "USD", MoneyUtil.of("0.00"));

            // Trasferisci 100 EUR -> USD
            walletService.transfer(
                    eurAccount.getId(),
                    usdAccount.getId(),
                    MoneyUtil.of("100.00"),
                    null,  // toAmount null = usa la strategy
                    "Test transfer",
                    1,
                    testDate
            );

            // Con FixedExchangeStrategy(1.0), 100 EUR = 100 USD
            // Questo test verifica che la LOGICA di transfer funzioni,
            // non che la conversione sia corretta (quello lo testa HistoricalExchangeStrategy)
            Account updatedEur = walletService.getAccount(eurAccount.getId());
            Account updatedUsd = walletService.getAccount(usdAccount.getId());

            assertEquals(MoneyUtil.of("900.00"), updatedEur.getBalance(),
                    "Il conto EUR deve avere 900 dopo il trasferimento");
            assertEquals(MoneyUtil.of("100.00"), updatedUsd.getBalance(),
                    "Con tasso 1.0, il conto USD deve ricevere esattamente 100");
        }

        @Test
        @DisplayName("getTotalBalance con tasso fisso: somma diretta dei saldi")
        void testGetTotalBalanceWithFixedRate() throws ItemNotFoundException, InvalidInputException,
                RateNotFoundException {

            // Crea conti con valute diverse
            walletService.createAccount("EUR Account", "EUR", MoneyUtil.of("500.00"));
            walletService.createAccount("USD Account", "USD", MoneyUtil.of("300.00"));

            // Con tasso 1.0, il totale è semplicemente 500 + 300 = 800
            BigDecimal total = walletService.getTotalBalance("EUR", testDate);

            assertEquals(MoneyUtil.of("800.00"), total,
                    "Con tasso 1.0, il totale deve essere la somma diretta");
        }

        @Test
        @DisplayName("Transfer con fondi insufficienti lancia eccezione")
        void testTransferInsufficientFunds() throws ItemNotFoundException, InvalidInputException {

            Account eurAccount = walletService.createAccount("Conto EUR", "EUR", MoneyUtil.of("50.00"));
            Account usdAccount = walletService.createAccount("Conto USD", "USD", MoneyUtil.of("0.00"));

            // Questo test verifica la logica di validazione fondi,
            // indipendentemente dalla conversione valutaria
            assertThrows(InsufficientFundsException.class, () ->
                    walletService.transfer(
                            eurAccount.getId(),
                            usdAccount.getId(),
                            MoneyUtil.of("100.00"),  // più del saldo disponibile
                            null,
                            "Test",
                            1,
                            testDate
                    )
            );
        }
    }

    // ==================== Confronto: stesso test con tasso fisso diverso ====================

    @Nested
    @DisplayName("Confronto comportamento con tassi diversi")
    class RateComparisonTest {

        @BeforeEach
        void setUpCurrencies() {
            CurrencyManager.getInstance().addCurrency(eur);
            CurrencyManager.getInstance().addCurrency(usd);
        }

        @Test
        @DisplayName("Stesso transfer, tassi diversi: risultati prevedibili")
        void testTransferWithDifferentRates() throws ItemNotFoundException, InvalidInputException,
                InsufficientFundsException, RateNotFoundException {

            // Test con tasso 2.0
            WalletService serviceDouble = new WalletService(new FixedExchangeStrategy(MoneyUtil.of("2.00")));
            Account eurDouble = serviceDouble.createAccount("EUR 2x", "EUR", MoneyUtil.of("100.00"));
            Account usdDouble = serviceDouble.createAccount("USD 2x", "USD", MoneyUtil.of("0.00"));

            serviceDouble.transfer(eurDouble.getId(), usdDouble.getId(),
                    MoneyUtil.of("50.00"), null, "Test", 1, testDate);

            // Con tasso 2.0: 50 EUR -> 100 USD
            assertEquals(MoneyUtil.of("100.00"),
                    serviceDouble.getAccount(usdDouble.getId()).getBalance());

            // Reset per secondo test
            DataStorage.resetInstance();
            CurrencyManager.resetInstance();
            CurrencyManager.getInstance().addCurrency(eur);
            CurrencyManager.getInstance().addCurrency(usd);

            // Test con tasso 0.5
            WalletService serviceHalf = new WalletService(new FixedExchangeStrategy(MoneyUtil.of("0.50")));
            Account eurHalf = serviceHalf.createAccount("EUR 0.5x", "EUR", MoneyUtil.of("100.00"));
            Account usdHalf = serviceHalf.createAccount("USD 0.5x", "USD", MoneyUtil.of("0.00"));

            serviceHalf.transfer(eurHalf.getId(), usdHalf.getId(),
                    MoneyUtil.of("50.00"), null, "Test", 1, testDate);

            // Con tasso 0.5: 50 EUR -> 25 USD
            assertEquals(MoneyUtil.of("25.00"),
                    serviceHalf.getAccount(usdHalf.getId()).getBalance());
        }
    }
}
