package edu.epicode.mysmartwallet.model;

import edu.epicode.mysmartwallet.exception.RateNotFoundException;
import edu.epicode.mysmartwallet.util.AppLogger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Rappresenta una valuta con il suo storico dei tassi di cambio rispetto all'EUR.
 * Utilizza un TreeMap per memorizzare i tassi storici ordinati per data.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class Currency extends BaseEntity {

    private static final Logger logger = AppLogger.getLogger(Currency.class);

    private final String code;
    private final String name;
    private final String symbol;
    private final TreeMap<LocalDate, BigDecimal> rateHistory;

    /**
     * Crea una nuova valuta.
     *
     * @param id     l'identificativo univoco
     * @param code   il codice ISO della valuta (es: EUR, USD)
     * @param name   il nome completo della valuta
     * @param symbol il simbolo della valuta (es: €, $)
     */
    public Currency(int id, String code, String name, String symbol) {
        super(id);
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.rateHistory = new TreeMap<>();
        logger.fine("Creata valuta: " + code);
    }

    /**
     * Aggiunge un tasso di cambio per una data specifica.
     *
     * @param date la data del tasso
     * @param rate il tasso di cambio rispetto all'EUR
     */
    public void addRate(LocalDate date, BigDecimal rate) {
        rateHistory.put(date, rate);
        logger.fine("Aggiunto tasso per " + code + " in data " + date + ": " + rate);
    }

    /**
     * Restituisce il tasso di cambio per una data specifica.
     * Se non esiste un tasso esatto per la data richiesta, restituisce
     * il tasso più recente disponibile prima di quella data.
     *
     * @param date la data per cui ottenere il tasso
     * @return il tasso di cambio
     * @throws RateNotFoundException se non esiste alcun tasso per la data o precedente
     */
    public BigDecimal getRateForDate(LocalDate date) throws RateNotFoundException {
        Map.Entry<LocalDate, BigDecimal> entry = rateHistory.floorEntry(date);
        if (entry == null) {
            throw new RateNotFoundException(
                    "Nessun tasso di cambio disponibile per " + code + " alla data " + date);
        }
        return entry.getValue();
    }

    /**
     * Restituisce il tasso di cambio più recente disponibile.
     *
     * @return il tasso più recente, o BigDecimal.ONE se non ci sono tassi
     */
    public BigDecimal getLatestRate() {
        if (rateHistory.isEmpty()) {
            return BigDecimal.ONE;
        }
        return rateHistory.lastEntry().getValue();
    }

    /**
     * Restituisce la data dell'ultimo tasso di cambio disponibile.
     *
     * @return la data più recente, o null se non ci sono tassi
     */
    public LocalDate getLatestRateDate() {
        if (rateHistory.isEmpty()) {
            return null;
        }
        return rateHistory.lastKey();
    }

    /**
     * Restituisce il codice ISO della valuta.
     *
     * @return il codice valuta
     */
    public String getCode() {
        return code;
    }

    /**
     * Restituisce il nome completo della valuta.
     *
     * @return il nome della valuta
     */
    public String getName() {
        return name;
    }

    /**
     * Restituisce il simbolo della valuta.
     *
     * @return il simbolo
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Restituisce lo storico dei tassi di cambio.
     *
     * @return mappa non modificabile dei tassi
     */
    public TreeMap<LocalDate, BigDecimal> getRateHistory() {
        return new TreeMap<>(rateHistory);
    }

    @Override
    public String toString() {
        return "Currency{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                '}';
    }
}
