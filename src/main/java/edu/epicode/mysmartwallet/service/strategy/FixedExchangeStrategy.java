package edu.epicode.mysmartwallet.service.strategy;

import edu.epicode.mysmartwallet.model.Currency;
import edu.epicode.mysmartwallet.util.AppLogger;
import edu.epicode.mysmartwallet.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.logging.Logger;

/**
 * Implementazione della strategia di conversione con tasso fisso.
 *
 * Questa strategia ignora i tassi storici e utilizza sempre un tasso
 * di cambio predefinito. È particolarmente utile per:
 * <ul>
 *   <li>Test unitari deterministici</li>
 *   <li>Simulazioni con tassi controllati</li>
 *   <li>Scenari di parità valutaria (tasso 1.0)</li>
 * </ul>
 * 
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class FixedExchangeStrategy implements ExchangeStrategy {

    private static final Logger logger = AppLogger.getLogger(FixedExchangeStrategy.class);

    /**
     * Tasso di cambio fisso utilizzato per tutte le conversioni.
     */
    private final BigDecimal fixedRate;

    /**
     * Crea una strategia con tasso di parità (1.0).
     * Utile per test dove le valute devono essere equivalenti.
     */
    public FixedExchangeStrategy() {
        this(BigDecimal.ONE);
        logger.fine("FixedExchangeStrategy creata con tasso di parità (1.0)");
    }

    /**
     * Crea una strategia con il tasso fisso specificato.
     *
     * @param fixedRate il tasso di cambio da applicare a tutte le conversioni
     */
    public FixedExchangeStrategy(BigDecimal fixedRate) {
        this.fixedRate = fixedRate;
        logger.fine("FixedExchangeStrategy creata con tasso: " + fixedRate);
    }

    /**
     * {@inheritDoc}
     *
     * Questa implementazione ignora completamente i tassi storici
     * delle valute e la data specificata, applicando sempre il tasso fisso
     * configurato nel costruttore.
     *
     * Se le due valute sono uguali (stesso codice), restituisce l'importo
     * originale senza applicare il tasso.
     */
    @Override
    public BigDecimal convert(BigDecimal amount, Currency from, Currency to, LocalDate date) {
        // Se le valute sono uguali, nessuna conversione necessaria
        if (from.getCode().equals(to.getCode())) {
            logger.fine("Conversione non necessaria: stessa valuta " + from.getCode());
            return amount;
        }

        BigDecimal result = MoneyUtil.multiply(amount, fixedRate);

        logger.fine(String.format("Conversione fissa: %s %s -> %s %s (tasso fisso: %s)",
                amount, from.getCode(), result, to.getCode(), fixedRate));

        return result;
    }

    /**
     * Restituisce il tasso fisso configurato.
     *
     * @return il tasso di cambio fisso
     */
    public BigDecimal getFixedRate() {
        return fixedRate;
    }
}
