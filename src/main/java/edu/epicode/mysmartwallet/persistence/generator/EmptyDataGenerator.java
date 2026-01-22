package edu.epicode.mysmartwallet.persistence.generator;

import edu.epicode.mysmartwallet.model.Currency;
import edu.epicode.mysmartwallet.model.category.MacroCategory;
import edu.epicode.mysmartwallet.model.category.StandardCategory;
import edu.epicode.mysmartwallet.repository.DataStorage;
import edu.epicode.mysmartwallet.repository.Repository;
import edu.epicode.mysmartwallet.model.category.CategoryComponent;
import edu.epicode.mysmartwallet.util.AppLogger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.logging.Logger;

/**
 * Generatore di dati vuoto con struttura minima.
 *
 * <p>Questa classe estende {@link DataGenerator} e implementa il Template Method pattern
 * per creare solo la struttura minima necessaria al funzionamento del sistema:
 * <ul>
 *   <li>Solo la valuta base EUR</li>
 *   <li>Categorie minime: Spese, Entrate, Trasferimenti</li>
 *   <li>Nessun account pre-configurato</li>
 *   <li>Nessuna transazione</li>
 * </ul>
 * </p>
 *
 * <p>Utile per inizializzare un nuovo database pulito dove l'utente
 * creerà i propri dati da zero.</p>
 *
 * @author Jesse Galetta
 * @version 1.0
 * @see DataGenerator
 */
public class EmptyDataGenerator extends DataGenerator {

    private static final Logger logger = AppLogger.getLogger(EmptyDataGenerator.class);

    private final Repository<Currency> currencyRepository;
    private final Repository<CategoryComponent> categoryRepository;

    /**
     * Crea un nuovo generatore di dati vuoto.
     */
    public EmptyDataGenerator() {
        super();
        DataStorage storage = DataStorage.getInstance();
        this.currencyRepository = storage.getCurrencyRepository();
        this.categoryRepository = storage.getCategoryRepository();
    }

    /**
     * Crea solo la valuta base EUR.
     * La valuta EUR è necessaria per il funzionamento del sistema.
     */
    @Override
    protected void createCurrencies() {
        LocalDate today = LocalDate.now();

        // EUR - Valuta base (obbligatoria)
        Currency eur = new Currency(1, "EUR", "Euro", "€");
        eur.addRate(today, BigDecimal.ONE);
        currencyRepository.save(eur);

        logger.info("Creata valuta base EUR");
    }

    /**
     * Crea la struttura minima delle categorie.
     * Solo le categorie radice: Spese, Entrate, Trasferimenti.
     */
    @Override
    protected void createCategories() {
        // Spese (Macro, id=1)
        MacroCategory spese = new MacroCategory(1, "Spese", "Categoria principale per le uscite", null);
        categoryRepository.save(spese);

        // Entrate (Macro, id=2)
        MacroCategory entrate = new MacroCategory(2, "Entrate", "Categoria principale per le entrate", null);
        categoryRepository.save(entrate);

        // Trasferimenti (Standard, id=3)
        StandardCategory trasferimenti = new StandardCategory(3, "Trasferimenti", "Movimenti tra conti", null);
        categoryRepository.save(trasferimenti);

        logger.info("Create 3 categorie base: Spese, Entrate, Trasferimenti");
    }

    /**
     * Non crea nessun account.
     * L'utente creerà i propri account manualmente.
     */
    @Override
    protected void createAccounts() {
        // Nessun account da creare
        logger.info("Nessun account pre-configurato (database vuoto)");
    }

    /**
     * Non popola nessun dato aggiuntivo.
     * L'utente aggiungerà le proprie transazioni manualmente.
     */
    @Override
    protected void populateData() {
        // Nessun dato aggiuntivo da creare
        logger.info("Nessuna transazione pre-configurata (database vuoto)");
    }
}
