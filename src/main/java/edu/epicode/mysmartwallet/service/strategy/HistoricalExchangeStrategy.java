package edu.epicode.mysmartwallet.service.strategy;

import edu.epicode.mysmartwallet.exception.RateNotFoundException;
import edu.epicode.mysmartwallet.model.Currency;
import edu.epicode.mysmartwallet.util.AppLogger;
import edu.epicode.mysmartwallet.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.logging.Logger;

/**
 * Implementazione della strategia di conversione basata sui tassi storici.
 *
 * <p>Questa strategia utilizza i tassi di cambio memorizzati nelle entità
 * Currency per effettuare conversioni accurate basate sulla data specificata.
 * I tassi sono tutti espressi rispetto all'EUR come valuta base.</p>
 *
 * <p>Formula di conversione: amount * (tassoFrom / tassoTo)</p>
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class HistoricalExchangeStrategy implements ExchangeStrategy {

    private static final Logger logger = AppLogger.getLogger(HistoricalExchangeStrategy.class);

    /**
     * Costruttore di default.
     */
    public HistoricalExchangeStrategy() {
        logger.fine("HistoricalExchangeStrategy inizializzata");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Se le due valute sono uguali (stesso codice), restituisce l'importo
     * originale senza effettuare conversioni.</p>
     *
     * <p>La conversione avviene tramite l'EUR come valuta pivot:
     * <ol>
     *   <li>Recupera il tasso della valuta di origine rispetto all'EUR</li>
     *   <li>Recupera il tasso della valuta di destinazione rispetto all'EUR</li>
     *   <li>Calcola: importo * (tassoOrigine / tassoDestinazione)</li>
     * </ol>
     * </p>
     */
    @Override
    public BigDecimal convert(BigDecimal amount, Currency from, Currency to, LocalDate date)
            throws RateNotFoundException {

        // Se le valute sono uguali, nessuna conversione necessaria
        if (from.getCode().equals(to.getCode())) {
            logger.fine("Conversione non necessaria: stessa valuta " + from.getCode());
            return amount;
        }

        // Recupera i tassi rispetto all'EUR
        BigDecimal rateFrom = from.getRateForDate(date);
        BigDecimal rateTo = to.getRateForDate(date);

        // Calcola la conversione: amount * (rateFrom / rateTo)
        BigDecimal conversionRate = MoneyUtil.divide(rateFrom, rateTo);
        BigDecimal result = MoneyUtil.multiply(amount, conversionRate);

        logger.fine(String.format("Conversione: %s %s -> %s %s (tasso: %s)",
                amount, from.getCode(), result, to.getCode(), conversionRate));

        return result;
    }
}
