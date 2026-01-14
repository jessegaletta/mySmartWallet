package edu.epicode.mysmartwallet.model;

import edu.epicode.mysmartwallet.exception.InvalidInputException;
import edu.epicode.mysmartwallet.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Rappresenta una transazione finanziaria immutabile.
 * Le istanze possono essere create solo tramite il Builder interno.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public final class Transaction extends BaseEntity {

    private final LocalDate date;
    private final String description;
    private final BigDecimal amount;
    private final TransactionType type;
    private final int categoryId;
    private final int accountId;

    /**
     * Costruttore privato. Usare {@link Builder} per creare istanze.
     */
    private Transaction(Builder builder) {
        super(builder.id);
        this.date = builder.date;
        this.description = builder.description;
        this.amount = builder.amount;
        this.type = builder.type;
        this.categoryId = builder.categoryId;
        this.accountId = builder.accountId;
    }

    /**
     * Restituisce la data della transazione.
     *
     * @return la data
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Restituisce la descrizione della transazione.
     *
     * @return la descrizione
     */
    public String getDescription() {
        return description;
    }

    /**
     * Restituisce l'importo della transazione.
     *
     * @return l'importo
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Restituisce il tipo di transazione.
     *
     * @return il tipo
     */
    public TransactionType getType() {
        return type;
    }

    /**
     * Restituisce l'ID della categoria associata.
     *
     * @return l'ID della categoria
     */
    public int getCategoryId() {
        return categoryId;
    }

    /**
     * Restituisce l'ID del conto associato.
     *
     * @return l'ID del conto
     */
    public int getAccountId() {
        return accountId;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", date=" + date +
                ", type=" + type.getDescription() +
                ", amount=" + MoneyUtil.format(amount, "") +
                ", description='" + description + '\'' +
                ", categoryId=" + categoryId +
                ", accountId=" + accountId +
                '}';
    }

    /**
     * Builder per la creazione di istanze Transaction.
     * Permette la costruzione fluente di transazioni immutabili.
     */
    public static class Builder {

        private int id;
        private LocalDate date;
        private String description;
        private BigDecimal amount;
        private TransactionType type;
        private int categoryId;
        private int accountId;

        /**
         * Crea un nuovo Builder.
         */
        public Builder() {
            // Valori di default
        }

        /**
         * Imposta l'ID della transazione.
         *
         * @param id l'identificativo univoco
         * @return questo Builder
         */
        public Builder withId(int id) {
            this.id = id;
            return this;
        }

        /**
         * Imposta la data della transazione.
         *
         * @param date la data
         * @return questo Builder
         */
        public Builder withDate(LocalDate date) {
            this.date = date;
            return this;
        }

        /**
         * Imposta la descrizione della transazione.
         *
         * @param description la descrizione
         * @return questo Builder
         */
        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Imposta l'importo della transazione.
         *
         * @param amount l'importo
         * @return questo Builder
         */
        public Builder withAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Imposta il tipo di transazione.
         *
         * @param type il tipo
         * @return questo Builder
         */
        public Builder withType(TransactionType type) {
            this.type = type;
            return this;
        }

        /**
         * Imposta l'ID della categoria.
         *
         * @param categoryId l'ID della categoria
         * @return questo Builder
         */
        public Builder withCategoryId(int categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        /**
         * Imposta l'ID del conto.
         *
         * @param accountId l'ID del conto
         * @return questo Builder
         */
        public Builder withAccountId(int accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Costruisce la transazione dopo aver validato i dati.
         *
         * @return la nuova Transaction
         * @throws InvalidInputException se i dati non sono validi
         */
        public Transaction build() throws InvalidInputException {
            validate();
            return new Transaction(this);
        }

        /**
         * Valida i dati del builder.
         *
         * @throws InvalidInputException se la validazione fallisce
         */
        private void validate() throws InvalidInputException {
            if (date == null) {
                throw new InvalidInputException("La data della transazione è obbligatoria");
            }
            if (type == null) {
                throw new InvalidInputException("Il tipo di transazione è obbligatorio");
            }
            if (amount == null || !MoneyUtil.isPositive(amount)) {
                throw new InvalidInputException("L'importo deve essere positivo");
            }
        }
    }
}
