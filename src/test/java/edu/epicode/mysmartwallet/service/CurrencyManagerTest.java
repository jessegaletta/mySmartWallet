package edu.epicode.mysmartwallet.service;

import edu.epicode.mysmartwallet.exception.ItemNotFoundException;
import edu.epicode.mysmartwallet.exception.RateNotFoundException;
import edu.epicode.mysmartwallet.model.Currency;
import edu.epicode.mysmartwallet.repository.DataStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per la classe CurrencyManager (Singleton).
 * Verifica la gestione delle valute, la protezione EUR
 * e l'aggiornamento dei tassi di cambio.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
class CurrencyManagerTest {

    @BeforeEach
    void setUp() {
        DataStorage.resetInstance();
        CurrencyManager.resetInstance();
    }

    @AfterEach
    void tearDown() {
        DataStorage.resetInstance();
        CurrencyManager.resetInstance();
    }

    @Test
    @DisplayName("getInstance() restituisce sempre la stessa istanza (Singleton)")
    void testSingletonInstance() {
        CurrencyManager first = CurrencyManager.getInstance();
        CurrencyManager second = CurrencyManager.getInstance();

        assertSame(first, second);
    }

    @Test
    @DisplayName("getBaseCurrency() restituisce EUR")
    void testGetBaseCurrency() {
        CurrencyManager manager = CurrencyManager.getInstance();

        Currency base = manager.getBaseCurrency();

        assertNotNull(base);
        assertEquals("EUR", base.getCode());
    }

    @Test
    @DisplayName("getCurrencyByCode() trova valuta esistente")
    void testGetCurrencyByCode() throws ItemNotFoundException {
        CurrencyManager manager = CurrencyManager.getInstance();

        // EUR viene creato automaticamente
        Currency eur = manager.getCurrencyByCode("EUR");

        assertNotNull(eur);
        assertEquals("Euro", eur.getName());
    }

    @Test
    @DisplayName("getCurrencyByCode() lancia eccezione per valuta inesistente")
    void testGetCurrencyByCodeNotFound() {
        CurrencyManager manager = CurrencyManager.getInstance();

        assertThrows(ItemNotFoundException.class,
                () -> manager.getCurrencyByCode("XXX"));
    }

    @Test
    @DisplayName("deleteCurrency() impedisce eliminazione di EUR")
    void testDeleteCurrencyProtectsEUR() {
        CurrencyManager manager = CurrencyManager.getInstance();
        Currency eur = manager.getBaseCurrency();

        assertThrows(IllegalStateException.class,
                () -> manager.deleteCurrency(eur.getId()));
    }

    @Test
    @DisplayName("addCurrency() e deleteCurrency() funzionano per valute non-base")
    void testAddAndDeleteCurrency() throws ItemNotFoundException {
        CurrencyManager manager = CurrencyManager.getInstance();

        // Aggiungi USD
        int id = manager.generateNextId();
        Currency usd = new Currency(id, "USD", "US Dollar", "$");
        usd.addRate(LocalDate.now(), new BigDecimal("1.10"));
        manager.addCurrency(usd);

        // Verifica che esista
        Currency found = manager.getCurrencyByCode("USD");
        assertEquals("USD", found.getCode());

        // Elimina USD
        manager.deleteCurrency(id);

        // Verifica che non esista piu'
        assertThrows(ItemNotFoundException.class,
                () -> manager.getCurrencyByCode("USD"));
    }

    @Test
    @DisplayName("updateRate() aggiorna tasso di cambio per data specifica")
    void testUpdateRate() throws ItemNotFoundException, RateNotFoundException {
        CurrencyManager manager = CurrencyManager.getInstance();

        // Aggiungi valuta
        int id = manager.generateNextId();
        Currency gbp = new Currency(id, "GBP", "British Pound", "£");
        gbp.addRate(LocalDate.now(), new BigDecimal("0.85"));
        manager.addCurrency(gbp);

        // Aggiorna tasso per una data specifica
        LocalDate targetDate = LocalDate.of(2024, 6, 1);
        manager.updateRate("GBP", targetDate, new BigDecimal("0.87"));

        // Verifica che il tasso sia stato aggiornato
        Currency updated = manager.getCurrencyByCode("GBP");
        assertEquals(new BigDecimal("0.87"), updated.getRateForDate(targetDate));
    }
}
