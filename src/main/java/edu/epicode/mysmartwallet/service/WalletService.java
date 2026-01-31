package edu.epicode.mysmartwallet.service;

import edu.epicode.mysmartwallet.exception.InsufficientFundsException;
import edu.epicode.mysmartwallet.exception.InvalidInputException;
import edu.epicode.mysmartwallet.exception.ItemNotFoundException;
import edu.epicode.mysmartwallet.exception.RateNotFoundException;
import edu.epicode.mysmartwallet.model.Account;
import edu.epicode.mysmartwallet.model.Currency;
import edu.epicode.mysmartwallet.model.Transaction;
import edu.epicode.mysmartwallet.model.TransactionType;
import edu.epicode.mysmartwallet.repository.DataStorage;
import edu.epicode.mysmartwallet.repository.Repository;
import edu.epicode.mysmartwallet.service.factory.TransactionFactory;
import edu.epicode.mysmartwallet.service.observer.BalanceObserver;
import edu.epicode.mysmartwallet.service.strategy.ExchangeStrategy;
import edu.epicode.mysmartwallet.util.AppLogger;
import edu.epicode.mysmartwallet.util.InputValidator;
import edu.epicode.mysmartwallet.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Servizio principale per la gestione dei conti e delle operazioni finanziarie.
 *
 * <p>Fornisce le funzionalità core dell'applicazione:
 * <ul>
 *   <li>Creazione e gestione conti</li>
 *   <li>Aggiunta transazioni (entrate, uscite)</li>
 *   <li>Trasferimenti tra conti con conversione valuta</li>
 *   <li>Calcolo saldo totale multi-valuta</li>
 * </ul>
 * </p>
 *
 * <p>Utilizza dependency injection per la strategia di conversione valuta,
 * permettendo di configurare diverse strategie a runtime.</p>
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class WalletService {

    private static final Logger logger = AppLogger.getLogger(WalletService.class);

    private final Repository<Account> accountRepository;
    private final Repository<Transaction> transactionRepository;
    private final ExchangeStrategy exchangeStrategy;
    private final CurrencyManager currencyManager;
    private final List<BalanceObserver> globalObservers;

    /**
     * Crea un nuovo WalletService con la strategia di conversione specificata.
     *
     * @param exchangeStrategy la strategia per la conversione valutaria
     */
    public WalletService(ExchangeStrategy exchangeStrategy) {
        DataStorage dataStorage = DataStorage.getInstance();
        this.accountRepository = dataStorage.getAccountRepository();
        this.transactionRepository = dataStorage.getTransactionRepository();
        this.exchangeStrategy = exchangeStrategy;
        this.currencyManager = CurrencyManager.getInstance();
        this.globalObservers = new ArrayList<>();
        logger.info("WalletService inizializzato");
    }

    /**
     * Aggiunge un observer globale che verrà registrato su tutti i conti
     * esistenti e su quelli creati in futuro.
     *
     * @param observer l'observer da aggiungere globalmente
     */
    public void addGlobalObserver(BalanceObserver observer) {
        globalObservers.add(observer);
        // Aggiungi a tutti i conti esistenti
        for (Account account : accountRepository.findAll()) {
            account.addObserver(observer);
        }
        logger.fine("Observer globale aggiunto");
    }

    /**
     * Crea un nuovo conto.
     *
     * @param name           il nome del conto
     * @param currencyCode   il codice della valuta (es: EUR, USD)
     * @param initialBalance il saldo iniziale
     * @return il conto creato
     * @throws InvalidInputException  se i parametri non sono validi
     * @throws ItemNotFoundException  se la valuta non esiste
     */
    public Account createAccount(String name, String currencyCode, BigDecimal initialBalance)
            throws InvalidInputException, ItemNotFoundException {

        InputValidator.validateNotEmpty(name, "nome conto");
        InputValidator.validateNotEmpty(currencyCode, "codice valuta");
        InputValidator.validateNotNull(initialBalance, "saldo iniziale");

        Currency currency = currencyManager.getCurrencyByCode(currencyCode);

        int id = accountRepository.generateNextId();
        Account account = new Account(id, name, currency.getId(), initialBalance);

        // Registra gli observer globali
        for (BalanceObserver observer : globalObservers) {
            account.addObserver(observer);
        }

        accountRepository.save(account);
        logger.info("Creato conto: " + name + " (ID: " + id + ")");

        return account;
    }

    /**
     * Aggiunge una transazione a un conto con supporto per conversione valutaria.
     *
     * @param accountId          l'ID del conto
     * @param type               il tipo di transazione
     * @param amount             l'importo (nella valuta del conto)
     * @param description        la descrizione
     * @param categoryId         l'ID della categoria
     * @param date               la data della transazione
     * @param originalAmount     l'importo originale (null se nessuna conversione)
     * @param originalCurrencyId l'ID della valuta originale (null se nessuna conversione)
     * @param exchangeRate       il tasso di cambio usato (null se nessuna conversione)
     * @throws ItemNotFoundException se il conto non esiste
     * @throws InvalidInputException se i parametri non sono validi
     */
    public void addTransaction(int accountId, TransactionType type, BigDecimal amount,
            String description, int categoryId, LocalDate date,
            BigDecimal originalAmount, Integer originalCurrencyId, BigDecimal exchangeRate)
            throws ItemNotFoundException, InvalidInputException {

        Account account = getAccount(accountId);

        Transaction transaction;
        switch (type) {
            case INCOME:
                transaction = TransactionFactory.createIncome(
                        amount, description, categoryId, accountId, date,
                        originalAmount, originalCurrencyId, exchangeRate);
                break;
            case EXPENSE:
                transaction = TransactionFactory.createExpense(
                        amount, description, categoryId, accountId, date,
                        originalAmount, originalCurrencyId, exchangeRate);
                break;
            default:
                throw new InvalidInputException(
                        "Usa il metodo transfer() per i trasferimenti");
        }

        account.addTransaction(transaction);
        transactionRepository.save(transaction);
        accountRepository.save(account);

        logger.info("Transazione " + type + " aggiunta al conto " + accountId);
    }

    /**
     * Esegue un trasferimento tra due conti.
     *
     * <p>Se i conti hanno valute diverse e toAmount e' specificato,
     * viene usato quell'importo per il conto destinazione e il tasso
     * viene calcolato dal rapporto toAmount/fromAmount.
     * Se toAmount e' null, viene usata la strategia di cambio configurata.</p>
     *
     * @param fromAccountId l'ID del conto sorgente
     * @param toAccountId   l'ID del conto destinazione
     * @param fromAmount    l'importo da trasferire (nella valuta del conto sorgente)
     * @param toAmount      l'importo nella valuta del conto destinazione (null per conversione automatica)
     * @param description   la descrizione del trasferimento
     * @param categoryId    l'ID della categoria
     * @param date          la data del trasferimento
     * @throws ItemNotFoundException      se uno dei conti non esiste
     * @throws InvalidInputException      se i parametri non sono validi
     * @throws InsufficientFundsException se il conto sorgente non ha fondi sufficienti
     * @throws RateNotFoundException      se non è disponibile un tasso di cambio
     */
    public void transfer(int fromAccountId, int toAccountId, BigDecimal fromAmount,
            BigDecimal toAmount, String description, int categoryId, LocalDate date)
            throws ItemNotFoundException, InvalidInputException,
            InsufficientFundsException, RateNotFoundException {

        Account fromAccount = getAccount(fromAccountId);
        Account toAccount = getAccount(toAccountId);

        // Verifica fondi sufficienti
        if (MoneyUtil.isGreaterThan(fromAmount, fromAccount.getBalance())) {
            throw new InsufficientFundsException(
                    "Fondi insufficienti sul conto " + fromAccount.getName() +
                            ". Disponibile: " + fromAccount.getBalance() +
                            ", Richiesto: " + fromAmount);
        }

        // Recupera le valute dei conti
        Currency fromCurrency = currencyManager.getCurrencyByCode(
                getCurrencyCodeForAccount(fromAccount));
        Currency toCurrency = currencyManager.getCurrencyByCode(
                getCurrencyCodeForAccount(toAccount));

        boolean differentCurrencies = !fromCurrency.getCode().equals(toCurrency.getCode());

        // Determina l'importo finale per il conto destinazione
        BigDecimal finalToAmount;
        BigDecimal exchangeRate = null;

        if (differentCurrencies) {
            if (toAmount != null) {
                // Importo manuale: calcola il tasso dal rapporto
                finalToAmount = toAmount;
                exchangeRate = MoneyUtil.divideRates(toAmount, fromAmount);
            } else {
                // Conversione automatica con la strategia
                finalToAmount = exchangeStrategy.convert(fromAmount, fromCurrency, toCurrency, date);
            }
        } else {
            finalToAmount = fromAmount;
        }

        // Transazione di uscita sul conto sorgente (TRANSFER)
        List<Transaction> transfers = TransactionFactory.createTransfer(
                fromAmount, description, fromAccountId, toAccountId, categoryId, date);

        Transaction outgoing = transfers.get(0);
        fromAccount.addTransaction(outgoing);
        transactionRepository.save(outgoing);
        accountRepository.save(fromAccount);

        // Transazione di entrata sul conto destinazione
        int inId = transactionRepository.generateNextId();
        Transaction.Builder incomingBuilder = new Transaction.Builder()
                .withId(inId)
                .withAccountId(toAccountId)
                .withType(TransactionType.INCOME)
                .withDate(date)
                .withAmount(finalToAmount)
                .withDescription(description + " (trasferimento da " + fromAccount.getName() + ")")
                .withCategoryId(categoryId);

        // Se valute diverse e importo manuale, registra info sulla conversione
        if (differentCurrencies && toAmount != null) {
            incomingBuilder.withOriginalAmount(fromAmount);
            incomingBuilder.withOriginalCurrencyId(fromCurrency.getId());
            incomingBuilder.withExchangeRate(exchangeRate);
        }

        Transaction incoming = incomingBuilder.build();
        toAccount.addTransaction(incoming);
        transactionRepository.save(incoming);
        accountRepository.save(toAccount);

        logger.info("Trasferimento completato: " + fromAmount + " " + fromCurrency.getCode() +
                " -> " + finalToAmount + " " + toCurrency.getCode());
    }

    /**
     * Recupera un conto per ID.
     *
     * @param accountId l'ID del conto
     * @return il conto trovato
     * @throws ItemNotFoundException se il conto non esiste
     */
    public Account getAccount(int accountId) throws ItemNotFoundException {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Conto con ID " + accountId + " non trovato"));
    }

    /**
     * Restituisce tutti i conti.
     *
     * @return lista di tutti i conti
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Calcola il saldo totale di tutti i conti convertito nella valuta target.
     *
     * @param targetCurrencyCode il codice della valuta target
     * @param date               la data per il tasso di cambio
     * @return il saldo totale nella valuta target
     * @throws RateNotFoundException se non è disponibile un tasso di cambio
     */
    public BigDecimal getTotalBalance(String targetCurrencyCode, LocalDate date)
            throws RateNotFoundException {

        BigDecimal total = BigDecimal.ZERO;

        try {
            Currency targetCurrency = currencyManager.getCurrencyByCode(targetCurrencyCode);

            for (Account account : accountRepository.findAll()) {
                Currency accountCurrency = currencyManager.getCurrencyByCode(
                        getCurrencyCodeForAccount(account));

                BigDecimal convertedBalance = exchangeStrategy.convert(
                        account.getBalance(), accountCurrency, targetCurrency, date);

                total = MoneyUtil.add(total, convertedBalance);
            }
        } catch (ItemNotFoundException e) {
            throw new RateNotFoundException(
                    "Valuta non trovata per il calcolo del saldo totale", e);
        }

        logger.fine("Saldo totale in " + targetCurrencyCode + ": " + total);
        return total;
    }

    /**
     * Recupera il codice valuta per un account.
     */
    private String getCurrencyCodeForAccount(Account account) throws ItemNotFoundException {
        return currencyManager.getAllCurrencies().stream()
                .filter(c -> c.getId() == account.getCurrencyId())
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException(
                        "Valuta con ID " + account.getCurrencyId() + " non trovata"))
                .getCode();
    }

    /**
     * Recupera una transazione per ID.
     *
     * @param transactionId l'ID della transazione
     * @return la transazione trovata
     * @throws ItemNotFoundException se la transazione non esiste
     */
    public Transaction getTransaction(int transactionId) throws ItemNotFoundException {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Transazione con ID " + transactionId + " non trovata"));
    }

    /**
     * Elimina una transazione e aggiorna il saldo del conto associato.
     * Il saldo viene ricalcolato automaticamente poiche' Account.getBalance()
     * calcola dinamicamente il saldo dalla lista delle transazioni.
     *
     * @param transactionId l'ID della transazione da eliminare
     * @throws ItemNotFoundException se la transazione o il conto non esistono
     */
    public void deleteTransaction(int transactionId) throws ItemNotFoundException {
        Transaction transaction = getTransaction(transactionId);
        Account account = getAccount(transaction.getAccountId());

        // Rimuove la transazione dal conto (il saldo si ricalcola automaticamente)
        boolean removed = account.removeTransaction(transactionId);
        if (!removed) {
            throw new ItemNotFoundException(
                    "Transazione " + transactionId + " non trovata nel conto " + account.getName());
        }

        // Rimuove dal repository
        transactionRepository.delete(transactionId);

        // Salva il conto aggiornato
        accountRepository.save(account);

        logger.info("Eliminata transazione " + transactionId + " dal conto " + account.getName());
    }
}
