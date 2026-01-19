package edu.epicode.mysmartwallet.service;

import edu.epicode.mysmartwallet.exception.ItemNotFoundException;
import edu.epicode.mysmartwallet.exception.RateNotFoundException;
import edu.epicode.mysmartwallet.model.Currency;
import edu.epicode.mysmartwallet.repository.DataStorage;
import edu.epicode.mysmartwallet.repository.Repository;
import edu.epicode.mysmartwallet.util.AppLogger;
import edu.epicode.mysmartwallet.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Singleton per la gestione centralizzata delle valute.
 *
 * <p>Fornisce accesso alle operazioni sulle valute, inclusa la gestione
 * della valuta base (EUR) e l'aggiornamento dei tassi di cambio storici.</p>
 *
 * <p>Implementa il pattern Singleton con lazy initialization e sincronizzazione
 * per garantire thread-safety.</p>
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class CurrencyManager {

    private static final Logger logger = AppLogger.getLogger(CurrencyManager.class);

    /**
     * Codice della valuta base di default.
     */
    private static final String BASE_CURRENCY_CODE = "EUR";

    /**
     * Istanza singleton del CurrencyManager.
     */
    private static CurrencyManager instance;

    /**
     * Repository per la persistenza delle valute.
     */
    private final Repository<Currency> currencyRepository;

    /**
     * Valuta base del sistema (EUR di default).
     */
    private Currency baseCurrency;

    /**
     * Costruttore privato che inizializza il manager.
     * Recupera il repository da DataStorage e inizializza EUR se necessario.
     */
    private CurrencyManager() {
        this.currencyRepository = DataStorage.getInstance().getCurrencyRepository();
        initializeBaseCurrency();
        logger.info("CurrencyManager inizializzato");
    }

    /**
     * Inizializza la valuta base EUR se non esiste nel repository.
     */
    private void initializeBaseCurrency() {
        try {
            baseCurrency = getCurrencyByCode(BASE_CURRENCY_CODE);
            logger.fine("Valuta base EUR caricata dal repository");
        } catch (ItemNotFoundException e) {
            // EUR non esiste, la creiamo
            int id = currencyRepository.generateNextId();
            baseCurrency = new Currency(id, BASE_CURRENCY_CODE, "Euro", "€");
            baseCurrency.addRate(LocalDate.now(), BigDecimal.ONE);
            currencyRepository.save(baseCurrency);
            logger.fine("Valuta base EUR creata con ID: " + id);
        }
    }

    /**
     * Restituisce l'istanza singleton del CurrencyManager.
     * Utilizza lazy initialization con sincronizzazione per thread-safety.
     *
     * @return l'istanza singleton di CurrencyManager
     */
    public static synchronized CurrencyManager getInstance() {
        if (instance == null) {
            instance = new CurrencyManager();
            logger.fine("Nuova istanza CurrencyManager creata");
        }
        return instance;
    }

    /**
     * Resetta l'istanza singleton a null.
     * Utile per i test unitari per garantire uno stato pulito.
     */
    public static synchronized void resetInstance() {
        instance = null;
        logger.fine("Istanza CurrencyManager resettata");
    }

    /**
     * Restituisce la valuta base del sistema (EUR).
     *
     * @return la valuta base
     */
    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    /**
     * Cerca una valuta per codice ISO.
     *
     * @param code il codice ISO della valuta (es: EUR, USD, GBP)
     * @return la valuta corrispondente
     * @throws ItemNotFoundException se la valuta non esiste
     */
    public Currency getCurrencyByCode(String code) throws ItemNotFoundException {
        return currencyRepository.findAll().stream()
                .filter(c -> c.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException(
                        "Valuta con codice " + code + " non trovata"));
    }

    /**
     * Aggiunge una nuova valuta al repository.
     *
     * @param currency la valuta da aggiungere
     */
    public void addCurrency(Currency currency) {
        currencyRepository.save(currency);
        logger.info("Valuta aggiunta: " + currency.getCode());
    }

    /**
     * Aggiorna il tasso di cambio di una valuta per una data specifica.
     *
     * @param currencyCode il codice della valuta
     * @param date         la data del tasso
     * @param rate         il tasso di cambio rispetto all'EUR
     * @throws ItemNotFoundException se la valuta non esiste
     */
    public void updateRate(String currencyCode, LocalDate date, BigDecimal rate)
            throws ItemNotFoundException {
        Currency currency = getCurrencyByCode(currencyCode);
        currency.addRate(date, rate);
        currencyRepository.save(currency);
        logger.fine("Tasso aggiornato per " + currencyCode + ": " + rate + " in data " + date);
    }

    /**
     * Restituisce il tasso di cambio di una valuta per una data specifica.
     *
     * @param currencyCode il codice della valuta
     * @param date         la data per cui ottenere il tasso
     * @return il tasso di cambio rispetto all'EUR
     * @throws RateNotFoundException se il tasso non è disponibile
     */
    public BigDecimal getRate(String currencyCode, LocalDate date) throws RateNotFoundException {
        try {
            Currency currency = getCurrencyByCode(currencyCode);
            return currency.getRateForDate(date);
        } catch (ItemNotFoundException e) {
            throw new RateNotFoundException(
                    "Impossibile ottenere il tasso: valuta " + currencyCode + " non trovata", e);
        }
    }

    /**
     * Restituisce tutte le valute nel sistema.
     *
     * @return lista di tutte le valute
     */
    public List<Currency> getAllCurrencies() {
        return currencyRepository.findAll();
    }

    /**
     * Inizializza le valute di default con tassi base.
     * Crea EUR, USD e GBP se non esistono già.
     */
    public void initializeDefaultCurrencies() {
        LocalDate today = LocalDate.now();

        // EUR già inizializzato come valuta base
        if (baseCurrency.getRateHistory().isEmpty()) {
            baseCurrency.addRate(today, BigDecimal.ONE);
            currencyRepository.save(baseCurrency);
        }

        // USD
        try {
            getCurrencyByCode("USD");
            logger.fine("USD già presente nel repository");
        } catch (ItemNotFoundException e) {
            int usdId = currencyRepository.generateNextId();
            Currency usd = new Currency(usdId, "USD", "Dollaro Americano", "$");
            usd.addRate(today, MoneyUtil.of("1.10"));
            currencyRepository.save(usd);
            logger.fine("Valuta USD creata con tasso 1.10");
        }

        // GBP
        try {
            getCurrencyByCode("GBP");
            logger.fine("GBP già presente nel repository");
        } catch (ItemNotFoundException e) {
            int gbpId = currencyRepository.generateNextId();
            Currency gbp = new Currency(gbpId, "GBP", "Sterlina Britannica", "£");
            gbp.addRate(today, MoneyUtil.of("0.86"));
            currencyRepository.save(gbp);
            logger.fine("Valuta GBP creata con tasso 0.86");
        }

        logger.info("Valute di default inizializzate");
    }
}
