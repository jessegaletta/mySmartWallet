package edu.epicode.mysmartwallet.service;

import edu.epicode.mysmartwallet.exception.RateNotFoundException;
import edu.epicode.mysmartwallet.model.Account;
import edu.epicode.mysmartwallet.model.Currency;
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
 *   <li>Filtraggio transazioni per periodo</li>
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
    private final CurrencyManager currencyManager;

    /**
     * Crea un nuovo ReportService recuperando i repository da DataStorage.
     */
    public ReportService() {
        DataStorage dataStorage = DataStorage.getInstance();
        this.accountRepository = dataStorage.getAccountRepository();
        this.currencyManager = CurrencyManager.getInstance();
        logger.info("ReportService inizializzato");
    }

    /**
     * Converte un importo nella valuta EUR usando il tasso del giorno della transazione.
     *
     * @param amount   l'importo da convertire
     * @param currency la valuta dell'importo
     * @param date     la data per il tasso di cambio
     * @return l'importo convertito in EUR
     */
    private BigDecimal convertToEur(BigDecimal amount, Currency currency, LocalDate date) {
        if (currency == null || currency.getCode().equals("EUR")) {
            return amount;
        }
        try {
            BigDecimal rate = currency.getRateForDate(date);
            return MoneyUtil.divide(amount, rate);
        } catch (RateNotFoundException e) {
            logger.warning("Tasso non trovato per " + currency.getCode() + " alla data " + date +
                    ", uso ultimo tasso disponibile");
            return MoneyUtil.divide(amount, currency.getLatestRate());
        }
    }

    /**
     * Recupera la valuta associata a un account.
     */
    private Currency getCurrencyForAccount(Account account) {
        return currencyManager.getAllCurrencies().stream()
                .filter(c -> c.getId() == account.getCurrencyId())
                .findFirst()
                .orElse(null);
    }

    /**
     * Verifica se una data rientra in un periodo.
     *
     * @param date la data da verificare
     * @param from la data di inizio (inclusa), null per nessun limite inferiore
     * @param to   la data di fine (inclusa), null per nessun limite superiore
     * @return true se la data rientra nel periodo
     */
    private boolean isInPeriod(LocalDate date, LocalDate from, LocalDate to) {
        if (from != null && date.isBefore(from)) {
            return false;
        }
        if (to != null && date.isAfter(to)) {
            return false;
        }
        return true;
    }

    /**
     * Calcola il totale delle uscite per un conto in un periodo, convertito in EUR.
     *
     * @param accountId l'ID del conto
     * @param from      la data di inizio (inclusa), null per nessun limite
     * @param to        la data di fine (inclusa), null per nessun limite
     * @return il totale delle uscite in EUR
     */
    public BigDecimal getTotalExpenses(int accountId, LocalDate from, LocalDate to) {
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return BigDecimal.ZERO;
        }

        Currency currency = getCurrencyForAccount(account);

        BigDecimal total = account.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .filter(t -> isInPeriod(t.getDate(), from, to))
                .map(t -> convertToEur(t.getAmount(), currency, t.getDate()))
                .reduce(BigDecimal.ZERO, MoneyUtil::add);

        logger.fine("Totale uscite per conto " + accountId + " (EUR): " + total);
        return total;
    }

    /**
     * Calcola il totale delle entrate per un conto in un periodo, convertito in EUR.
     *
     * @param accountId l'ID del conto
     * @param from      la data di inizio (inclusa), null per nessun limite
     * @param to        la data di fine (inclusa), null per nessun limite
     * @return il totale delle entrate in EUR
     */
    public BigDecimal getTotalIncome(int accountId, LocalDate from, LocalDate to) {
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return BigDecimal.ZERO;
        }

        Currency currency = getCurrencyForAccount(account);

        BigDecimal total = account.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .filter(t -> isInPeriod(t.getDate(), from, to))
                .map(t -> convertToEur(t.getAmount(), currency, t.getDate()))
                .reduce(BigDecimal.ZERO, MoneyUtil::add);

        logger.fine("Totale entrate per conto " + accountId + " (EUR): " + total);
        return total;
    }

    /**
     * Raggruppa le uscite per categoria in un periodo, convertite in EUR.
     *
     * @param accountId l'ID del conto
     * @param from      la data di inizio (inclusa), null per nessun limite
     * @param to        la data di fine (inclusa), null per nessun limite
     * @return mappa con ID categoria come chiave e totale spese in EUR come valore
     */
    public Map<Integer, BigDecimal> getExpensesByCategory(int accountId, LocalDate from, LocalDate to) {
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return Map.of();
        }

        Currency currency = getCurrencyForAccount(account);

        Map<Integer, BigDecimal> result = account.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .filter(t -> isInPeriod(t.getDate(), from, to))
                .collect(Collectors.groupingBy(
                        Transaction::getCategoryId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                t -> convertToEur(t.getAmount(), currency, t.getDate()),
                                MoneyUtil::add)));

        logger.fine("Spese per categoria calcolate per conto " + accountId + " (EUR)");
        return result;
    }

    /**
     * Recupera le transazioni in un periodo specifico.
     *
     * @param accountId l'ID del conto
     * @param from      la data di inizio (inclusa), null per nessun limite inferiore
     * @param to        la data di fine (inclusa), null per nessun limite superiore
     * @return lista delle transazioni nel periodo, ordinate per data
     */
    public List<Transaction> getTransactionsByPeriod(int accountId, LocalDate from, LocalDate to) {
        List<Transaction> transactions = getTransactionsForAccount(accountId);

        List<Transaction> result = transactions.stream()
                .filter(t -> (from == null || !t.getDate().isBefore(from)) &&
                             (to == null || !t.getDate().isAfter(to)))
                .sorted(Comparator.comparing(Transaction::getDate))
                .collect(Collectors.toList());

        String fromStr = from != null ? from.toString() : "inizio";
        String toStr = to != null ? to.toString() : "fine";
        logger.fine("Transazioni nel periodo " + fromStr + " - " + toStr +
                " per conto " + accountId + ": " + result.size());
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
