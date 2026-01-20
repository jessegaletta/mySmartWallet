package edu.epicode.mysmartwallet.service.observer;

import edu.epicode.mysmartwallet.model.Account;
import edu.epicode.mysmartwallet.model.Transaction;
import edu.epicode.mysmartwallet.util.AppLogger;
import edu.epicode.mysmartwallet.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.logging.Logger;

/**
 * Implementazione dell'Observer che notifica le variazioni di saldo sulla console.
 *
 * <p>Questa classe implementa il pattern Observer per mostrare messaggi
 * informativi e di warning quando il saldo di un conto cambia.</p>
 *
 * <p>Funzionalità:
 * <ul>
 *   <li>Stampa a console quando il saldo viene aggiornato</li>
 *   <li>Mostra un warning se il saldo diventa negativo</li>
 *   <li>Notifica quando viene aggiunta una nuova transazione</li>
 * </ul>
 * </p>
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class ConsoleBalanceObserver implements BalanceObserver {

    private static final Logger logger = AppLogger.getLogger(ConsoleBalanceObserver.class);

    /**
     * Costruttore di default.
     */
    public ConsoleBalanceObserver() {
        logger.fine("ConsoleBalanceObserver inizializzato");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stampa a console il cambiamento di saldo e genera un warning
     * se il nuovo saldo è negativo.</p>
     */
    @Override
    public void onBalanceChanged(Account account, BigDecimal oldBalance, BigDecimal newBalance) {
        String oldFormatted = MoneyUtil.format(oldBalance, "");
        String newFormatted = MoneyUtil.format(newBalance, "");

        System.out.println("Conto [" + account.getName() + "]: saldo aggiornato da " +
                oldFormatted + " a " + newFormatted);

        logger.fine("Notifica saldo: " + account.getName() + " " + oldFormatted + " -> " + newFormatted);

        if (MoneyUtil.isNegative(newBalance)) {
            System.out.println("ATTENZIONE: Il saldo del conto [" + account.getName() +
                    "] è negativo: " + newFormatted);
            logger.warning("Saldo negativo sul conto: " + account.getName());
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stampa a console i dettagli della nuova transazione.</p>
     */
    @Override
    public void onTransactionAdded(Account account, Transaction transaction) {
        String amountFormatted = MoneyUtil.format(transaction.getAmount(), "");

        System.out.println("Nuova transazione su [" + account.getName() + "]: " +
                transaction.getType().getDescription() + " " + amountFormatted +
                " - " + transaction.getDescription());

        logger.fine("Notifica transazione: " + transaction.getId() +
                " su conto " + account.getName());
    }
}
