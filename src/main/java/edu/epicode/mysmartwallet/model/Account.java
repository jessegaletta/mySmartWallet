package edu.epicode.mysmartwallet.model;

import edu.epicode.mysmartwallet.service.observer.BalanceObserver;
import edu.epicode.mysmartwallet.util.AppLogger;
import edu.epicode.mysmartwallet.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Rappresenta un conto finanziario con le sue transazioni.
 * Il saldo viene calcolato dinamicamente dalla somma delle transazioni.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class Account extends BaseEntity {

    private static final Logger logger = AppLogger.getLogger(Account.class);

    private final String name;
    private final int currencyId;
    private final BigDecimal initialBalance;
    private final List<Transaction> transactions;
    private final List<BalanceObserver> observers;

    /**
     * Crea un nuovo conto.
     *
     * @param id             l'identificativo univoco
     * @param name           il nome del conto
     * @param currencyId     l'ID della valuta del conto
     * @param initialBalance il saldo iniziale
     */
    public Account(int id, String name, int currencyId, BigDecimal initialBalance) {
        super(id);
        this.name = name;
        this.currencyId = currencyId;
        this.initialBalance = initialBalance;
        this.transactions = new ArrayList<>();
        this.observers = new ArrayList<>();
        logger.fine("Creato conto: " + name + " con saldo iniziale " + initialBalance);
    }

    /**
     * Aggiunge una transazione al conto e notifica gli observer.
     *
     * @param transaction la transazione da aggiungere
     */
    public void addTransaction(Transaction transaction) {
        BigDecimal oldBalance = getBalance();
        transactions.add(transaction);
        BigDecimal newBalance = getBalance();

        logger.fine("Aggiunta transazione " + transaction.getId() + " al conto " + name);

        notifyTransactionAdded(transaction);
        notifyBalanceChanged(oldBalance, newBalance);
    }

    /**
     * Registra un observer per le notifiche di variazione saldo.
     *
     * @param observer l'observer da registrare
     */
    public void addObserver(BalanceObserver observer) {
        observers.add(observer);
        logger.fine("Observer aggiunto al conto " + name);
    }

    /**
     * Rimuove un observer registrato.
     *
     * @param observer l'observer da rimuovere
     */
    public void removeObserver(BalanceObserver observer) {
        observers.remove(observer);
        logger.fine("Observer rimosso dal conto " + name);
    }

    /**
     * Notifica tutti gli observer del cambiamento di saldo.
     *
     * @param oldBalance il saldo precedente
     * @param newBalance il nuovo saldo
     */
    private void notifyBalanceChanged(BigDecimal oldBalance, BigDecimal newBalance) {
        for (BalanceObserver observer : observers) {
            observer.onBalanceChanged(this, oldBalance, newBalance);
        }
    }

    /**
     * Notifica tutti gli observer dell'aggiunta di una transazione.
     *
     * @param transaction la transazione aggiunta
     */
    private void notifyTransactionAdded(Transaction transaction) {
        for (BalanceObserver observer : observers) {
            observer.onTransactionAdded(this, transaction);
        }
    }

    /**
     * Rimuove una transazione dal conto.
     *
     * @param transactionId l'ID della transazione da rimuovere
     * @return true se la transazione è stata rimossa, false altrimenti
     */
    public boolean removeTransaction(int transactionId) {
        boolean removed = transactions.removeIf(t -> t.getId() == transactionId);
        if (removed) {
            logger.fine("Rimossa transazione " + transactionId + " dal conto " + name);
        }
        return removed;
    }

    /**
     * Calcola e restituisce il saldo attuale del conto.
     * Il saldo è calcolato come: saldo iniziale + entrate - uscite - trasferimenti.
     *
     * @return il saldo corrente
     */
    public BigDecimal getBalance() {
        BigDecimal balance = initialBalance;

        for (Transaction t : transactions) {
            switch (t.getType()) {
                case INCOME:
                    balance = MoneyUtil.add(balance, t.getAmount());
                    break;
                case EXPENSE:
                case TRANSFER:
                    balance = MoneyUtil.subtract(balance, t.getAmount());
                    break;
            }
        }

        return balance;
    }

    /**
     * Restituisce una copia difensiva della lista delle transazioni.
     *
     * @return lista delle transazioni
     */
    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    /**
     * Restituisce il nome del conto.
     *
     * @return il nome
     */
    public String getName() {
        return name;
    }

    /**
     * Restituisce l'ID della valuta del conto.
     *
     * @return l'ID della valuta
     */
    public int getCurrencyId() {
        return currencyId;
    }

    /**
     * Restituisce il saldo iniziale del conto.
     *
     * @return il saldo iniziale
     */
    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", currencyId=" + currencyId +
                ", balance=" + getBalance() +
                '}';
    }
}
