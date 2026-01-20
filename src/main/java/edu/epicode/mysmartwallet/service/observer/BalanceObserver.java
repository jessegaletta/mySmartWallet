package edu.epicode.mysmartwallet.service.observer;

import edu.epicode.mysmartwallet.model.Account;
import edu.epicode.mysmartwallet.model.Transaction;

import java.math.BigDecimal;

/**
 * Interfaccia per il pattern Observer applicato alle variazioni di saldo.
 *
 * <p>Il pattern Observer definisce una dipendenza uno-a-molti tra oggetti,
 * in modo che quando un oggetto (il Subject, in questo caso Account) cambia
 * stato, tutti i suoi dipendenti (Observer) vengono notificati automaticamente.</p>
 *
 * <p>Questo pattern permette di:
 * <ul>
 *   <li>Disaccoppiare il Subject dagli Observer</li>
 *   <li>Supportare la comunicazione broadcast</li>
 *   <li>Aggiungere/rimuovere Observer a runtime</li>
 * </ul>
 * </p>
 *
 * <p>Implementazioni tipiche possono:
 * <ul>
 *   <li>Mostrare notifiche a console ({@link ConsoleBalanceObserver})</li>
 *   <li>Inviare alert via email o notifiche push</li>
 *   <li>Aggiornare interfacce grafiche</li>
 *   <li>Registrare eventi in un sistema di audit</li>
 * </ul>
 * </p>
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public interface BalanceObserver {

    /**
     * Chiamato quando il saldo di un conto cambia.
     *
     * @param account    il conto il cui saldo è cambiato
     * @param oldBalance il saldo precedente
     * @param newBalance il nuovo saldo
     */
    void onBalanceChanged(Account account, BigDecimal oldBalance, BigDecimal newBalance);

    /**
     * Chiamato quando una nuova transazione viene aggiunta a un conto.
     *
     * @param account     il conto a cui è stata aggiunta la transazione
     * @param transaction la transazione aggiunta
     */
    void onTransactionAdded(Account account, Transaction transaction);
}
