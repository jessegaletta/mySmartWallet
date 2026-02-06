package edu.epicode.mysmartwallet.persistence.generator;

import edu.epicode.mysmartwallet.exception.StorageException;
import edu.epicode.mysmartwallet.persistence.CsvService;
import edu.epicode.mysmartwallet.util.AppLogger;

import java.util.logging.Logger;

/**
 * Classe astratta che implementa il Template Method pattern per la generazione dei dati.
 *
 * Il Template Method pattern definisce lo scheletro di un algoritmo nella classe base,
 * delegando alcuni passaggi alle sottoclassi. Questo permette alle sottoclassi di ridefinire
 * determinati passi dell'algoritmo senza modificarne la struttura complessiva.
 *
 * In questo caso, il metodo template {@link #generate()} definisce l'ordine in cui
 * i dati devono essere creati (valute, categorie, account, dati popolati),
 * mentre le implementazioni concrete determinano quali dati specifici creare.
 *
 * Le sottoclassi concrete sono:
 * <ul>
 *   <li>{@link DemoDataGenerator} - genera dati di esempio per test e demo</li>
 *   <li>{@link EmptyDataGenerator} - genera solo la struttura minima necessaria</li>
 * </ul>
 *
 * @author Jesse Galetta
 * @version 1.0
 * @see DemoDataGenerator
 * @see EmptyDataGenerator
 */
public abstract class DataGenerator {

    /**
     * Logger per le sottoclassi.
     */
    protected static final Logger logger = AppLogger.getLogger(DataGenerator.class);

    /**
     * Costruttore protetto per le sottoclassi.
     */
    protected DataGenerator() {
        // Costruttore vuoto per le sottoclassi
    }

    /**
     * Metodo template che definisce l'algoritmo di generazione dei dati.
     * Questo metodo è final e non può essere sovrascritto dalle sottoclassi.
     *
     * L'ordine delle operazioni è:
     * <ol>
     *   <li>Creazione delle valute ({@link #createCurrencies()})</li>
     *   <li>Creazione delle categorie ({@link #createCategories()})</li>
     *   <li>Creazione degli account ({@link #createAccounts()})</li>
     *   <li>Popolazione dei dati aggiuntivi ({@link #populateData()})</li>
     *   <li>Salvataggio su file ({@link #saveData()})</li>
     * </ol>
     * 
     *
     * @throws StorageException se si verifica un errore durante la generazione o il salvataggio
     */
    public final void generate() throws StorageException {
        logger.info("Avvio generazione dati con " + getClass().getSimpleName());

        logger.fine("Step 1: Creazione valute...");
        createCurrencies();
        logger.fine("Step 1 completato: Valute create");

        logger.fine("Step 2: Creazione categorie...");
        createCategories();
        logger.fine("Step 2 completato: Categorie create");

        logger.fine("Step 3: Creazione account...");
        createAccounts();
        logger.fine("Step 3 completato: Account creati");

        logger.fine("Step 4: Popolazione dati aggiuntivi...");
        populateData();
        logger.fine("Step 4 completato: Dati aggiuntivi popolati");

        logger.fine("Step 5: Salvataggio su file...");
        saveData();
        logger.fine("Step 5 completato: Dati salvati su file");

        logger.info("Generazione dati completata con successo");
    }

    /**
     * Crea le valute nel sistema.
     * Le sottoclassi devono implementare questo metodo per definire
     * quali valute creare e con quali tassi di cambio.
     */
    protected abstract void createCurrencies();

    /**
     * Crea la struttura delle categorie nel sistema.
     * Le sottoclassi devono implementare questo metodo per definire
     * la gerarchia delle categorie (macro e standard).
     */
    protected abstract void createCategories();

    /**
     * Crea gli account iniziali nel sistema.
     * Le sottoclassi devono implementare questo metodo per definire
     * quali account creare e con quali saldi iniziali.
     */
    protected abstract void createAccounts();

    /**
     * Popola il sistema con dati aggiuntivi (es. transazioni di esempio).
     * Le sottoclassi devono implementare questo metodo per definire
     * eventuali dati aggiuntivi da creare dopo gli elementi base.
     */
    protected abstract void populateData();

    /**
     * Salva tutti i dati generati su file CSV.
     * Questo metodo è concreto e utilizza {@link CsvService#saveAll()}.
     *
     * @throws StorageException se si verifica un errore durante il salvataggio
     */
    protected void saveData() throws StorageException {
        CsvService.saveAll();
    }
}
