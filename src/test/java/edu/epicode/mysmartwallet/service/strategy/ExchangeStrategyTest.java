package edu.epicode.mysmartwallet.service.strategy;

import edu.epicode.mysmartwallet.exception.RateNotFoundException;
import edu.epicode.mysmartwallet.model.Currency;
import edu.epicode.mysmartwallet.repository.DataStorage;
import edu.epicode.mysmartwallet.service.CurrencyManager;
import edu.epicode.mysmartwallet.util.MoneyUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
}
