package edu.epicode.mysmartwallet.service.factory;

import edu.epicode.mysmartwallet.exception.InvalidInputException;
import edu.epicode.mysmartwallet.model.Transaction;
import edu.epicode.mysmartwallet.model.TransactionType;
import edu.epicode.mysmartwallet.util.AppLogger;
import edu.epicode.mysmartwallet.util.InputValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Factory per la creazione di transazioni finanziarie.
 *
 * Implementa il pattern Factory Method per centralizzare la logica
 * di creazione delle transazioni, garantendo validazione degli input
 * e generazione automatica degli ID.
 *
 * Questa classe fornisce metodi statici per creare:
 * <ul>
 *   <li>Entrate (INCOME)</li>
 *   <li>Uscite (EXPENSE)</li>
 *   <li>Trasferimenti tra conti (coppia TRANSFER_OUT/TRANSFER_IN)</li>
 * </ul>
 * 
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public final class TransactionFactory {

    private static final Logger logger = AppLogger.getLogger(TransactionFactory.class);

    /**
     * Contatore atomico per la generazione di ID univoci.
     */
    private static final AtomicInteger idCounter = new AtomicInteger(1);

    /**
     * Costruttore privato per impedire l'istanziazione.
     */
    private TransactionFactory() {
        throw new UnsupportedOperationException("Classe factory non istanziabile");
    }

    /**
     * Crea una transazione di tipo INCOME (entrata) con supporto per conversione valutaria.
     *
     * @param amount             l'importo dell'entrata (nella valuta del conto)
     * @param description        la descrizione della transazione
     * @param categoryId         l'ID della categoria
     * @param accountId          l'ID del conto
     * @param date               la data della transazione
     * @param originalAmount     l'importo originale (null se nessuna conversione)
     * @param originalCurrencyId l'ID della valuta originale (null se nessuna conversione)
     * @param exchangeRate       il tasso di cambio usato (null se nessuna conversione)
     * @return la transazione creata
     * @throws InvalidInputException se i parametri non sono validi
     */
    public static Transaction createIncome(BigDecimal amount, String description,
            int categoryId, int accountId, LocalDate date,
            BigDecimal originalAmount, Integer originalCurrencyId, BigDecimal exchangeRate)
            throws InvalidInputException {

        validateInputs(amount, description, categoryId, accountId, date);

        Transaction.Builder builder = new Transaction.Builder()
                .withId(idCounter.getAndIncrement())
                .withAmount(amount)
                .withDescription(description)
                .withType(TransactionType.INCOME)
                .withCategoryId(categoryId)
                .withAccountId(accountId)
                .withDate(date);

        if (originalAmount != null) {
            builder.withOriginalAmount(originalAmount);
        }
        if (originalCurrencyId != null) {
            builder.withOriginalCurrencyId(originalCurrencyId);
        }
        if (exchangeRate != null) {
            builder.withExchangeRate(exchangeRate);
        }

        Transaction transaction = builder.build();

        logger.info("Creata transazione INCOME: " + transaction.getId() + " - " + amount);
        return transaction;
    }

    /**
     * Crea una transazione di tipo EXPENSE (uscita) con supporto per conversione valutaria.
     *
     * @param amount             l'importo dell'uscita (nella valuta del conto)
     * @param description        la descrizione della transazione
     * @param categoryId         l'ID della categoria
     * @param accountId          l'ID del conto
     * @param date               la data della transazione
     * @param originalAmount     l'importo originale (null se nessuna conversione)
     * @param originalCurrencyId l'ID della valuta originale (null se nessuna conversione)
     * @param exchangeRate       il tasso di cambio usato (null se nessuna conversione)
     * @return la transazione creata
     * @throws InvalidInputException se i parametri non sono validi
     */
    public static Transaction createExpense(BigDecimal amount, String description,
            int categoryId, int accountId, LocalDate date,
            BigDecimal originalAmount, Integer originalCurrencyId, BigDecimal exchangeRate)
            throws InvalidInputException {

        validateInputs(amount, description, categoryId, accountId, date);

        Transaction.Builder builder = new Transaction.Builder()
                .withId(idCounter.getAndIncrement())
                .withAmount(amount)
                .withDescription(description)
                .withType(TransactionType.EXPENSE)
                .withCategoryId(categoryId)
                .withAccountId(accountId)
                .withDate(date);

        if (originalAmount != null) {
            builder.withOriginalAmount(originalAmount);
        }
        if (originalCurrencyId != null) {
            builder.withOriginalCurrencyId(originalCurrencyId);
        }
        if (exchangeRate != null) {
            builder.withExchangeRate(exchangeRate);
        }

        Transaction transaction = builder.build();

        logger.info("Creata transazione EXPENSE: " + transaction.getId() + " - " + amount);
        return transaction;
    }

    /**
     * Crea un trasferimento tra due conti.
     *
     * Un trasferimento genera due transazioni:
     * <ol>
     *   <li>Una transazione TRANSFER_OUT sul conto sorgente</li>
     *   <li>Una transazione TRANSFER_IN sul conto destinazione</li>
     * </ol>
     *
     * Supporta la conversione valutaria: se toAmount e' diverso da fromAmount,
     * i dati di conversione vengono registrati nella transazione di entrata.
     *
     * @param fromAmount         l'importo in uscita (nella valuta del conto sorgente)
     * @param toAmount           l'importo in entrata (nella valuta del conto destinazione)
     * @param description        la descrizione del trasferimento
     * @param fromAccountId      l'ID del conto sorgente
     * @param toAccountId        l'ID del conto destinazione
     * @param categoryId         l'ID della categoria
     * @param date               la data del trasferimento
     * @param originalAmount     l'importo originale per conversione (null se stessa valuta)
     * @param originalCurrencyId l'ID della valuta originale (null se stessa valuta)
     * @param exchangeRate       il tasso di cambio applicato (null se stessa valuta)
     * @return lista contenente le due transazioni (TRANSFER_OUT e TRANSFER_IN)
     * @throws InvalidInputException se i parametri non sono validi
     */
    public static List<Transaction> createTransfer(BigDecimal fromAmount, BigDecimal toAmount,
            String description, int fromAccountId, int toAccountId, int categoryId, LocalDate date,
            BigDecimal originalAmount, Integer originalCurrencyId, BigDecimal exchangeRate)
            throws InvalidInputException {

        InputValidator.validatePositiveAmount(fromAmount);
        InputValidator.validatePositiveAmount(toAmount);
        InputValidator.validateNotEmpty(description, "descrizione");
        InputValidator.validateId(fromAccountId);
        InputValidator.validateId(toAccountId);
        InputValidator.validateId(categoryId);
        InputValidator.validateNotNull(date, "data");

        if (fromAccountId == toAccountId) {
            throw new InvalidInputException(
                    "Il conto sorgente e destinazione non possono essere uguali");
        }

        List<Transaction> transfers = new ArrayList<>();

        // Transazione di uscita dal conto sorgente (TRANSFER_OUT)
        Transaction outgoing = new Transaction.Builder()
                .withId(idCounter.getAndIncrement())
                .withAmount(fromAmount)
                .withDescription(description + " (trasferimento in uscita)")
                .withType(TransactionType.TRANSFER_OUT)
                .withCategoryId(categoryId)
                .withAccountId(fromAccountId)
                .withDate(date)
                .build();
        transfers.add(outgoing);

        // Transazione di entrata nel conto destinazione (TRANSFER_IN)
        Transaction.Builder incomingBuilder = new Transaction.Builder()
                .withId(idCounter.getAndIncrement())
                .withAmount(toAmount)
                .withDescription(description + " (trasferimento in entrata)")
                .withType(TransactionType.TRANSFER_IN)
                .withCategoryId(categoryId)
                .withAccountId(toAccountId)
                .withDate(date);

        // Aggiungi dati di conversione se presenti
        if (originalAmount != null) {
            incomingBuilder.withOriginalAmount(originalAmount);
        }
        if (originalCurrencyId != null) {
            incomingBuilder.withOriginalCurrencyId(originalCurrencyId);
        }
        if (exchangeRate != null) {
            incomingBuilder.withExchangeRate(exchangeRate);
        }

        transfers.add(incomingBuilder.build());

        logger.info("Creato trasferimento: " + fromAmount + " da conto " + fromAccountId +
                " -> " + toAmount + " a conto " + toAccountId);

        return transfers;
    }

    /**
     * Resetta il contatore degli ID al valore iniziale.
     * Utile per i test unitari.
     */
    public static void resetIdCounter() {
        idCounter.set(1);
        logger.fine("Contatore ID resettato");
    }

    /**
     * Valida i parametri comuni a tutte le transazioni.
     */
    private static void validateInputs(BigDecimal amount, String description,
            int categoryId, int accountId, LocalDate date) throws InvalidInputException {
        InputValidator.validateNotEmpty(description, "descrizione");
        InputValidator.validateId(categoryId);
        InputValidator.validateId(accountId);
        InputValidator.validateNotNull(date, "data");
    }
}
