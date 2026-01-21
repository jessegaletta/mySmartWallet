package edu.epicode.mysmartwallet.service;

import edu.epicode.mysmartwallet.model.Account;
import edu.epicode.mysmartwallet.model.Transaction;
import edu.epicode.mysmartwallet.model.TransactionType;
import edu.epicode.mysmartwallet.repository.DataStorage;
import edu.epicode.mysmartwallet.repository.Repository;
import edu.epicode.mysmartwallet.util.AppLogger;
import edu.epicode.mysmartwallet.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Servizio per la generazione di report finanziari.
 *
 * <p>Utilizza intensivamente la Stream API di Java per elaborare
 * e aggregare i dati delle transazioni in modo efficiente e dichiarativo.</p>
 *
 * <p>Funzionalità disponibili:
 * <ul>
 *   <li>Calcolo totali entrate/uscite</li>
 *   <li>Raggruppamento spese per categoria</li>
 *   <li>Filtraggio transazioni per periodo e tipo</li>
 *   <li>Statistiche (media, massimo)</li>
 *   <li>Ricerca per descrizione</li>
 * </ul>
 * </p>
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class ReportService {

    private static final Logger logger = AppLogger.getLogger(ReportService.class);

    private final Repository<Account> accountRepository;

    /**
     * Crea un nuovo ReportService recuperando i repository da DataStorage.
     */
    public ReportService() {
        DataStorage dataStorage = DataStorage.getInstance();
        this.accountRepository = dataStorage.getAccountRepository();
        logger.info("ReportService inizializzato");
    }

    /**
     * Calcola il totale delle uscite per un conto.
     *
     * @param accountId l'ID del conto
     * @return il totale delle uscite
     */
    public BigDecimal getTotalExpenses(int accountId) {
        List<Transaction> transactions = getTransactionsForAccount(accountId);

        BigDecimal total = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, MoneyUtil::add);

        logger.fine("Totale uscite per conto " + accountId + ": " + total);
        return total;
    }

    /**
     * Calcola il totale delle entrate per un conto.
     *
     * @param accountId l'ID del conto
     * @return il totale delle entrate
     */
    public BigDecimal getTotalIncome(int accountId) {
        List<Transaction> transactions = getTransactionsForAccount(accountId);

        BigDecimal total = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, MoneyUtil::add);

        logger.fine("Totale entrate per conto " + accountId + ": " + total);
        return total;
    }

    /**
     * Raggruppa le uscite per categoria.
     *
     * @param accountId l'ID del conto
     * @return mappa con ID categoria come chiave e totale spese come valore
     */
    public Map<Integer, BigDecimal> getExpensesByCategory(int accountId) {
        List<Transaction> transactions = getTransactionsForAccount(accountId);

        Map<Integer, BigDecimal> result = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategoryId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Transaction::getAmount,
                                MoneyUtil::add)));

        logger.fine("Spese per categoria calcolate per conto " + accountId);
        return result;
    }

    /**
     * Recupera le transazioni in un periodo specifico.
     *
     * @param accountId l'ID del conto
     * @param from      la data di inizio (inclusa)
     * @param to        la data di fine (inclusa)
     * @return lista delle transazioni nel periodo, ordinate per data
     */
    public List<Transaction> getTransactionsByPeriod(int accountId, LocalDate from, LocalDate to) {
        List<Transaction> transactions = getTransactionsForAccount(accountId);

        List<Transaction> result = transactions.stream()
                .filter(t -> !t.getDate().isBefore(from) && !t.getDate().isAfter(to))
                .sorted(Comparator.comparing(Transaction::getDate))
                .collect(Collectors.toList());

        logger.fine("Transazioni nel periodo " + from + " - " + to +
                " per conto " + accountId + ": " + result.size());
        return result;
    }

    /**
     * Recupera le transazioni di un tipo specifico.
     *
     * @param accountId l'ID del conto
     * @param type      il tipo di transazione
     * @return lista delle transazioni del tipo specificato
     */
    public List<Transaction> getTransactionsByType(int accountId, TransactionType type) {
        List<Transaction> transactions = getTransactionsForAccount(accountId);

        List<Transaction> result = transactions.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.toList());

        logger.fine("Transazioni " + type + " per conto " + accountId + ": " + result.size());
        return result;
    }

    /**
     * Calcola la media delle uscite per un conto.
     *
     * @param accountId l'ID del conto
     * @return la media delle uscite, o zero se non ci sono uscite
     */
    public BigDecimal getAverageExpense(int accountId) {
        List<Transaction> transactions = getTransactionsForAccount(accountId);

        List<BigDecimal> expenses = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .collect(Collectors.toList());

        if (expenses.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = expenses.stream()
                .reduce(BigDecimal.ZERO, MoneyUtil::add);

        BigDecimal average = MoneyUtil.divide(sum, BigDecimal.valueOf(expenses.size()));

        logger.fine("Media uscite per conto " + accountId + ": " + average);
        return average;
    }

    /**
     * Trova la spesa più grande per un conto.
     *
     * @param accountId l'ID del conto
     * @return la transazione con l'importo maggiore, o Optional vuoto se non ci sono uscite
     */
    public Optional<Transaction> getLargestExpense(int accountId) {
        List<Transaction> transactions = getTransactionsForAccount(accountId);

        Optional<Transaction> result = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .max(Comparator.comparing(Transaction::getAmount));

        if (result.isPresent()) {
            logger.fine("Spesa maggiore per conto " + accountId + ": " + result.get().getAmount());
        }
        return result;
    }

    /**
     * Conta le transazioni raggruppate per tipo.
     *
     * @param accountId l'ID del conto
     * @return mappa con tipo transazione come chiave e conteggio come valore
     */
    public Map<TransactionType, Long> getTransactionCountByType(int accountId) {
        List<Transaction> transactions = getTransactionsForAccount(accountId);

        Map<TransactionType, Long> result = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getType,
                        Collectors.counting()));

        logger.fine("Conteggio transazioni per tipo calcolato per conto " + accountId);
        return result;
    }

    /**
     * Cerca transazioni per parola chiave nella descrizione.
     *
     * @param accountId l'ID del conto
     * @param keyword   la parola chiave da cercare (case insensitive)
     * @return lista delle transazioni che contengono la parola chiave
     */
    public List<Transaction> searchByDescription(int accountId, String keyword) {
        List<Transaction> transactions = getTransactionsForAccount(accountId);
        String lowercaseKeyword = keyword.toLowerCase();

        List<Transaction> result = transactions.stream()
                .filter(t -> t.getDescription().toLowerCase().contains(lowercaseKeyword))
                .collect(Collectors.toList());

        logger.fine("Ricerca '" + keyword + "' per conto " + accountId +
                ": " + result.size() + " risultati");
        return result;
    }

    /**
     * Recupera le transazioni per un account dal repository.
     */
    private List<Transaction> getTransactionsForAccount(int accountId) {
        return accountRepository.findById(accountId)
                .map(Account::getTransactions)
                .orElse(List.of());
    }
}
