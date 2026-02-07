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
     * Trasferimento in uscita: denaro trasferito dal conto.
     */
    TRANSFER_OUT("Trasferimento in uscita"),

    /**
     * Trasferimento in entrata: denaro ricevuto da un altro conto.
     */
    TRANSFER_IN("Trasferimento in entrata");

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
