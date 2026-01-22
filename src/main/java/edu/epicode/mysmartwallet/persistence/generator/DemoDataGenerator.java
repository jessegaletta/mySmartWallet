package edu.epicode.mysmartwallet.persistence.generator;

import edu.epicode.mysmartwallet.exception.InvalidInputException;
import edu.epicode.mysmartwallet.model.Account;
import edu.epicode.mysmartwallet.model.Currency;
import edu.epicode.mysmartwallet.model.Transaction;
import edu.epicode.mysmartwallet.model.TransactionType;
import edu.epicode.mysmartwallet.model.category.CategoryComponent;
import edu.epicode.mysmartwallet.model.category.MacroCategory;
import edu.epicode.mysmartwallet.model.category.StandardCategory;
import edu.epicode.mysmartwallet.repository.DataStorage;
import edu.epicode.mysmartwallet.repository.Repository;
import edu.epicode.mysmartwallet.util.AppLogger;
import edu.epicode.mysmartwallet.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.logging.Logger;

/**
 * Generatore di dati demo per test e dimostrazioni.
 *
 * <p>Questa classe estende {@link DataGenerator} e implementa il Template Method pattern
 * per creare un set completo di dati di esempio, inclusi:
 * <ul>
 *   <li>Tre valute: EUR, USD, GBP con tassi di cambio</li>
 *   <li>Struttura completa di categorie gerarchiche</li>
 *   <li>Tre conti esempio in valute diverse</li>
 *   <li>Transazioni di esempio per l'ultimo mese</li>
 * </ul>
 * </p>
 *
 * @author Jesse Galetta
 * @version 1.0
 * @see DataGenerator
 */
public class DemoDataGenerator extends DataGenerator {

    private static final Logger logger = AppLogger.getLogger(DemoDataGenerator.class);

    private final Repository<Currency> currencyRepository;
    private final Repository<CategoryComponent> categoryRepository;
    private final Repository<Account> accountRepository;
    private final Repository<Transaction> transactionRepository;

    /**
     * Crea un nuovo generatore di dati demo.
     */
    public DemoDataGenerator() {
        super();
        DataStorage storage = DataStorage.getInstance();
        this.currencyRepository = storage.getCurrencyRepository();
        this.categoryRepository = storage.getCategoryRepository();
        this.accountRepository = storage.getAccountRepository();
        this.transactionRepository = storage.getTransactionRepository();
    }

    /**
     * Crea le valute di base del sistema.
     * EUR come valuta base con tasso 1.0, USD con tasso 1.10, GBP con tasso 0.86.
     */
    @Override
    protected void createCurrencies() {
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusDays(30);

        // EUR - Valuta base
        Currency eur = new Currency(1, "EUR", "Euro", "€");
        eur.addRate(monthAgo, BigDecimal.ONE);
        eur.addRate(today, BigDecimal.ONE);
        currencyRepository.save(eur);
        logger.fine("Creata valuta EUR");

        // USD - Dollaro Americano
        Currency usd = new Currency(2, "USD", "Dollaro Americano", "$");
        usd.addRate(monthAgo, MoneyUtil.of("1.08"));
        usd.addRate(today, MoneyUtil.of("1.10"));
        currencyRepository.save(usd);
        logger.fine("Creata valuta USD con tasso 1.10");

        // GBP - Sterlina Britannica
        Currency gbp = new Currency(3, "GBP", "Sterlina Britannica", "£");
        gbp.addRate(monthAgo, MoneyUtil.of("0.85"));
        gbp.addRate(today, MoneyUtil.of("0.86"));
        currencyRepository.save(gbp);
        logger.fine("Creata valuta GBP con tasso 0.86");

        logger.info("Create 3 valute: EUR, USD, GBP");
    }

    /**
     * Crea la struttura gerarchica delle categorie.
     * Include macro-categorie per Spese e Entrate con sottocategorie dettagliate.
     */
    @Override
    protected void createCategories() {
        // ===== SPESE (Macro, id=1) =====
        MacroCategory spese = new MacroCategory(1, "Spese", "Categoria principale per le uscite", null);
        categoryRepository.save(spese);

        // Cibo (Macro, id=10, parent=1)
        MacroCategory cibo = new MacroCategory(10, "Cibo", "Spese alimentari", 1);
        categoryRepository.save(cibo);
        spese.addChild(cibo);

        // Ristoranti (Standard, id=100, parent=10)
        StandardCategory ristoranti = new StandardCategory(100, "Ristoranti", "Pranzi e cene fuori", 10);
        categoryRepository.save(ristoranti);
        cibo.addChild(ristoranti);

        // Supermercato (Standard, id=101, parent=10)
        StandardCategory supermercato = new StandardCategory(101, "Supermercato", "Spesa alimentare", 10);
        categoryRepository.save(supermercato);
        cibo.addChild(supermercato);

        // Trasporti (Standard, id=11, parent=1)
        StandardCategory trasporti = new StandardCategory(11, "Trasporti", "Carburante, mezzi pubblici, taxi", 1);
        categoryRepository.save(trasporti);
        spese.addChild(trasporti);

        // Casa (Macro, id=12, parent=1)
        MacroCategory casa = new MacroCategory(12, "Casa", "Spese domestiche", 1);
        categoryRepository.save(casa);
        spese.addChild(casa);

        // Affitto (Standard, id=120, parent=12)
        StandardCategory affitto = new StandardCategory(120, "Affitto", "Canone mensile affitto", 12);
        categoryRepository.save(affitto);
        casa.addChild(affitto);

        // Bollette (Standard, id=121, parent=12)
        StandardCategory bollette = new StandardCategory(121, "Bollette", "Luce, gas, acqua, internet", 12);
        categoryRepository.save(bollette);
        casa.addChild(bollette);

        // ===== ENTRATE (Macro, id=2) =====
        MacroCategory entrate = new MacroCategory(2, "Entrate", "Categoria principale per le entrate", null);
        categoryRepository.save(entrate);

        // Stipendio (Standard, id=20, parent=2)
        StandardCategory stipendio = new StandardCategory(20, "Stipendio", "Stipendio mensile", 2);
        categoryRepository.save(stipendio);
        entrate.addChild(stipendio);

        // Altro (Standard, id=21, parent=2)
        StandardCategory altroEntrate = new StandardCategory(21, "Altro", "Altre entrate occasionali", 2);
        categoryRepository.save(altroEntrate);
        entrate.addChild(altroEntrate);

        // ===== TRASFERIMENTI (Standard, id=3) =====
        StandardCategory trasferimenti = new StandardCategory(3, "Trasferimenti", "Movimenti tra conti", null);
        categoryRepository.save(trasferimenti);

        logger.info("Create 12 categorie con struttura gerarchica");
    }

    /**
     * Crea i conti di esempio.
     * Tre conti: uno principale in EUR, uno PayPal in USD, uno contanti in EUR.
     */
    @Override
    protected void createAccounts() {
        // Conto Principale (EUR, saldo 1000)
        Account contoPrincipale = new Account(1, "Conto Principale", 1, MoneyUtil.of("1000.00"));
        accountRepository.save(contoPrincipale);
        logger.fine("Creato Conto Principale con saldo 1000 EUR");

        // PayPal (USD, saldo 500)
        Account paypal = new Account(2, "PayPal", 2, MoneyUtil.of("500.00"));
        accountRepository.save(paypal);
        logger.fine("Creato conto PayPal con saldo 500 USD");

        // Contanti (EUR, saldo 200)
        Account contanti = new Account(3, "Contanti", 1, MoneyUtil.of("200.00"));
        accountRepository.save(contanti);
        logger.fine("Creato conto Contanti con saldo 200 EUR");

        logger.info("Creati 3 conti: Conto Principale, PayPal, Contanti");
    }

    /**
     * Popola il sistema con transazioni di esempio.
     * Crea un mix di entrate e uscite distribuite nell'ultimo mese.
     */
    @Override
    protected void populateData() {
        LocalDate today = LocalDate.now();

        try {
            // ===== ENTRATE =====
            // Stipendio del mese scorso
            Transaction stipendio = createTransaction(1, 1, TransactionType.INCOME,
                    today.minusDays(25), MoneyUtil.of("2500.00"),
                    "Stipendio mensile", 20);
            addTransactionToAccount(stipendio, 1);

            // Rimborso spese
            Transaction rimborso = createTransaction(2, 1, TransactionType.INCOME,
                    today.minusDays(15), MoneyUtil.of("150.00"),
                    "Rimborso spese aziendali", 21);
            addTransactionToAccount(rimborso, 1);

            // ===== USCITE =====
            // Affitto
            Transaction affitto = createTransaction(3, 1, TransactionType.EXPENSE,
                    today.minusDays(28), MoneyUtil.of("700.00"),
                    "Affitto appartamento", 120);
            addTransactionToAccount(affitto, 1);

            // Spesa supermercato
            Transaction spesa1 = createTransaction(4, 1, TransactionType.EXPENSE,
                    today.minusDays(20), MoneyUtil.of("85.50"),
                    "Spesa settimanale Esselunga", 101);
            addTransactionToAccount(spesa1, 1);

            // Bolletta luce
            Transaction bolletta = createTransaction(5, 1, TransactionType.EXPENSE,
                    today.minusDays(18), MoneyUtil.of("65.00"),
                    "Bolletta Enel luce", 121);
            addTransactionToAccount(bolletta, 1);

            // Cena ristorante
            Transaction cena = createTransaction(6, 3, TransactionType.EXPENSE,
                    today.minusDays(12), MoneyUtil.of("45.00"),
                    "Cena pizzeria con amici", 100);
            addTransactionToAccount(cena, 3);

            // Benzina
            Transaction benzina = createTransaction(7, 3, TransactionType.EXPENSE,
                    today.minusDays(10), MoneyUtil.of("60.00"),
                    "Rifornimento benzina", 11);
            addTransactionToAccount(benzina, 3);

            // Altra spesa supermercato
            Transaction spesa2 = createTransaction(8, 1, TransactionType.EXPENSE,
                    today.minusDays(7), MoneyUtil.of("62.30"),
                    "Spesa supermercato Conad", 101);
            addTransactionToAccount(spesa2, 1);

            // Acquisto online PayPal
            Transaction acquisto = createTransaction(9, 2, TransactionType.EXPENSE,
                    today.minusDays(5), MoneyUtil.of("35.99"),
                    "Amazon - Libro programmazione Java", 100);
            addTransactionToAccount(acquisto, 2);

            // Pranzo fuori
            Transaction pranzo = createTransaction(10, 3, TransactionType.EXPENSE,
                    today.minusDays(2), MoneyUtil.of("15.00"),
                    "Pranzo in pausa lavoro", 100);
            addTransactionToAccount(pranzo, 3);

            logger.info("Create 10 transazioni di esempio");

        } catch (InvalidInputException e) {
            logger.severe("Errore durante la creazione delle transazioni: " + e.getMessage());
        }
    }

    /**
     * Crea una transazione usando il Builder.
     */
    private Transaction createTransaction(int id, int accountId, TransactionType type,
            LocalDate date, BigDecimal amount, String description, int categoryId)
            throws InvalidInputException {

        return new Transaction.Builder()
                .withId(id)
                .withAccountId(accountId)
                .withType(type)
                .withDate(date)
                .withAmount(amount)
                .withDescription(description)
                .withCategoryId(categoryId)
                .build();
    }

    /**
     * Aggiunge una transazione all'account e la salva nel repository.
     */
    private void addTransactionToAccount(Transaction transaction, int accountId) {
        transactionRepository.save(transaction);
        accountRepository.findById(accountId).ifPresent(account -> {
            account.addTransaction(transaction);
            accountRepository.save(account);
        });
    }
}
