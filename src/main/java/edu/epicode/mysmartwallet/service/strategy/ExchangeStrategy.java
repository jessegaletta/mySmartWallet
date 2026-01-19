package edu.epicode.mysmartwallet.service.strategy;

import edu.epicode.mysmartwallet.exception.RateNotFoundException;
import edu.epicode.mysmartwallet.model.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Interfaccia per il pattern Strategy applicato alla conversione valutaria.
 *
 * <p>Il pattern Strategy permette di definire una famiglia di algoritmi
 * di conversione intercambiabili. Questo consente di variare l'algoritmo
 * indipendentemente dal client che lo utilizza.</p>
 *
 * <p>Implementazioni tipiche includono:
 * <ul>
 *   <li>{@link HistoricalExchangeStrategy} - usa tassi storici dalle valute</li>
 *   <li>{@link FixedExchangeStrategy} - usa un tasso fisso per testing</li>
 * </ul>
 * </p>
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public interface ExchangeStrategy {

    /**
     * Converte un importo da una valuta ad un'altra.
     *
     * @param amount l'importo da convertire
     * @param from   la valuta di origine
     * @param to     la valuta di destinazione
     * @param date   la data per cui utilizzare il tasso di cambio
     * @return l'importo convertito nella valuta di destinazione
     * @throws RateNotFoundException se non è disponibile un tasso di cambio
     *                               per una delle valute alla data specificata
     */
    BigDecimal convert(BigDecimal amount, Currency from, Currency to, LocalDate date)
            throws RateNotFoundException;
}
