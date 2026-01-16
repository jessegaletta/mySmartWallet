package edu.epicode.mysmartwallet.repository;

import edu.epicode.mysmartwallet.model.Account;
import edu.epicode.mysmartwallet.model.Currency;
import edu.epicode.mysmartwallet.model.Transaction;
import edu.epicode.mysmartwallet.model.category.CategoryComponent;
import edu.epicode.mysmartwallet.util.AppLogger;

import java.util.logging.Logger;

/**
 * Singleton container per tutti i repository dell'applicazione.
 * Fornisce accesso centralizzato ai repository di Account, Transaction,
 * CategoryComponent e Currency.
 *
 * Implementa il pattern Singleton con lazy initialization e sincronizzazione
 * per garantire thread-safety.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class DataStorage {

    private static final Logger logger = AppLogger.getLogger(DataStorage.class);

    /**
     * Istanza singleton del DataStorage.
     */
    private static DataStorage instance;

    /**
     * Repository per la gestione degli account.
     */
    private final Repository<Account> accountRepository;

    /**
     * Repository per la gestione delle transazioni.
     */
    private final Repository<Transaction> transactionRepository;

    /**
     * Repository per la gestione delle categorie (Composite pattern).
     */
    private final Repository<CategoryComponent> categoryRepository;

    /**
     * Repository per la gestione delle valute.
     */
    private final Repository<Currency> currencyRepository;

    /**
     * Costruttore privato che inizializza tutti i repository.
     * Accessibile solo tramite getInstance().
     */
    private DataStorage() {
        this.accountRepository = new InMemoryRepository<>();
        this.transactionRepository = new InMemoryRepository<>();
        this.categoryRepository = new InMemoryRepository<>();
        this.currencyRepository = new InMemoryRepository<>();
        logger.info("DataStorage inizializzato con tutti i repository");
    }

    /**
     * Restituisce l'istanza singleton del DataStorage.
     * Utilizza lazy initialization con sincronizzazione per thread-safety.
     *
     * @return l'istanza singleton di DataStorage
     */
    public static synchronized DataStorage getInstance() {
        if (instance == null) {
            instance = new DataStorage();
            logger.fine("Nuova istanza DataStorage creata");
        }
        return instance;
    }

    /**
     * Resetta l'istanza singleton a null.
     * Utile per i test unitari per garantire uno stato pulito.
     */
    public static synchronized void resetInstance() {
        instance = null;
        logger.fine("Istanza DataStorage resettata");
    }

    /**
     * Restituisce il repository degli account.
     *
     * @return il repository per la gestione degli Account
     */
    public Repository<Account> getAccountRepository() {
        return accountRepository;
    }

    /**
     * Restituisce il repository delle transazioni.
     *
     * @return il repository per la gestione delle Transaction
     */
    public Repository<Transaction> getTransactionRepository() {
        return transactionRepository;
    }

    /**
     * Restituisce il repository delle categorie.
     *
     * @return il repository per la gestione dei CategoryComponent
     */
    public Repository<CategoryComponent> getCategoryRepository() {
        return categoryRepository;
    }

    /**
     * Restituisce il repository delle valute.
     *
     * @return il repository per la gestione delle Currency
     */
    public Repository<Currency> getCurrencyRepository() {
        return currencyRepository;
    }
}
