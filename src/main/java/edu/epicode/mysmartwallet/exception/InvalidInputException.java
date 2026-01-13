package edu.epicode.mysmartwallet.exception;

/**
 * Eccezione lanciata quando l'input fornito dall'utente non è valido.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class InvalidInputException extends WalletException {

    /**
     * Crea una nuova eccezione con il messaggio specificato.
     *
     * @param message il messaggio di errore
     */
    public InvalidInputException(String message) {
        super(message);
    }

    /**
     * Crea una nuova eccezione con il messaggio e la causa specificati.
     *
     * @param message il messaggio di errore
     * @param cause   l'eccezione che ha causato questo errore
     */
    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
