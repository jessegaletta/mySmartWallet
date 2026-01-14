package edu.epicode.mysmartwallet.model;

/**
 * Enum che rappresenta i tipi di transazione supportati dal sistema.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public enum TransactionType {

    /**
     * Entrata: denaro ricevuto sul conto.
     */
    INCOME("Entrata"),

    /**
     * Uscita: denaro speso dal conto.
     */
    EXPENSE("Uscita"),

    /**
     * Trasferimento: movimento tra conti.
     */
    TRANSFER("Trasferimento");

    private final String description;

    /**
     * Costruttore privato per l'enum.
     *
     * @param description la descrizione in italiano del tipo
     */
    TransactionType(String description) {
        this.description = description;
    }

    /**
     * Restituisce la descrizione in italiano del tipo di transazione.
     *
     * @return la descrizione localizzata
     */
    public String getDescription() {
        return description;
    }
}
