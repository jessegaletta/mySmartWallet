package edu.epicode.mysmartwallet.util;

import edu.epicode.mysmartwallet.exception.InvalidInputException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Classe utility per la validazione dell'input utente.
 * Fornisce metodi statici per verificare la correttezza dei dati inseriti.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public final class InputValidator {

    /**
     * Costruttore privato per impedire l'istanziazione.
     */
    private InputValidator() {
        throw new UnsupportedOperationException("Classe utility non istanziabile");
    }

    /**
     * Valida che l'importo sia positivo.
     *
     * @param amount l'importo da validare
     * @throws InvalidInputException se l'importo è null, zero o negativo
     */
    public static void validatePositiveAmount(BigDecimal amount) throws InvalidInputException {
        if (amount == null) {
            throw new InvalidInputException("L'importo non può essere null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("L'importo deve essere positivo: " + amount);
        }
    }

    /**
     * Valida che la stringa non sia vuota o null.
     *
     * @param value     il valore da validare
     * @param fieldName il nome del campo per il messaggio di errore
     * @throws InvalidInputException se il valore è null o vuoto
     */
    public static void validateNotEmpty(String value, String fieldName) throws InvalidInputException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException("Il campo '" + fieldName + "' non può essere vuoto");
        }
    }

    /**
     * Valida che l'oggetto non sia null.
     *
     * @param obj       l'oggetto da validare
     * @param fieldName il nome del campo per il messaggio di errore
     * @throws InvalidInputException se l'oggetto è null
     */
    public static void validateNotNull(Object obj, String fieldName) throws InvalidInputException {
        if (obj == null) {
            throw new InvalidInputException("Il campo '" + fieldName + "' non può essere null");
        }
    }

    /**
     * Valida che l'ID sia maggiore di zero.
     *
     * @param id l'ID da validare
     * @throws InvalidInputException se l'ID è minore o uguale a zero
     */
    public static void validateId(int id) throws InvalidInputException {
        if (id <= 0) {
            throw new InvalidInputException("L'ID deve essere maggiore di zero: " + id);
        }
    }

    /**
     * Valida che la data non sia futura.
     *
     * @param date la data da validare
     * @throws InvalidInputException se la data è null o futura
     */
    public static void validateDate(LocalDate date) throws InvalidInputException {
        if (date == null) {
            throw new InvalidInputException("La data non può essere null");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidInputException("La data non può essere futura: " + date);
        }
    }

    /**
     * Converte una stringa in BigDecimal.
     *
     * @param input la stringa da convertire
     * @return il BigDecimal corrispondente
     * @throws InvalidInputException se la stringa non è un numero valido
     */
    public static BigDecimal parseAmount(String input) throws InvalidInputException {
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidInputException("L'importo non può essere vuoto");
        }
        try {
            String normalized = input.trim().replace(",", ".");
            return MoneyUtil.of(normalized);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Formato importo non valido: " + input, e);
        }
    }

    /**
     * Converte una stringa in intero.
     *
     * @param input     la stringa da convertire
     * @param fieldName il nome del campo per il messaggio di errore
     * @return il valore intero
     * @throws InvalidInputException se la stringa non è un numero intero valido
     */
    public static int parseInt(String input, String fieldName) throws InvalidInputException {
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidInputException("Il campo '" + fieldName + "' non può essere vuoto");
        }
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            throw new InvalidInputException(
                    "Valore non valido per '" + fieldName + "': " + input, e);
        }
    }
}
