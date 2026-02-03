package edu.epicode.mysmartwallet;

import edu.epicode.mysmartwallet.exception.InvalidInputException;
import edu.epicode.mysmartwallet.exception.WalletException;
import edu.epicode.mysmartwallet.model.Account;
import edu.epicode.mysmartwallet.model.Currency;
import edu.epicode.mysmartwallet.model.Transaction;
import edu.epicode.mysmartwallet.model.TransactionType;
import edu.epicode.mysmartwallet.model.category.CategoryComponent;
import edu.epicode.mysmartwallet.model.category.MacroCategory;
import edu.epicode.mysmartwallet.persistence.CsvService;
import edu.epicode.mysmartwallet.persistence.generator.DemoDataGenerator;
import edu.epicode.mysmartwallet.persistence.generator.EmptyDataGenerator;
import edu.epicode.mysmartwallet.repository.DataStorage;
import edu.epicode.mysmartwallet.service.CategoryService;
import edu.epicode.mysmartwallet.service.CurrencyManager;
import edu.epicode.mysmartwallet.service.ReportService;
import edu.epicode.mysmartwallet.service.WalletService;
import edu.epicode.mysmartwallet.service.observer.ConsoleBalanceObserver;
import edu.epicode.mysmartwallet.service.strategy.HistoricalExchangeStrategy;
import edu.epicode.mysmartwallet.exception.RateNotFoundException;
import edu.epicode.mysmartwallet.util.AppLogger;
import edu.epicode.mysmartwallet.util.InputValidator;
import edu.epicode.mysmartwallet.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe principale dell'applicazione MySmartWallet.
 * Fornisce un'interfaccia CLI interattiva per la gestione della contabilità personale.
 *
 * <p>Funzionalità disponibili:
 * <ul>
 *   <li>Riepilogo conti e saldi</li>
 *   <li>Gestione transazioni (entrate, uscite, trasferimenti)</li>
 *   <li>Report finanziari</li>
 *   <li>Gestione categorie gerarchiche</li>
 *   <li>Gestione valute e tassi di cambio</li>
 *   <li>Persistenza dati su file CSV</li>
 * </ul>
 * </p>
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class Main {

    private static final Logger logger = AppLogger.getLogger(Main.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static Scanner scanner;
    private static WalletService walletService;
    private static ReportService reportService;
    private static CurrencyManager currencyManager;
    private static CategoryService categoryService;
    private static boolean running = true;

    /**
     * Entry point dell'applicazione.
     *
     * @param args argomenti da linea di comando (non utilizzati)
     */
    public static void main(String[] args) {
        AppLogger.setup();

        try {
            scanner = new Scanner(System.in);
            initialize();
            runMainLoop();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore critico nell'applicazione", e);
            System.out.println("\nSi e' verificato un errore imprevisto. Controlla i log per dettagli.");
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    /**
     * Inizializza l'applicazione caricando i dati e configurando i servizi.
     */
    private static void initialize() {
        System.out.println("\n========================================");
        System.out.println("       MySmartWallet - Avvio            ");
        System.out.println("========================================\n");

        try {
            CsvService.loadAll();
            DataStorage storage = DataStorage.getInstance();

            if (storage.getAccountRepository().count() == 0 &&
                storage.getCategoryRepository().count() == 0) {
                System.out.println("Nessun dato trovato. Vuoi creare dati demo? (s/n)");
                if (readYesNo("")) {
                    new DemoDataGenerator().generate();
                    System.out.println("Dati demo creati con successo!");
                } else {
                    new EmptyDataGenerator().generate();
                    System.out.println("Database vuoto inizializzato.");
                }
            } else {
                System.out.println("Dati caricati con successo!");
            }
        } catch (WalletException e) {
            logger.warning("Errore caricamento dati: " + e.getMessage());
            System.out.println("Impossibile caricare i dati esistenti.");
            System.out.println("Vuoi creare dati demo? (s/n)");

            try {
                if (readYesNo("")) {
                    DataStorage.resetInstance();
                    CurrencyManager.resetInstance();
                    new DemoDataGenerator().generate();
                    System.out.println("Dati demo creati con successo!");
                } else {
                    DataStorage.resetInstance();
                    CurrencyManager.resetInstance();
                    new EmptyDataGenerator().generate();
                    System.out.println("Database vuoto inizializzato.");
                }
            } catch (WalletException ex) {
                logger.log(Level.SEVERE, "Errore durante la generazione dei dati", ex);
                System.out.println("Errore durante la generazione dei dati iniziali.");
            }
        }

        walletService = new WalletService(new HistoricalExchangeStrategy());
        reportService = new ReportService();
        currencyManager = CurrencyManager.getInstance();
        categoryService = new CategoryService();

        walletService.addGlobalObserver(new ConsoleBalanceObserver());

        logger.info("Applicazione inizializzata con successo");
    }

    /**
     * Esegue il loop principale del menu interattivo.
     */
    private static void runMainLoop() {
        while (running) {
            printMainMenu();
            try {
                int choice = readInt("Scelta");
                switch (choice) {
                    case 1:
                        handleTransactionMenu();
                        break;
                    case 2:
                        handleAccountMenu();
                        break;
                    case 3:
                        handleCategories();
                        break;
                    case 4:
                        handleCurrencies();
                        break;
                    case 5:
                        handleSave();
                        break;
                    case 6:
                        handleExit();
                        break;
                    default:
                        System.out.println("Scelta non valida. Riprova.");
                }
            } catch (WalletException e) {
                System.out.println("Errore: " + e.getMessage());
                logger.warning("Errore operazione: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Si e' verificato un errore. Riprova.");
                logger.log(Level.SEVERE, "Errore imprevisto nel menu", e);
            }
        }

        System.out.println("\nArrivederci!");
    }

    /**
     * Stampa il menu principale.
     */
    private static void printMainMenu() {
        System.out.println("\n+=============================+");
        System.out.println("|       MENU PRINCIPALE       |");
        System.out.println("+=============================+");
        System.out.println("| 1. Transazioni              |");
        System.out.println("| 2. Conti                    |");
        System.out.println("| 3. Categorie                |");
        System.out.println("| 4. Valute                   |");
        System.out.println("| 5. Salva                    |");
        System.out.println("| 6. Esci                     |");
        System.out.println("+=============================+");
    }

    // ==================== DASHBOARD ====================

    /**
     * Mostra la dashboard con il riepilogo dei conti.
     */
    private static void handleDashboard() {
        List<Account> accounts = walletService.getAllAccounts();

        if (accounts.isEmpty()) {
            System.out.println("Nessun conto presente. Creane uno dal menu transazioni.");
            return;
        }

        System.out.println("\nI tuoi conti:");
        System.out.println("--------------------------------------");

        for (Account account : accounts) {
            Currency currency = getCurrencyForAccount(account);
            String symbol = currency != null ? currency.getSymbol() : "";
            System.out.printf("  [%d] %-20s %15s %s%n",
                    account.getId(),
                    account.getName(),
                    MoneyUtil.format(account.getBalance(), ""),
                    symbol);
        }

        System.out.println("--------------------------------------");

        try {
            BigDecimal totalEur = walletService.getTotalBalance("EUR", LocalDate.now());
            System.out.printf("%nPatrimonio totale in EUR: %s%n", MoneyUtil.format(totalEur, "EUR"));
        } catch (WalletException e) {
            logger.warning("Errore calcolo patrimonio totale: " + e.getMessage());
        }
    }

    // ==================== GESTIONE CONTI ====================

    /**
     * Gestisce il sottomenu dei conti.
     * Il menu rimane attivo fino a quando l'utente non sceglie "Indietro".
     */
    private static void handleAccountMenu() throws WalletException {
        while (true) {
            System.out.println("\n+--- GESTIONE CONTI ---+");
            System.out.println("| 1. Visualizza saldi  |");
            System.out.println("| 2. Crea conto        |");
            System.out.println("| 3. Elimina conto     |");
            System.out.println("| 4. Indietro          |");
            System.out.println("+----------------------+");

            int choice = readInt("Scelta");

            switch (choice) {
                case 1:
                    handleDashboard();
                    break;
                case 2:
                    handleCreateAccount();
                    break;
                case 3:
                    handleDeleteAccount();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Scelta non valida.");
            }
        }
    }

    /**
     * Gestisce la creazione di un nuovo conto.
     */
    private static void handleCreateAccount() throws WalletException {
        System.out.println("\n+--- NUOVO CONTO ---+");

        String name = readString("Nome del conto");
        if (name.isEmpty()) {
            System.out.println("Il nome non puo' essere vuoto.");
            return;
        }

        // Selezione valuta
        System.out.println("\nSeleziona la valuta:");
        List<Currency> currencies = currencyManager.getAllCurrencies();
        for (int i = 0; i < currencies.size(); i++) {
            Currency c = currencies.get(i);
            System.out.printf("  [%d] %s (%s) %s%n", i + 1, c.getCode(), c.getName(), c.getSymbol());
        }

        int currencyChoice = readInt("Valuta (1-" + currencies.size() + ")");
        if (currencyChoice < 1 || currencyChoice > currencies.size()) {
            System.out.println("Scelta non valida.");
            return;
        }
        Currency selectedCurrency = currencies.get(currencyChoice - 1);

        BigDecimal initialBalance = readAmount("Saldo iniziale (" + selectedCurrency.getSymbol() + ")");

        Account newAccount = walletService.createAccount(name, selectedCurrency.getCode(), initialBalance);
        System.out.printf("%nConto '%s' creato con successo! (ID: %d)%n", newAccount.getName(), newAccount.getId());
    }

    /**
     * Gestisce l'eliminazione di un conto.
     * Verifica che il conto non abbia transazioni associate prima di eliminarlo.
     */
    private static void handleDeleteAccount() throws WalletException {
        System.out.println("\n+--- ELIMINA CONTO ---+");

        List<Account> accounts = walletService.getAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("Nessun conto disponibile.");
            return;
        }

        // Mostra i conti disponibili
        System.out.println("\nConti disponibili:");
        System.out.println("--------------------------------------");
        for (Account account : accounts) {
            Currency currency = getCurrencyForAccount(account);
            String symbol = currency != null ? currency.getSymbol() : "";
            int transactionCount = account.getTransactions().size();
            System.out.printf("  [%d] %-15s - Saldo: %s %s - Transazioni: %d%n",
                    account.getId(), account.getName(),
                    MoneyUtil.format(account.getBalance(), ""), symbol, transactionCount);
        }
        System.out.println("--------------------------------------");

        int accountId = readInt("ID conto da eliminare (0 per annullare)");
        if (accountId == 0) {
            System.out.println("Operazione annullata.");
            return;
        }

        // Recupera il conto
        Account account;
        try {
            account = walletService.getAccount(accountId);
        } catch (WalletException e) {
            System.out.println("Conto non trovato.");
            return;
        }

        // Verifica se puo' essere eliminato
        WalletService.DeleteAccountResult check = walletService.canDeleteAccount(accountId);
        if (!check.canDelete()) {
            System.out.printf("%nImpossibile eliminare: %s%n", check.getReason());
            System.out.println("Elimina prima tutte le transazioni del conto.");
            return;
        }

        // Mostra dettagli e chiedi conferma
        Currency currency = getCurrencyForAccount(account);
        String symbol = currency != null ? currency.getSymbol() : "";

        System.out.println("\n--- Dettagli conto ---");
        System.out.printf("ID:             %d%n", account.getId());
        System.out.printf("Nome:           %s%n", account.getName());
        System.out.printf("Valuta:         %s%n", currency != null ? currency.getCode() : "N/D");
        System.out.printf("Saldo iniziale: %s %s%n", MoneyUtil.format(account.getInitialBalance(), ""), symbol);

        if (!readYesNo("\nConfermi l'eliminazione? (s/n)")) {
            System.out.println("Operazione annullata.");
            return;
        }

        // Elimina il conto tramite servizio
        walletService.deleteAccount(accountId);
        System.out.printf("%nConto '%s' eliminato con successo!%n", account.getName());
    }

    // ==================== GESTIONE TRANSAZIONI ====================

    /**
     * Gestisce il sottomenu delle transazioni.
     * Il menu rimane attivo fino a quando l'utente non sceglie "Indietro".
     */
    private static void handleTransactionMenu() throws WalletException {
        while (true) {
            System.out.println("\n+--- GESTIONE TRANSAZIONI ---+");
            System.out.println("| 1. Inserisci transazione   |");
            System.out.println("| 2. Cancella transazione    |");
            System.out.println("| 3. Report                  |");
            System.out.println("| 4. Indietro                |");
            System.out.println("+----------------------------+");

            int choice = readInt("Scelta");

            switch (choice) {
                case 1:
                    handleNewTransaction();
                    break;
                case 2:
                    handleDeleteTransaction();
                    break;
                case 3:
                    handleReports();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Scelta non valida.");
            }
        }
    }

    /**
     * Gestisce la cancellazione di una transazione.
     * Chiede l'ID, mostra i dettagli per conferma, poi elimina la transazione
     * aggiornando automaticamente il saldo del conto.
     */
    private static void handleDeleteTransaction() throws WalletException {
        System.out.println("\n+--- CANCELLA TRANSAZIONE ---+");

        List<Account> accounts = walletService.getAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("Nessun conto disponibile.");
            return;
        }

        // Mostra tutte le transazioni recenti per aiutare l'utente
        System.out.println("\nTransazioni recenti:");
        System.out.println("--------------------------------------");

        boolean hasTransactions = false;
        for (Account account : accounts) {
            List<Transaction> transactions = account.getTransactions();
            if (!transactions.isEmpty()) {
                hasTransactions = true;
                Currency currency = getCurrencyForAccount(account);
                String symbol = currency != null ? currency.getSymbol() : "";
                System.out.println("\n" + account.getName() + " (" + symbol + "):");

                // Mostra le ultime 5 transazioni per conto
                int start = Math.max(0, transactions.size() - 5);
                for (int i = start; i < transactions.size(); i++) {
                    Transaction t = transactions.get(i);
                    System.out.printf("  [%d] %s | %-10s | %10s | %s%n",
                            t.getId(),
                            t.getDate().format(DATE_FORMAT),
                            t.getType().getDescription(),
                            MoneyUtil.format(t.getAmount(), ""),
                            t.getDescription());
                }
            }
        }

        if (!hasTransactions) {
            System.out.println("Nessuna transazione presente.");
            return;
        }

        System.out.println("--------------------------------------");

        int transactionId = readInt("ID transazione da eliminare (0 per annullare)");
        if (transactionId == 0) {
            System.out.println("Operazione annullata.");
            return;
        }

        // Recupera la transazione
        Transaction transaction;
        try {
            transaction = walletService.getTransaction(transactionId);
        } catch (WalletException e) {
            System.out.println("Transazione non trovata.");
            return;
        }

        // Recupera il conto associato
        Account account = walletService.getAccount(transaction.getAccountId());
        Currency currency = getCurrencyForAccount(account);
        String symbol = currency != null ? currency.getSymbol() : "";

        // Mostra i dettagli della transazione
        System.out.println("\n--- Dettagli transazione ---");
        System.out.printf("ID:          %d%n", transaction.getId());
        System.out.printf("Conto:       %s%n", account.getName());
        System.out.printf("Tipo:        %s%n", transaction.getType().getDescription());
        System.out.printf("Data:        %s%n", transaction.getDate().format(DATE_FORMAT));
        System.out.printf("Importo:     %s %s%n", MoneyUtil.format(transaction.getAmount(), ""), symbol);
        System.out.printf("Descrizione: %s%n", transaction.getDescription());

        // Calcola l'impatto sul saldo tramite servizio
        WalletService.TransactionImpact impact = walletService.calculateDeleteTransactionImpact(transactionId);

        System.out.println("\n--- Impatto sul saldo ---");
        System.out.printf("Saldo attuale:         %s %s%n", MoneyUtil.format(impact.getCurrentBalance(), ""), symbol);
        System.out.printf("Saldo dopo eliminazione: %s %s%n", MoneyUtil.format(impact.getExpectedBalance(), ""), symbol);

        // Chiedi conferma
        if (!readYesNo("\nConfermi l'eliminazione? (s/n)")) {
            System.out.println("Operazione annullata.");
            return;
        }

        // Esegui la cancellazione
        walletService.deleteTransaction(transactionId);
        System.out.println("\nTransazione eliminata con successo!");
        System.out.printf("Nuovo saldo del conto '%s': %s %s%n",
                account.getName(),
                MoneyUtil.format(walletService.getAccount(account.getId()).getBalance(), ""),
                symbol);
    }

    // ==================== NUOVA TRANSAZIONE ====================

    /**
     * Gestisce la creazione di una nuova transazione.
     */
    private static void handleNewTransaction() throws WalletException {
        System.out.println("\n+--- NUOVA TRANSAZIONE ---+");
        System.out.println("| 1. Entrata              |");
        System.out.println("| 2. Uscita               |");
        System.out.println("| 3. Trasferimento        |");
        System.out.println("| 4. Indietro             |");
        System.out.println("+-------------------------+");

        int choice = readInt("Scelta");

        if (choice == 4) {
            return;
        }

        if (choice == 3) {
            handleTransfer();
            return;
        }

        TransactionType type;
        if (choice == 1) {
            type = TransactionType.INCOME;
        } else if (choice == 2) {
            type = TransactionType.EXPENSE;
        } else {
            System.out.println("Scelta non valida.");
            return;
        }

        List<Account> accounts = walletService.getAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("Nessun conto disponibile. Creane uno prima.");
            return;
        }

        System.out.println("\nSeleziona il conto:");
        int accountIndex = selectFromList("Conto", accounts);
        if (accountIndex < 0) return;
        Account account = accounts.get(accountIndex);

        // Selezione valuta con default = valuta del conto
        Currency accountCurrency = getCurrencyForAccount(account);
        Currency selectedCurrency = selectTransactionCurrency(accountCurrency);

        // Importo nella valuta selezionata
        String currencyLabel = selectedCurrency != null ?
                " (" + selectedCurrency.getCode() + ")" : "";
        BigDecimal inputAmount = readAmount("Importo" + currencyLabel);

        String description = readString("Descrizione");

        System.out.println("\nSeleziona la categoria:");
        List<CategoryComponent> categories = categoryService.getAllCategories();
        printCategoryList(categories);
        int categoryId = readInt("ID Categoria");

        LocalDate date = readDate("Data (yyyy-MM-dd, invio per oggi)");

        // Gestione conversione valutaria
        BigDecimal finalAmount = inputAmount;
        BigDecimal originalAmount = null;
        Integer originalCurrencyId = null;
        BigDecimal exchangeRate = null;

        if (selectedCurrency != null && accountCurrency != null &&
                selectedCurrency.getId() != accountCurrency.getId()) {

            // Chiedi come gestire il tasso
            ExchangeRateChoice rateChoice = selectExchangeRateMethod();

            if (rateChoice.isManual()) {
                exchangeRate = rateChoice.getRate();
            } else {
                // Usa strategia storica
                try {
                    BigDecimal rateFrom = selectedCurrency.getRateForDate(date);
                    BigDecimal rateTo = accountCurrency.getRateForDate(date);
                    exchangeRate = MoneyUtil.divideRates(rateFrom, rateTo);
                } catch (RateNotFoundException e) {
                    System.out.println("Tasso storico non disponibile per la data selezionata.");
                    System.out.println("Inserisci un tasso manualmente:");
                    exchangeRate = readExchangeRate();
                }
            }

            // Converti
            finalAmount = MoneyUtil.multiplyByRate(inputAmount, exchangeRate);
            originalAmount = inputAmount;
            originalCurrencyId = selectedCurrency.getId();

            System.out.printf("%nConversione: %s %s -> %s %s (tasso: %s)%n",
                    MoneyUtil.format(inputAmount, selectedCurrency.getSymbol()),
                    selectedCurrency.getCode(),
                    MoneyUtil.format(finalAmount, accountCurrency.getSymbol()),
                    accountCurrency.getCode(),
                    exchangeRate.toPlainString());
        }

        walletService.addTransaction(account.getId(), type, finalAmount,
                description, categoryId, date, originalAmount, originalCurrencyId, exchangeRate);
        System.out.println("\nTransazione registrata con successo!");
    }

    /**
     * Permette all'utente di selezionare la valuta per la transazione.
     * La valuta del conto e' proposta come default (premendo Enter).
     *
     * @param accountCurrency la valuta del conto (default)
     * @return la valuta selezionata
     */
    private static Currency selectTransactionCurrency(Currency accountCurrency) {
        if (accountCurrency == null) {
            return null;
        }

        System.out.printf("%nValuta transazione (default: %s, premi Enter per confermare):%n",
                accountCurrency.getCode());

        List<Currency> currencies = currencyManager.getAllCurrencies();
        for (int i = 0; i < currencies.size(); i++) {
            Currency c = currencies.get(i);
            String defaultMark = c.getId() == accountCurrency.getId() ? " [DEFAULT]" : "";
            System.out.printf("  [%d] %s (%s) %s%s%n",
                    i + 1, c.getCode(), c.getName(), c.getSymbol(), defaultMark);
        }

        System.out.print("Valuta (1-" + currencies.size() + ", Enter per default): ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            return accountCurrency;
        }

        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < currencies.size()) {
                return currencies.get(index);
            }
        } catch (NumberFormatException e) {
            // Ignora, usa default
        }

        System.out.println("Scelta non valida, uso valuta del conto.");
        return accountCurrency;
    }

    /**
     * Chiede all'utente come determinare il tasso di cambio.
     *
     * @return la scelta con eventuale tasso manuale
     */
    private static ExchangeRateChoice selectExchangeRateMethod() {
        System.out.println("\nMetodo per il tasso di cambio:");
        System.out.println("  [1] Tasso automatico (storico alla data)");
        System.out.println("  [2] Tasso manuale (inserisci tu)");

        int choice = readInt("Scelta");

        if (choice == 2) {
            BigDecimal rate = readExchangeRate();
            return new ExchangeRateChoice(true, rate);
        }

        return new ExchangeRateChoice(false, null);
    }

    /**
     * Legge un tasso di cambio dall'input.
     *
     * @return il tasso come BigDecimal
     */
    private static BigDecimal readExchangeRate() {
        while (true) {
            System.out.print("Tasso di cambio: ");
            String input = scanner.nextLine().trim();
            try {
                BigDecimal rate = MoneyUtil.ofRate(input);
                if (MoneyUtil.isPositive(rate)) {
                    return rate;
                }
                System.out.println("Il tasso deve essere positivo.");
            } catch (NumberFormatException e) {
                System.out.println("Formato non valido. Usa formato numerico (es: 1.10).");
            }
        }
    }

    /**
     * Classe interna per rappresentare la scelta del tasso di cambio.
     */
    private static class ExchangeRateChoice {
        private final boolean manual;
        private final BigDecimal rate;

        ExchangeRateChoice(boolean manual, BigDecimal rate) {
            this.manual = manual;
            this.rate = rate;
        }

        boolean isManual() {
            return manual;
        }

        BigDecimal getRate() {
            return rate;
        }
    }

    /**
     * Stampa la lista delle categorie con navigazione dell'albero.
     */
    private static void printCategoryList(List<CategoryComponent> categories) {
        for (CategoryComponent category : categories) {
            if (category.getParentId() == null) {
                if (category instanceof MacroCategory) {
                    printCategoryTree((MacroCategory) category, "");
                } else {
                    System.out.printf("  [%d] %s%n", category.getId(), category.getName());
                }
            }
        }
    }

    /**
     * Stampa ricorsivamente l'albero delle categorie usando l'Iterator.
     */
    private static void printCategoryTree(MacroCategory macro, String indent) {
        Iterator<CategoryComponent> iterator = macro.iterator();
        while (iterator.hasNext()) {
            CategoryComponent cat = iterator.next();
            String prefix = cat.isLeaf() ? "-" : "+";
            String currentIndent = cat.getParentId() == null ? "" :
                    (cat.getParentId().equals(macro.getId()) ? "  " : "    ");
            System.out.printf("%s%s [%d] %s%n", indent + currentIndent, prefix,
                    cat.getId(), cat.getName());
        }
    }

    // ==================== TRASFERIMENTO ====================

    /**
     * Gestisce un trasferimento tra conti.
     * Il flusso permette di specificare importi diversi per conti con valute diverse.
     */
    private static void handleTransfer() throws WalletException {
        System.out.println("\n+--- TRASFERIMENTO ---+");

        List<Account> accounts = walletService.getAllAccounts();
        if (accounts.size() < 2) {
            System.out.println("Servono almeno 2 conti per un trasferimento.");
            return;
        }

        // Mostra conti disponibili
        System.out.println("\nConti disponibili:");
        for (Account account : accounts) {
            Currency currency = getCurrencyForAccount(account);
            String symbol = currency != null ? currency.getSymbol() : "";
            System.out.printf("  [%d] %-15s - Saldo: %s %s%n",
                    account.getId(), account.getName(),
                    MoneyUtil.format(account.getBalance(), ""), symbol);
        }

        // Selezione conto origine
        int fromId = readInt("ID conto origine");
        Account fromAccount = walletService.getAccount(fromId);
        Currency fromCurrency = getCurrencyForAccount(fromAccount);
        String fromSymbol = fromCurrency != null ? fromCurrency.getCode() : "";

        // Chiedi importo nella valuta del conto origine
        BigDecimal fromAmount = readAmount("Importo (" + fromSymbol + ")");

        // Verifica disponibilità
        if (MoneyUtil.isGreaterThan(fromAmount, fromAccount.getBalance())) {
            System.out.println("Fondi insufficienti. Disponibile: " +
                    MoneyUtil.format(fromAccount.getBalance(), fromSymbol));
            return;
        }

        // Selezione conto destinazione
        System.out.println("\nAltri conti:");
        for (Account account : accounts) {
            if (account.getId() != fromId) {
                Currency currency = getCurrencyForAccount(account);
                String symbol = currency != null ? currency.getSymbol() : "";
                System.out.printf("  [%d] %-15s - Saldo: %s %s%n",
                        account.getId(), account.getName(),
                        MoneyUtil.format(account.getBalance(), ""), symbol);
            }
        }

        int toId = readInt("ID conto destinazione");
        if (fromId == toId) {
            System.out.println("I conti di origine e destinazione devono essere diversi.");
            return;
        }

        Account toAccount = walletService.getAccount(toId);
        Currency toCurrency = getCurrencyForAccount(toAccount);
        String toSymbol = toCurrency != null ? toCurrency.getCode() : "";

        // Se valute diverse, chiedi l'importo nella valuta di destinazione
        BigDecimal toAmount = null;
        if (fromCurrency != null && toCurrency != null &&
                fromCurrency.getId() != toCurrency.getId()) {

            System.out.println("\nI conti hanno valute diverse (" +
                    fromSymbol + " -> " + toSymbol + ").");
            toAmount = readAmount("Importo da accreditare (" + toSymbol + ")");

            // Mostra il tasso di cambio calcolato
            BigDecimal rate = MoneyUtil.divideRates(toAmount, fromAmount);
            System.out.printf("Tasso di cambio calcolato: %s%n", rate.toPlainString());
        }

        String description = readString("Descrizione");
        int transferCategoryId = 3;

        walletService.transfer(fromId, toId, fromAmount, toAmount, description,
                transferCategoryId, LocalDate.now());
        System.out.println("\nTrasferimento completato con successo!");
    }

    // ==================== REPORT ====================

    /**
     * Gestisce il menu dei report.
     * Il menu rimane attivo fino a quando l'utente non sceglie "Indietro".
     */
    private static void handleReports() throws WalletException {
        while (true) {
            System.out.println("\n+--------- REPORT ---------+");
            System.out.println("| 1. Totale spese          |");
            System.out.println("| 2. Totale entrate        |");
            System.out.println("| 3. Spese per categoria   |");
            System.out.println("| 4. Transazioni periodo   |");
            System.out.println("| 5. Cerca per descrizione |");
            System.out.println("| 6. Indietro              |");
            System.out.println("+--------------------------+");

            int choice = readInt("Scelta");

            if (choice == 6) return;

            List<Account> accounts = walletService.getAllAccounts();
            if (accounts.isEmpty()) {
                System.out.println("Nessun conto disponibile.");
                continue;
            }

            System.out.println("\nSeleziona il conto (0 per tutti):");
            for (Account acc : accounts) {
                System.out.printf("  [%d] %s%n", acc.getId(), acc.getName());
            }
            int accountId = readInt("ID Conto");

            switch (choice) {
                case 1:
                    reportTotalExpenses(accountId, accounts);
                    break;
                case 2:
                    reportTotalIncome(accountId, accounts);
                    break;
                case 3:
                    reportExpensesByCategory(accountId, accounts);
                    break;
                case 4:
                    reportTransactionsByPeriod(accountId, accounts);
                    break;
                case 5:
                    reportSearchByDescription(accountId, accounts);
                    break;
                default:
                    System.out.println("Scelta non valida.");
            }
        }
    }

    private static void reportSearchByDescription(int accountId, List<Account> accounts) {
        String keyword = readString("Parola chiave da cercare");
        if (keyword.isEmpty()) {
            System.out.println("Inserisci una parola chiave valida.");
            return;
        }

        System.out.println("\nRisultati ricerca per '" + keyword + "':");
        System.out.println("--------------------------------------");

        boolean found = false;

        if (accountId == 0) {
            for (Account acc : accounts) {
                List<Transaction> transactions = reportService.searchByDescription(acc.getId(), keyword);
                if (!transactions.isEmpty()) {
                    found = true;
                    Currency currency = getCurrencyForAccount(acc);
                    String symbol = currency != null ? currency.getSymbol() : "";
                    System.out.println("\n" + acc.getName() + " (" + symbol + "):");
                    printTransactions(transactions);
                }
            }
        } else {
            List<Transaction> transactions = reportService.searchByDescription(accountId, keyword);
            if (!transactions.isEmpty()) {
                found = true;
                printTransactions(transactions);
            }
        }

        if (!found) {
            System.out.println("Nessuna transazione trovata.");
        }
    }

    private static void reportTotalExpenses(int accountId, List<Account> accounts) {
        LocalDate from = readDateOptional("Data inizio (yyyy-MM-dd, invio per nessun limite)");
        LocalDate to = readDateOptional("Data fine (yyyy-MM-dd, invio per nessun limite)");

        if (accountId == 0) {
            BigDecimal total = reportService.getTotalExpensesAllAccounts(from, to);
            System.out.printf("%nTotale spese (tutti i conti): %s EUR%n", MoneyUtil.format(total, ""));
        } else {
            BigDecimal total = reportService.getTotalExpenses(accountId, from, to);
            System.out.printf("%nTotale spese: %s EUR%n", MoneyUtil.format(total, ""));
        }
    }

    private static void reportTotalIncome(int accountId, List<Account> accounts) {
        LocalDate from = readDateOptional("Data inizio (yyyy-MM-dd, invio per nessun limite)");
        LocalDate to = readDateOptional("Data fine (yyyy-MM-dd, invio per nessun limite)");

        if (accountId == 0) {
            BigDecimal total = reportService.getTotalIncomeAllAccounts(from, to);
            System.out.printf("%nTotale entrate (tutti i conti): %s EUR%n", MoneyUtil.format(total, ""));
        } else {
            BigDecimal total = reportService.getTotalIncome(accountId, from, to);
            System.out.printf("%nTotale entrate: %s EUR%n", MoneyUtil.format(total, ""));
        }
    }

    private static void reportExpensesByCategory(int accountId, List<Account> accounts) {
        LocalDate from = readDateOptional("Data inizio (yyyy-MM-dd, invio per nessun limite)");
        LocalDate to = readDateOptional("Data fine (yyyy-MM-dd, invio per nessun limite)");

        System.out.println("\nSpese per categoria (EUR):");
        System.out.println("--------------------------------------");

        Map<Integer, CategoryComponent> categoryMap = new java.util.HashMap<>();
        categoryService.getAllCategories().forEach(c -> categoryMap.put(c.getId(), c));

        Map<Integer, BigDecimal> expenses;
        if (accountId == 0) {
            expenses = reportService.getExpensesByCategoryAllAccounts(from, to);
        } else {
            expenses = reportService.getExpensesByCategory(accountId, from, to);
        }

        expenses.forEach((catId, amt) -> {
            String catName = categoryMap.containsKey(catId) ?
                    categoryMap.get(catId).getName() : "Sconosciuta";
            System.out.printf("  %-20s: %s EUR%n", catName, MoneyUtil.format(amt, ""));
        });
    }

    private static void reportTransactionsByPeriod(int accountId, List<Account> accounts) {
        LocalDate from = readDateOptional("Data inizio (yyyy-MM-dd, invio per nessun limite)");
        LocalDate to = readDateOptional("Data fine (yyyy-MM-dd, invio per nessun limite)");

        System.out.println("\nTransazioni nel periodo:");
        System.out.println("--------------------------------------");

        if (accountId == 0) {
            for (Account acc : accounts) {
                List<Transaction> transactions = reportService.getTransactionsByPeriod(
                        acc.getId(), from, to);
                if (!transactions.isEmpty()) {
                    System.out.println("\n" + acc.getName() + ":");
                    printTransactions(transactions);
                }
            }
        } else {
            List<Transaction> transactions = reportService.getTransactionsByPeriod(
                    accountId, from, to);
            printTransactions(transactions);
        }
    }

    private static void printTransactions(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            System.out.println("  Nessuna transazione trovata.");
            return;
        }
        for (Transaction t : transactions) {
            printTransaction(t);
        }
    }

    private static void printTransaction(Transaction t) {
        System.out.printf("  %s | %-10s | %10s | %s%n",
                t.getDate().format(DATE_FORMAT),
                t.getType().getDescription(),
                MoneyUtil.format(t.getAmount(), ""),
                t.getDescription());
    }

    // ==================== CATEGORIE ====================

    /**
     * Gestisce il menu delle categorie.
     * Il menu rimane attivo fino a quando l'utente non sceglie "Indietro".
     */
    private static void handleCategories() throws WalletException {
        while (true) {
            System.out.println("\n+--- GESTIONE CATEGORIE ---+");
            System.out.println("| 1. Visualizza albero     |");
            System.out.println("| 2. Aggiungi categoria    |");
            System.out.println("| 3. Elimina categoria     |");
            System.out.println("| 4. Indietro              |");
            System.out.println("+--------------------------+");

            int choice = readInt("Scelta");

            switch (choice) {
                case 1:
                    showCategoryTree();
                    break;
                case 2:
                    addCategory();
                    break;
                case 3:
                    deleteCategory();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Scelta non valida.");
            }
        }
    }

    private static void showCategoryTree() {
        System.out.println("\nAlbero delle categorie:");
        System.out.println("======================================");

        List<CategoryComponent> categories = categoryService.getAllCategories();

        for (CategoryComponent category : categories) {
            if (category.getParentId() == null) {
                category.print("");
            }
        }
    }

    private static void addCategory() throws WalletException {
        System.out.println("\nNuova categoria:");

        String name = readString("Nome");
        String description = readString("Descrizione");

        System.out.println("Tipo: 1. Macro (con sottocategorie), 2. Standard (foglia)");
        int type = readInt("Tipo");

        System.out.println("\nCategorie esistenti (invio per nessun parent):");
        showCategoryTree();
        String parentInput = readString("ID Parent (vuoto per radice)");

        Integer parentId = null;

        if (!parentInput.trim().isEmpty()) {
            try {
                parentId = Integer.parseInt(parentInput.trim());
            } catch (NumberFormatException e) {
                System.out.println("ID non valido, categoria creata come radice.");
                parentId = null;
            }
        }

        try {
            CategoryComponent newCategory;
            if (type == 1) {
                newCategory = categoryService.createMacroCategory(name, description, parentId);
            } else {
                newCategory = categoryService.createStandardCategory(name, description, parentId);
            }
            System.out.println("\nCategoria '" + name + "' creata con ID: " + newCategory.getId());
        } catch (WalletException e) {
            System.out.println("Errore: " + e.getMessage());
            System.out.println("Categoria creata come radice.");
            // Riprova senza parent
            CategoryComponent newCategory;
            if (type == 1) {
                newCategory = categoryService.createMacroCategory(name, description, null);
            } else {
                newCategory = categoryService.createStandardCategory(name, description, null);
            }
            System.out.println("\nCategoria '" + name + "' creata con ID: " + newCategory.getId());
        }
    }

    /**
     * Gestisce l'eliminazione di una categoria.
     * Verifica che la categoria non abbia figli e non sia usata in transazioni.
     */
    private static void deleteCategory() throws WalletException {
        System.out.println("\nElimina categoria:");
        showCategoryTree();

        int categoryId = readInt("ID categoria da eliminare (0 per annullare)");

        if (categoryId == 0) {
            System.out.println("Operazione annullata.");
            return;
        }

        // Verifica se la categoria esiste
        Optional<CategoryComponent> categoryOpt = categoryService.findCategory(categoryId);
        if (categoryOpt.isEmpty()) {
            System.out.println("Categoria non trovata.");
            return;
        }

        CategoryComponent category = categoryOpt.get();

        // Verifica se puo' essere eliminata
        CategoryService.DeleteCheckResult check = categoryService.canDelete(categoryId);
        if (!check.canDelete()) {
            System.out.println("Impossibile eliminare: " + check.getReason());
            return;
        }

        // Conferma eliminazione
        System.out.println("\nStai per eliminare la categoria: " + category.getName());
        if (!readYesNo("Confermi l'eliminazione? (s/n)")) {
            System.out.println("Operazione annullata.");
            return;
        }

        // Elimina tramite servizio
        categoryService.deleteCategory(categoryId);

        System.out.println("\nCategoria '" + category.getName() + "' eliminata con successo.");
    }

    // ==================== VALUTE ====================

    /**
     * Gestisce il menu delle valute.
     * Il menu rimane attivo fino a quando l'utente non sceglie "Indietro".
     */
    private static void handleCurrencies() throws WalletException {
        while (true) {
            System.out.println("\n+--- GESTIONE VALUTE ---+");
            System.out.println("| 1. Lista valute        |");
            System.out.println("| 2. Inserisci tasso     |");
            System.out.println("| 3. Aggiungi valuta     |");
            System.out.println("| 4. Elimina valuta      |");
            System.out.println("| 5. Indietro            |");
            System.out.println("+------------------------+");

            int choice = readInt("Scelta");

            switch (choice) {
                case 1:
                    listCurrencies();
                    break;
                case 2:
                    updateRate();
                    break;
                case 3:
                    addCurrency();
                    break;
                case 4:
                    deleteCurrency();
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Scelta non valida.");
            }
        }
    }

    private static void listCurrencies() {
        System.out.println("\nValute disponibili:");
        System.out.println("--------------------------------------");

        List<Currency> currencies = currencyManager.getAllCurrencies();
        for (Currency currency : currencies) {
            LocalDate rateDate = currency.getLatestRateDate();
            String dateStr = rateDate != null ? rateDate.format(DATE_FORMAT) : "N/D";
            System.out.printf("  [%d] %s (%s) %s - Tasso: %s (dal %s)%n",
                    currency.getId(),
                    currency.getCode(),
                    currency.getName(),
                    currency.getSymbol(),
                    currency.getLatestRate().toPlainString(),
                    dateStr);
        }
    }

    private static void updateRate() throws WalletException {
        System.out.println("\nValute con tasso modificabile:");
        System.out.println("--------------------------------------");

        List<Currency> currencies = currencyManager.getAllCurrencies();
        List<Currency> editableCurrencies = currencies.stream()
                .filter(c -> !c.getCode().equals("EUR"))
                .toList();

        if (editableCurrencies.isEmpty()) {
            System.out.println("Nessuna valuta modificabile disponibile.");
            return;
        }

        for (Currency currency : editableCurrencies) {
            LocalDate rateDate = currency.getLatestRateDate();
            String dateStr = rateDate != null ? rateDate.format(DATE_FORMAT) : "N/D";
            System.out.printf("  %s (%s) %s - Tasso: %s (dal %s)%n",
                    currency.getCode(),
                    currency.getName(),
                    currency.getSymbol(),
                    currency.getLatestRate().toPlainString(),
                    dateStr);
        }

        String currencyCode = readString("Codice valuta (es: USD)").toUpperCase();

        if (currencyCode.equals("EUR")) {
            System.out.println("Il tasso di EUR e' fisso a 1.0 (valuta base).");
            return;
        }

        BigDecimal rate = readAmount("Nuovo tasso rispetto EUR");
        LocalDate date = readDate("Data tasso (yyyy-MM-dd, invio per oggi)");

        currencyManager.updateRate(currencyCode, date, rate);
        System.out.println("\nTasso aggiornato con successo!");
    }

    /**
     * Gestisce l'aggiunta di una nuova valuta.
     */
    private static void addCurrency() {
        System.out.println("\nNuova valuta:");

        String code = readString("Codice ISO (es: CHF)").toUpperCase();

        // Verifica che il codice non esista già
        try {
            currencyManager.getCurrencyByCode(code);
            System.out.println("Errore: una valuta con codice " + code + " esiste gia'.");
            return;
        } catch (WalletException e) {
            // OK, il codice non esiste
        }

        String name = readString("Nome (es: Franco Svizzero)");
        String symbol = readString("Simbolo (es: CHF)");
        BigDecimal rate = readAmount("Tasso rispetto EUR");

        int newId = currencyManager.generateNextId();
        Currency newCurrency = new Currency(newId, code, name, symbol);
        newCurrency.addRate(LocalDate.now(), rate);

        currencyManager.addCurrency(newCurrency);
        System.out.println("\nValuta '" + code + "' creata con ID: " + newId);
    }

    /**
     * Gestisce l'eliminazione di una valuta.
     * Verifica che non sia usata in conti o transazioni.
     */
    private static void deleteCurrency() throws WalletException {
        System.out.println("\nElimina valuta:");
        listCurrencies();

        int currencyId = readInt("ID valuta da eliminare (0 per annullare)");

        if (currencyId == 0) {
            System.out.println("Operazione annullata.");
            return;
        }

        // Verifica che la valuta esista
        Optional<Currency> currencyOpt = currencyManager.findCurrencyById(currencyId);
        if (currencyOpt.isEmpty()) {
            System.out.println("Valuta non trovata.");
            return;
        }

        Currency currency = currencyOpt.get();

        // Verifica se puo' essere eliminata
        CurrencyManager.DeleteCurrencyResult check = currencyManager.canDelete(currencyId);
        if (!check.canDelete()) {
            System.out.println("Impossibile eliminare: " + check.getReason());
            return;
        }

        // Conferma eliminazione
        System.out.println("\nStai per eliminare la valuta: " + currency.getCode() +
                " (" + currency.getName() + ")");
        if (!readYesNo("Confermi l'eliminazione? (s/n)")) {
            System.out.println("Operazione annullata.");
            return;
        }

        currencyManager.deleteCurrency(currencyId);
        System.out.println("\nValuta '" + currency.getCode() + "' eliminata con successo.");
    }

    // ==================== SALVA / ESCI ====================

    /**
     * Salva tutti i dati su file.
     */
    private static void handleSave() {
        try {
            CsvService.saveAll();
            System.out.println("\nDati salvati con successo!");
        } catch (WalletException e) {
            System.out.println("Errore durante il salvataggio: " + e.getMessage());
            logger.warning("Errore salvataggio: " + e.getMessage());
        }
    }

    /**
     * Gestisce l'uscita dall'applicazione.
     */
    private static void handleExit() {
        System.out.println("\nVuoi salvare prima di uscire? (s/n)");
        if (readYesNo("")) {
            handleSave();
        }

        System.out.println("Confermi l'uscita? (s/n)");
        if (readYesNo("")) {
            running = false;
        }
    }

    // ==================== INPUT HELPERS ====================

    /**
     * Legge una stringa dall'input.
     */
    private static String readString(String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine().trim();
    }

    /**
     * Legge un intero dall'input con validazione.
     */
    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String input = scanner.nextLine().trim();
            try {
                return InputValidator.parseInt(input, prompt);
            } catch (InvalidInputException e) {
                System.out.println("Inserisci un numero valido.");
            }
        }
    }

    /**
     * Legge un importo BigDecimal dall'input con validazione.
     */
    private static BigDecimal readAmount(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String input = scanner.nextLine().trim();
            try {
                BigDecimal amount = InputValidator.parseAmount(input);
                InputValidator.validatePositiveAmount(amount);
                return amount;
            } catch (InvalidInputException e) {
                System.out.println("Inserisci un importo valido (es: 100.50).");
            }
        }
    }

    /**
     * Legge una data dall'input con validazione.
     * Se l'input e' vuoto, restituisce la data odierna.
     */
    private static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                return LocalDate.now();
            }

            try {
                LocalDate date = LocalDate.parse(input, DATE_FORMAT);
                InputValidator.validateDate(date);
                return date;
            } catch (DateTimeParseException e) {
                System.out.println("Formato data non valido. Usa yyyy-MM-dd (es: 2024-01-15).");
            } catch (InvalidInputException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Legge una data opzionale dall'input con validazione.
     * Se l'input e' vuoto, restituisce null (nessun limite).
     */
    private static LocalDate readDateOptional(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                return null;
            }

            try {
                LocalDate date = LocalDate.parse(input, DATE_FORMAT);
                InputValidator.validateDate(date);
                return date;
            } catch (DateTimeParseException e) {
                System.out.println("Formato data non valido. Usa yyyy-MM-dd (es: 2024-01-15).");
            } catch (InvalidInputException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Legge una risposta si/no dall'input.
     */
    private static boolean readYesNo(String prompt) {
        while (true) {
            if (!prompt.isEmpty()) {
                System.out.print(prompt + " (s/n): ");
            }
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("s") || input.equals("si") || input.equals("y") || input.equals("yes")) {
                return true;
            }
            if (input.equals("n") || input.equals("no")) {
                return false;
            }
            System.out.println("Rispondi con 's' o 'n'.");
        }
    }

    /**
     * Permette di selezionare un elemento da una lista.
     *
     * @return l'indice selezionato o -1 se annullato
     */
    private static int selectFromList(String prompt, List<?> items) {
        for (int i = 0; i < items.size(); i++) {
            System.out.printf("  [%d] %s%n", i + 1, items.get(i));
        }

        while (true) {
            int choice = readInt(prompt + " (1-" + items.size() + ", 0 per annullare)");
            if (choice == 0) {
                return -1;
            }
            if (choice >= 1 && choice <= items.size()) {
                return choice - 1;
            }
            System.out.println("Scelta non valida.");
        }
    }

    /**
     * Recupera la valuta associata a un account.
     */
    private static Currency getCurrencyForAccount(Account account) {
        return currencyManager.findCurrencyById(account.getCurrencyId()).orElse(null);
    }
}
