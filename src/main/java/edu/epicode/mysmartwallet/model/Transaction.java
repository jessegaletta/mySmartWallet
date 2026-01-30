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
    private final BigDecimal originalAmount;
    private final Integer originalCurrencyId;
    private final BigDecimal exchangeRate;

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
        this.originalAmount = builder.originalAmount;
        this.originalCurrencyId = builder.originalCurrencyId;
        this.exchangeRate = builder.exchangeRate;
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

    /**
     * Restituisce l'importo originale nella valuta di inserimento.
     * Se null, non c'e' stata conversione valutaria.
     *
     * @return l'importo originale o null
     */
    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    /**
     * Restituisce l'ID della valuta originale della transazione.
     * Se null, la valuta e' la stessa del conto associato.
     *
     * @return l'ID della valuta originale o null
     */
    public Integer getOriginalCurrencyId() {
        return originalCurrencyId;
    }

    /**
     * Restituisce il tasso di cambio applicato per la conversione.
     * Se null, non e' stata effettuata conversione.
     *
     * @return il tasso di cambio o null
     */
    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    /**
     * Indica se la transazione ha subito una conversione valutaria.
     *
     * @return true se c'e' stata conversione
     */
    public boolean hasConversion() {
        return originalCurrencyId != null && exchangeRate != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Transaction{");
        sb.append("id=").append(id);
        sb.append(", date=").append(date);
        sb.append(", type=").append(type.getDescription());
        sb.append(", amount=").append(MoneyUtil.format(amount, ""));
        if (hasConversion()) {
            sb.append(", originalAmount=").append(MoneyUtil.format(originalAmount, ""));
            sb.append(", originalCurrencyId=").append(originalCurrencyId);
            sb.append(", exchangeRate=").append(exchangeRate.toPlainString());
        }
        sb.append(", description='").append(description).append('\'');
        sb.append(", categoryId=").append(categoryId);
        sb.append(", accountId=").append(accountId);
        sb.append('}');
        return sb.toString();
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
        private BigDecimal originalAmount;
        private Integer originalCurrencyId;
        private BigDecimal exchangeRate;

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
         * Imposta l'importo originale prima della conversione valutaria.
         *
         * @param originalAmount l'importo nella valuta originale
         * @return questo Builder
         */
        public Builder withOriginalAmount(BigDecimal originalAmount) {
            this.originalAmount = originalAmount;
            return this;
        }

        /**
         * Imposta l'ID della valuta originale.
         *
         * @param originalCurrencyId l'ID della valuta
         * @return questo Builder
         */
        public Builder withOriginalCurrencyId(Integer originalCurrencyId) {
            this.originalCurrencyId = originalCurrencyId;
            return this;
        }

        /**
         * Imposta il tasso di cambio utilizzato per la conversione.
         *
         * @param exchangeRate il tasso di cambio
         * @return questo Builder
         */
        public Builder withExchangeRate(BigDecimal exchangeRate) {
            this.exchangeRate = exchangeRate;
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
