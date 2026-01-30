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
import java.util.logging.Logger;

/**
 * Classe principale dell'applicazione MySmartWallet.
 * Fornisce un'interfaccia CLI interattiva per la gestione della contabilità personale.
 *
 * <p>Funzionalità disponibili:
 * <ul>
 *   <li>Dashboard con riepilogo conti e saldi</li>
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
            AppLogger.error("Errore critico nell'applicazione", e);
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
                AppLogger.error("Errore durante la generazione dei dati", ex);
                System.out.println("Errore durante la generazione dei dati iniziali.");
            }
        }

        walletService = new WalletService(new HistoricalExchangeStrategy());
        reportService = new ReportService();
        currencyManager = CurrencyManager.getInstance();

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
                        handleDashboard();
                        break;
                    case 2:
                        handleNewTransaction();
                        break;
                    case 3:
                        handleTransfer();
                        break;
                    case 4:
                        handleReports();
                        break;
                    case 5:
                        handleCategories();
                        break;
                    case 6:
                        handleCurrencies();
                        break;
                    case 7:
                        handleSave();
                        break;
                    case 8:
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
                AppLogger.error("Errore imprevisto nel menu", e);
            }
        }

        System.out.println("\nArrivederci!");
    }

    /**
     * Stampa il menu principale.
     */
    private static void printMainMenu() {
        System.out.println("\n+======================================+");
        System.out.println("|         MENU PRINCIPALE              |");
        System.out.println("+======================================+");
        System.out.println("| 1. Dashboard                         |");
        System.out.println("| 2. Nuova Transazione                 |");
        System.out.println("| 3. Trasferimento                     |");
        System.out.println("| 4. Report                            |");
        System.out.println("| 5. Gestione Categorie                |");
        System.out.println("| 6. Gestione Valute                   |");
        System.out.println("| 7. Salva Dati                        |");
        System.out.println("| 8. Esci                              |");
        System.out.println("+======================================+");
    }

    // ==================== DASHBOARD ====================

    /**
     * Mostra la dashboard con il riepilogo dei conti.
     */
    private static void handleDashboard() {
        System.out.println("\n+======================================+");
        System.out.println("|            DASHBOARD                 |");
        System.out.println("+======================================+");

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

    // ==================== NUOVA TRANSAZIONE ====================

    /**
     * Gestisce la creazione di una nuova transazione.
     */
    private static void handleNewTransaction() throws WalletException {
        System.out.println("\n+--- NUOVA TRANSAZIONE ---+");
        System.out.println("| 1. Entrata              |");
        System.out.println("| 2. Uscita               |");
        System.out.println("| 3. Indietro             |");
        System.out.println("+-------------------------+");

        int choice = readInt("Scelta");

        if (choice == 3) {
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
        List<CategoryComponent> categories = DataStorage.getInstance()
                .getCategoryRepository().findAll();
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

        walletService.addTransactionWithCurrency(account.getId(), type, finalAmount,
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
     */
    private static void handleReports() throws WalletException {
        System.out.println("\n+--- REPORT ---+");
        System.out.println("| 1. Totale spese          |");
        System.out.println("| 2. Totale entrate        |");
        System.out.println("| 3. Spese per categoria   |");
        System.out.println("| 4. Transazioni periodo   |");
        System.out.println("| 5. Spesa piu' grande     |");
        System.out.println("| 6. Indietro              |");
        System.out.println("+--------------------------+");

        int choice = readInt("Scelta");

        if (choice == 6) return;

        List<Account> accounts = walletService.getAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("Nessun conto disponibile.");
            return;
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
                reportLargestExpense(accountId, accounts);
                break;
            default:
                System.out.println("Scelta non valida.");
        }
    }

    private static void reportTotalExpenses(int accountId, List<Account> accounts) {
        if (accountId == 0) {
            BigDecimal total = BigDecimal.ZERO;
            for (Account acc : accounts) {
                total = MoneyUtil.add(total, reportService.getTotalExpenses(acc.getId()));
            }
            System.out.printf("%nTotale spese (tutti i conti): %s%n", MoneyUtil.format(total, ""));
        } else {
            BigDecimal total = reportService.getTotalExpenses(accountId);
            System.out.printf("%nTotale spese: %s%n", MoneyUtil.format(total, ""));
        }
    }

    private static void reportTotalIncome(int accountId, List<Account> accounts) {
        if (accountId == 0) {
            BigDecimal total = BigDecimal.ZERO;
            for (Account acc : accounts) {
                total = MoneyUtil.add(total, reportService.getTotalIncome(acc.getId()));
            }
            System.out.printf("%nTotale entrate (tutti i conti): %s%n", MoneyUtil.format(total, ""));
        } else {
            BigDecimal total = reportService.getTotalIncome(accountId);
            System.out.printf("%nTotale entrate: %s%n", MoneyUtil.format(total, ""));
        }
    }

    private static void reportExpensesByCategory(int accountId, List<Account> accounts) {
        System.out.println("\nSpese per categoria:");
        System.out.println("--------------------------------------");

        Map<Integer, CategoryComponent> categoryMap = new java.util.HashMap<>();
        DataStorage.getInstance().getCategoryRepository().findAll()
                .forEach(c -> categoryMap.put(c.getId(), c));

        if (accountId == 0) {
            Map<Integer, BigDecimal> totals = new java.util.HashMap<>();
            for (Account acc : accounts) {
                Map<Integer, BigDecimal> expenses = reportService.getExpensesByCategory(acc.getId());
                expenses.forEach((catId, amt) ->
                        totals.merge(catId, amt, MoneyUtil::add));
            }
            totals.forEach((catId, amt) -> {
                String catName = categoryMap.containsKey(catId) ?
                        categoryMap.get(catId).getName() : "Sconosciuta";
                System.out.printf("  %-20s: %s%n", catName, MoneyUtil.format(amt, ""));
            });
        } else {
            Map<Integer, BigDecimal> expenses = reportService.getExpensesByCategory(accountId);
            expenses.forEach((catId, amt) -> {
                String catName = categoryMap.containsKey(catId) ?
                        categoryMap.get(catId).getName() : "Sconosciuta";
                System.out.printf("  %-20s: %s%n", catName, MoneyUtil.format(amt, ""));
            });
        }
    }

    private static void reportTransactionsByPeriod(int accountId, List<Account> accounts) {
        LocalDate from = readDate("Data inizio (yyyy-MM-dd)");
        LocalDate to = readDate("Data fine (yyyy-MM-dd)");

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

    private static void reportLargestExpense(int accountId, List<Account> accounts) {
        if (accountId == 0) {
            Transaction largest = null;
            for (Account acc : accounts) {
                Optional<Transaction> opt = reportService.getLargestExpense(acc.getId());
                if (opt.isPresent()) {
                    if (largest == null ||
                        opt.get().getAmount().compareTo(largest.getAmount()) > 0) {
                        largest = opt.get();
                    }
                }
            }
            if (largest != null) {
                System.out.println("\nSpesa piu' grande (tutti i conti):");
                printTransaction(largest);
            } else {
                System.out.println("\nNessuna spesa trovata.");
            }
        } else {
            Optional<Transaction> opt = reportService.getLargestExpense(accountId);
            if (opt.isPresent()) {
                System.out.println("\nSpesa piu' grande:");
                printTransaction(opt.get());
            } else {
                System.out.println("\nNessuna spesa trovata.");
            }
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
     */
    private static void handleCategories() throws WalletException {
        System.out.println("\n+--- GESTIONE CATEGORIE ---+");
        System.out.println("| 1. Visualizza albero     |");
        System.out.println("| 2. Aggiungi categoria    |");
        System.out.println("| 3. Indietro              |");
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
                break;
            default:
                System.out.println("Scelta non valida.");
        }
    }

    private static void showCategoryTree() {
        System.out.println("\nAlbero delle categorie:");
        System.out.println("======================================");

        List<CategoryComponent> categories = DataStorage.getInstance()
                .getCategoryRepository().findAll();

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
            }
        }

        int newId = DataStorage.getInstance().getCategoryRepository().generateNextId();

        CategoryComponent newCategory;
        if (type == 1) {
            newCategory = new MacroCategory(newId, name, description, parentId);
        } else {
            newCategory = new edu.epicode.mysmartwallet.model.category.StandardCategory(
                    newId, name, description, parentId);
        }

        DataStorage.getInstance().getCategoryRepository().save(newCategory);

        if (parentId != null) {
            DataStorage.getInstance().getCategoryRepository().findById(parentId)
                    .ifPresent(parent -> {
                        if (parent instanceof MacroCategory) {
                            ((MacroCategory) parent).addChild(newCategory);
                        }
                    });
        }

        System.out.println("\nCategoria '" + name + "' creata con ID: " + newId);
    }

    // ==================== VALUTE ====================

    /**
     * Gestisce il menu delle valute.
     */
    private static void handleCurrencies() throws WalletException {
        System.out.println("\n+--- GESTIONE VALUTE ---+");
        System.out.println("| 1. Lista valute        |");
        System.out.println("| 2. Aggiorna tasso      |");
        System.out.println("| 3. Indietro            |");
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
                break;
            default:
                System.out.println("Scelta non valida.");
        }
    }

    private static void listCurrencies() {
        System.out.println("\nValute disponibili:");
        System.out.println("--------------------------------------");

        List<Currency> currencies = currencyManager.getAllCurrencies();
        for (Currency currency : currencies) {
            System.out.printf("  [%d] %s (%s) %s - Tasso: %s%n",
                    currency.getId(),
                    currency.getCode(),
                    currency.getName(),
                    currency.getSymbol(),
                    currency.getLatestRate().toPlainString());
        }
    }

    private static void updateRate() throws WalletException {
        listCurrencies();

        String currencyCode = readString("Codice valuta (es: USD)");
        BigDecimal rate = readAmount("Nuovo tasso rispetto EUR");
        LocalDate date = readDate("Data tasso (yyyy-MM-dd, invio per oggi)");

        currencyManager.updateRate(currencyCode.toUpperCase(), date, rate);
        System.out.println("\nTasso aggiornato con successo!");
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
        try {
            return currencyManager.getAllCurrencies().stream()
                    .filter(c -> c.getId() == account.getCurrencyId())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
