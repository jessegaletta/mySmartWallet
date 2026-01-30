package edu.epicode.mysmartwallet.persistence;

import edu.epicode.mysmartwallet.exception.InvalidInputException;
import edu.epicode.mysmartwallet.exception.StorageException;
import edu.epicode.mysmartwallet.model.Account;
import edu.epicode.mysmartwallet.model.Currency;
import edu.epicode.mysmartwallet.model.Transaction;
import edu.epicode.mysmartwallet.model.TransactionType;
import edu.epicode.mysmartwallet.model.category.CategoryComponent;
import edu.epicode.mysmartwallet.model.category.MacroCategory;
import edu.epicode.mysmartwallet.model.category.StandardCategory;
import edu.epicode.mysmartwallet.repository.DataStorage;
import edu.epicode.mysmartwallet.util.AppLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Servizio per la persistenza dei dati su file CSV.
 * Gestisce la lettura e scrittura di account, transazioni, categorie e valute.
 *
 * <p>Utilizza il separatore ";" per i campi CSV e la codifica UTF-8
 * per garantire la corretta gestione dei caratteri speciali.</p>
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public final class CsvService {

    private static final Logger logger = AppLogger.getLogger(CsvService.class);

    /**
     * Separatore utilizzato nei file CSV.
     */
    private static final String SEPARATOR = ";";

    /**
     * Directory contenente i file di dati.
     */
    private static final String DATA_DIR = "data/";

    /**
     * Formato data utilizzato nei file CSV.
     */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Nome del file per gli account.
     */
    private static final String ACCOUNTS_FILE = "accounts.csv";

    /**
     * Nome del file per le transazioni.
     */
    private static final String TRANSACTIONS_FILE = "transactions.csv";

    /**
     * Nome del file per le categorie.
     */
    private static final String CATEGORIES_FILE = "categories.csv";

    /**
     * Nome del file per i tassi di cambio.
     */
    private static final String RATES_FILE = "rates.csv";

    /**
     * Costruttore privato per impedire l'istanziazione.
     */
    private CsvService() {
        throw new UnsupportedOperationException("Classe utility non istanziabile");
    }

    /**
     * Assicura che la directory data esista, creandola se necessario.
     *
     * @throws StorageException se non è possibile creare la directory
     */
    private static void ensureDataDirectory() throws StorageException {
        try {
            Path dataPath = Paths.get(DATA_DIR);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                logger.info("Directory dati creata: " + DATA_DIR);
            }
        } catch (IOException e) {
            logger.severe("Impossibile creare la directory dati: " + e.getMessage());
            throw new StorageException("Impossibile creare la directory dati", e);
        }
    }

    // ==================== ACCOUNTS ====================

    /**
     * Salva la lista degli account su file CSV.
     *
     * @param accounts la lista di account da salvare
     * @throws StorageException se si verifica un errore durante la scrittura
     */
    public static void saveAccounts(List<Account> accounts) throws StorageException {
        ensureDataDirectory();
        Path filePath = Paths.get(DATA_DIR + ACCOUNTS_FILE);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            // Scrivi header
            writer.write("ID" + SEPARATOR + "Nome" + SEPARATOR + "CurrencyId" + SEPARATOR + "InitialBalance");
            writer.newLine();

            // Scrivi dati
            for (Account account : accounts) {
                writer.write(String.valueOf(account.getId()));
                writer.write(SEPARATOR);
                writer.write(escapeField(account.getName()));
                writer.write(SEPARATOR);
                writer.write(String.valueOf(account.getCurrencyId()));
                writer.write(SEPARATOR);
                writer.write(account.getInitialBalance().toPlainString());
                writer.newLine();
            }

            logger.info("Salvati " + accounts.size() + " account su " + ACCOUNTS_FILE);
        } catch (IOException e) {
            logger.severe("Errore durante il salvataggio degli account: " + e.getMessage());
            throw new StorageException("Errore durante il salvataggio degli account", e);
        }
    }

    /**
     * Carica la lista degli account da file CSV.
     *
     * @return la lista degli account caricati
     * @throws StorageException se si verifica un errore durante la lettura
     */
    public static List<Account> loadAccounts() throws StorageException {
        List<Account> accounts = new ArrayList<>();
        Path filePath = Paths.get(DATA_DIR + ACCOUNTS_FILE);

        if (!Files.exists(filePath)) {
            logger.info("File " + ACCOUNTS_FILE + " non trovato, restituisco lista vuota");
            return accounts;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(SEPARATOR, -1);
                if (fields.length >= 4) {
                    int id = Integer.parseInt(fields[0].trim());
                    String name = unescapeField(fields[1]);
                    int currencyId = Integer.parseInt(fields[2].trim());
                    BigDecimal initialBalance = new BigDecimal(fields[3].trim());

                    Account account = new Account(id, name, currencyId, initialBalance);
                    accounts.add(account);
                }
            }

            logger.info("Caricati " + accounts.size() + " account da " + ACCOUNTS_FILE);
        } catch (IOException e) {
            logger.severe("Errore durante il caricamento degli account: " + e.getMessage());
            throw new StorageException("Errore durante il caricamento degli account", e);
        } catch (NumberFormatException e) {
            logger.severe("Formato numerico non valido nel file account: " + e.getMessage());
            throw new StorageException("Formato dati non valido nel file account", e);
        }

        return accounts;
    }

    // ==================== TRANSACTIONS ====================

    /**
     * Salva la lista delle transazioni su file CSV.
     *
     * @param transactions la lista di transazioni da salvare
     * @throws StorageException se si verifica un errore durante la scrittura
     */
    public static void saveTransactions(List<Transaction> transactions) throws StorageException {
        ensureDataDirectory();
        Path filePath = Paths.get(DATA_DIR + TRANSACTIONS_FILE);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            // Scrivi header (esteso con campi valuta)
            writer.write("ID" + SEPARATOR + "AccountID" + SEPARATOR + "Type" + SEPARATOR +
                    "Date" + SEPARATOR + "Amount" + SEPARATOR + "Description" + SEPARATOR +
                    "CategoryID" + SEPARATOR + "OriginalAmount" + SEPARATOR +
                    "OriginalCurrencyId" + SEPARATOR + "ExchangeRate");
            writer.newLine();

            // Scrivi dati
            for (Transaction transaction : transactions) {
                writer.write(String.valueOf(transaction.getId()));
                writer.write(SEPARATOR);
                writer.write(String.valueOf(transaction.getAccountId()));
                writer.write(SEPARATOR);
                writer.write(transaction.getType().name());
                writer.write(SEPARATOR);
                writer.write(transaction.getDate().format(DATE_FORMAT));
                writer.write(SEPARATOR);
                writer.write(transaction.getAmount().toPlainString());
                writer.write(SEPARATOR);
                writer.write(escapeField(transaction.getDescription() != null ? transaction.getDescription() : ""));
                writer.write(SEPARATOR);
                writer.write(String.valueOf(transaction.getCategoryId()));
                writer.write(SEPARATOR);
                // Nuovi campi valuta (vuoti se null)
                writer.write(transaction.getOriginalAmount() != null ?
                        transaction.getOriginalAmount().toPlainString() : "");
                writer.write(SEPARATOR);
                writer.write(transaction.getOriginalCurrencyId() != null ?
                        String.valueOf(transaction.getOriginalCurrencyId()) : "");
                writer.write(SEPARATOR);
                writer.write(transaction.getExchangeRate() != null ?
                        transaction.getExchangeRate().toPlainString() : "");
                writer.newLine();
            }

            logger.info("Salvate " + transactions.size() + " transazioni su " + TRANSACTIONS_FILE);
        } catch (IOException e) {
            logger.severe("Errore durante il salvataggio delle transazioni: " + e.getMessage());
            throw new StorageException("Errore durante il salvataggio delle transazioni", e);
        }
    }

    /**
     * Carica la lista delle transazioni da file CSV.
     *
     * @return la lista delle transazioni caricate
     * @throws StorageException se si verifica un errore durante la lettura
     */
    public static List<Transaction> loadTransactions() throws StorageException {
        List<Transaction> transactions = new ArrayList<>();
        Path filePath = Paths.get(DATA_DIR + TRANSACTIONS_FILE);

        if (!Files.exists(filePath)) {
            logger.info("File " + TRANSACTIONS_FILE + " non trovato, restituisco lista vuota");
            return transactions;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(SEPARATOR, -1);
                if (fields.length >= 10) {
                    int id = Integer.parseInt(fields[0].trim());
                    int accountId = Integer.parseInt(fields[1].trim());
                    TransactionType type = TransactionType.valueOf(fields[2].trim());
                    LocalDate date = LocalDate.parse(fields[3].trim(), DATE_FORMAT);
                    BigDecimal amount = new BigDecimal(fields[4].trim());
                    String description = unescapeField(fields[5]);
                    int categoryId = Integer.parseInt(fields[6].trim());

                    Transaction.Builder builder = new Transaction.Builder()
                            .withId(id)
                            .withAccountId(accountId)
                            .withType(type)
                            .withDate(date)
                            .withAmount(amount)
                            .withDescription(description)
                            .withCategoryId(categoryId);

                    // Campi valuta (vuoti se nessuna conversione)
                    if (!fields[7].trim().isEmpty()) {
                        builder.withOriginalAmount(new BigDecimal(fields[7].trim()));
                    }
                    if (!fields[8].trim().isEmpty()) {
                        builder.withOriginalCurrencyId(Integer.parseInt(fields[8].trim()));
                    }
                    if (!fields[9].trim().isEmpty()) {
                        builder.withExchangeRate(new BigDecimal(fields[9].trim()));
                    }

                    transactions.add(builder.build());
                }
            }

            logger.info("Caricate " + transactions.size() + " transazioni da " + TRANSACTIONS_FILE);
        } catch (IOException e) {
            logger.severe("Errore durante il caricamento delle transazioni: " + e.getMessage());
            throw new StorageException("Errore durante il caricamento delle transazioni", e);
        } catch (NumberFormatException | DateTimeParseException e) {
            logger.severe("Formato dati non valido nel file transazioni: " + e.getMessage());
            throw new StorageException("Formato dati non valido nel file transazioni", e);
        } catch (InvalidInputException e) {
            logger.severe("Dati transazione non validi: " + e.getMessage());
            throw new StorageException("Dati transazione non validi nel file", e);
        }

        return transactions;
    }

    // ==================== CATEGORIES ====================

    /**
     * Salva la lista delle categorie su file CSV.
     *
     * @param categories la lista di categorie da salvare
     * @throws StorageException se si verifica un errore durante la scrittura
     */
    public static void saveCategories(List<CategoryComponent> categories) throws StorageException {
        ensureDataDirectory();
        Path filePath = Paths.get(DATA_DIR + CATEGORIES_FILE);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            // Scrivi header
            writer.write("ID" + SEPARATOR + "Nome" + SEPARATOR + "Tipo" + SEPARATOR +
                    "Description" + SEPARATOR + "ParentID");
            writer.newLine();

            // Scrivi dati
            for (CategoryComponent category : categories) {
                writer.write(String.valueOf(category.getId()));
                writer.write(SEPARATOR);
                writer.write(escapeField(category.getName()));
                writer.write(SEPARATOR);
                writer.write(category instanceof MacroCategory ? "MACRO" : "STANDARD");
                writer.write(SEPARATOR);
                writer.write(escapeField(category.getDescription() != null ? category.getDescription() : ""));
                writer.write(SEPARATOR);
                writer.write(category.getParentId() != null ? String.valueOf(category.getParentId()) : "");
                writer.newLine();
            }

            logger.info("Salvate " + categories.size() + " categorie su " + CATEGORIES_FILE);
        } catch (IOException e) {
            logger.severe("Errore durante il salvataggio delle categorie: " + e.getMessage());
            throw new StorageException("Errore durante il salvataggio delle categorie", e);
        }
    }

    /**
     * Carica la lista delle categorie da file CSV.
     * Ricostruisce la struttura gerarchica parent-child.
     *
     * @return la lista delle categorie caricate
     * @throws StorageException se si verifica un errore durante la lettura
     */
    public static List<CategoryComponent> loadCategories() throws StorageException {
        List<CategoryComponent> categories = new ArrayList<>();
        Map<Integer, CategoryComponent> categoryMap = new HashMap<>();
        Path filePath = Paths.get(DATA_DIR + CATEGORIES_FILE);

        if (!Files.exists(filePath)) {
            logger.info("File " + CATEGORIES_FILE + " non trovato, restituisco lista vuota");
            return categories;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isHeader = true;

            // Prima passata: crea tutti gli oggetti categoria
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(SEPARATOR, -1);
                if (fields.length >= 5) {
                    int id = Integer.parseInt(fields[0].trim());
                    String name = unescapeField(fields[1]);
                    String type = fields[2].trim();
                    String description = unescapeField(fields[3]);
                    Integer parentId = fields[4].trim().isEmpty() ? null : Integer.parseInt(fields[4].trim());

                    CategoryComponent category;
                    if ("MACRO".equals(type)) {
                        category = new MacroCategory(id, name, description, parentId);
                    } else {
                        category = new StandardCategory(id, name, description, parentId);
                    }

                    categories.add(category);
                    categoryMap.put(id, category);
                }
            }

            // Seconda passata: ricostruisci le relazioni parent-child
            for (CategoryComponent category : categories) {
                Integer parentId = category.getParentId();
                if (parentId != null) {
                    CategoryComponent parent = categoryMap.get(parentId);
                    if (parent instanceof MacroCategory) {
                        ((MacroCategory) parent).addChild(category);
                    }
                }
            }

            logger.info("Caricate " + categories.size() + " categorie da " + CATEGORIES_FILE);
        } catch (IOException e) {
            logger.severe("Errore durante il caricamento delle categorie: " + e.getMessage());
            throw new StorageException("Errore durante il caricamento delle categorie", e);
        } catch (NumberFormatException e) {
            logger.severe("Formato numerico non valido nel file categorie: " + e.getMessage());
            throw new StorageException("Formato dati non valido nel file categorie", e);
        }

        return categories;
    }

    // ==================== CURRENCIES ====================

    /**
     * Salva la lista delle valute e i loro tassi di cambio su file CSV.
     * Ogni riga rappresenta un tasso di cambio per una data specifica.
     *
     * @param currencies la lista di valute da salvare
     * @throws StorageException se si verifica un errore durante la scrittura
     */
    public static void saveCurrencies(List<Currency> currencies) throws StorageException {
        ensureDataDirectory();
        Path filePath = Paths.get(DATA_DIR + RATES_FILE);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            // Scrivi header
            writer.write("CurrencyID" + SEPARATOR + "Code" + SEPARATOR + "Name" + SEPARATOR +
                    "Symbol" + SEPARATOR + "Date" + SEPARATOR + "Rate");
            writer.newLine();

            // Scrivi dati - una riga per ogni rate nella history
            for (Currency currency : currencies) {
                Map<LocalDate, BigDecimal> rateHistory = currency.getRateHistory();

                if (rateHistory.isEmpty()) {
                    // Scrivi almeno una riga con rate di default
                    writer.write(String.valueOf(currency.getId()));
                    writer.write(SEPARATOR);
                    writer.write(escapeField(currency.getCode()));
                    writer.write(SEPARATOR);
                    writer.write(escapeField(currency.getName()));
                    writer.write(SEPARATOR);
                    writer.write(escapeField(currency.getSymbol()));
                    writer.write(SEPARATOR);
                    writer.write(LocalDate.now().format(DATE_FORMAT));
                    writer.write(SEPARATOR);
                    writer.write(BigDecimal.ONE.toPlainString());
                    writer.newLine();
                } else {
                    for (Map.Entry<LocalDate, BigDecimal> entry : rateHistory.entrySet()) {
                        writer.write(String.valueOf(currency.getId()));
                        writer.write(SEPARATOR);
                        writer.write(escapeField(currency.getCode()));
                        writer.write(SEPARATOR);
                        writer.write(escapeField(currency.getName()));
                        writer.write(SEPARATOR);
                        writer.write(escapeField(currency.getSymbol()));
                        writer.write(SEPARATOR);
                        writer.write(entry.getKey().format(DATE_FORMAT));
                        writer.write(SEPARATOR);
                        writer.write(entry.getValue().toPlainString());
                        writer.newLine();
                    }
                }
            }

            logger.info("Salvate " + currencies.size() + " valute su " + RATES_FILE);
        } catch (IOException e) {
            logger.severe("Errore durante il salvataggio delle valute: " + e.getMessage());
            throw new StorageException("Errore durante il salvataggio delle valute", e);
        }
    }

    /**
     * Carica la lista delle valute da file CSV.
     * Ricostruisce lo storico dei tassi di cambio per ogni valuta.
     *
     * @return la lista delle valute caricate
     * @throws StorageException se si verifica un errore durante la lettura
     */
    public static List<Currency> loadCurrencies() throws StorageException {
        Map<Integer, Currency> currencyMap = new HashMap<>();
        Path filePath = Paths.get(DATA_DIR + RATES_FILE);

        if (!Files.exists(filePath)) {
            logger.info("File " + RATES_FILE + " non trovato, restituisco lista vuota");
            return new ArrayList<>();
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(SEPARATOR, -1);
                if (fields.length >= 6) {
                    int id = Integer.parseInt(fields[0].trim());
                    String code = unescapeField(fields[1]);
                    String name = unescapeField(fields[2]);
                    String symbol = unescapeField(fields[3]);
                    LocalDate date = LocalDate.parse(fields[4].trim(), DATE_FORMAT);
                    BigDecimal rate = new BigDecimal(fields[5].trim());

                    // Crea la valuta se non esiste ancora
                    Currency currency = currencyMap.get(id);
                    if (currency == null) {
                        currency = new Currency(id, code, name, symbol);
                        currencyMap.put(id, currency);
                    }

                    // Aggiungi il tasso
                    currency.addRate(date, rate);
                }
            }

            logger.info("Caricate " + currencyMap.size() + " valute da " + RATES_FILE);
        } catch (IOException e) {
            logger.severe("Errore durante il caricamento delle valute: " + e.getMessage());
            throw new StorageException("Errore durante il caricamento delle valute", e);
        } catch (NumberFormatException | DateTimeParseException e) {
            logger.severe("Formato dati non valido nel file valute: " + e.getMessage());
            throw new StorageException("Formato dati non valido nel file valute", e);
        }

        return new ArrayList<>(currencyMap.values());
    }

    // ==================== SAVE/LOAD ALL ====================

    /**
     * Salva tutti i dati dai repository su file CSV.
     *
     * @throws StorageException se si verifica un errore durante il salvataggio
     */
    public static void saveAll() throws StorageException {
        logger.info("Avvio salvataggio di tutti i dati...");

        DataStorage storage = DataStorage.getInstance();

        saveAccounts(storage.getAccountRepository().findAll());
        saveTransactions(storage.getTransactionRepository().findAll());
        saveCategories(storage.getCategoryRepository().findAll());
        saveCurrencies(storage.getCurrencyRepository().findAll());

        logger.info("Salvataggio di tutti i dati completato");
    }

    /**
     * Carica tutti i dati dai file CSV e popola i repository.
     *
     * @throws StorageException se si verifica un errore durante il caricamento
     */
    public static void loadAll() throws StorageException {
        logger.info("Avvio caricamento di tutti i dati...");

        DataStorage storage = DataStorage.getInstance();

        // Pulisci i repository prima del caricamento
        storage.getAccountRepository().clear();
        storage.getTransactionRepository().clear();
        storage.getCategoryRepository().clear();
        storage.getCurrencyRepository().clear();

        // Carica le valute per prime (necessarie per gli account)
        List<Currency> currencies = loadCurrencies();
        for (Currency currency : currencies) {
            storage.getCurrencyRepository().save(currency);
        }

        // Carica le categorie (necessarie per le transazioni)
        List<CategoryComponent> categories = loadCategories();
        for (CategoryComponent category : categories) {
            storage.getCategoryRepository().save(category);
        }

        // Carica gli account
        List<Account> accounts = loadAccounts();
        for (Account account : accounts) {
            storage.getAccountRepository().save(account);
        }

        // Carica le transazioni e associale agli account
        List<Transaction> transactions = loadTransactions();
        for (Transaction transaction : transactions) {
            storage.getTransactionRepository().save(transaction);

            // Associa la transazione all'account corrispondente
            storage.getAccountRepository().findById(transaction.getAccountId())
                    .ifPresent(account -> account.addTransaction(transaction));
        }

        logger.info("Caricamento di tutti i dati completato");
    }

    // ==================== UTILITY ====================

    /**
     * Effettua l'escape di un campo per la scrittura CSV.
     * Sostituisce i caratteri speciali che potrebbero interferire con il parsing.
     *
     * @param field il campo da processare
     * @return il campo con i caratteri speciali escaped
     */
    private static String escapeField(String field) {
        if (field == null) {
            return "";
        }
        // Sostituisci il separatore con una sequenza di escape
        return field.replace(SEPARATOR, "\\;").replace("\n", "\\n").replace("\r", "\\r");
    }

    /**
     * Rimuove l'escape da un campo letto da CSV.
     *
     * @param field il campo da processare
     * @return il campo con i caratteri speciali ripristinati
     */
    private static String unescapeField(String field) {
        if (field == null) {
            return "";
        }
        return field.replace("\\;", SEPARATOR).replace("\\n", "\n").replace("\\r", "\r");
    }
}
