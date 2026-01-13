package edu.epicode.mysmartwallet.exception;

/**
 * Eccezione lanciata quando un conto non ha fondi sufficienti
 * per completare un'operazione.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class InsufficientFundsException extends WalletException {

    /**
     * Crea una nuova eccezione con il messaggio specificato.
     *
     * @param message il messaggio di errore
     */
    public InsufficientFundsException(String message) {
        super(message);
    }

    /**
     * Crea una nuova eccezione con il messaggio e la causa specificati.
     *
     * @param message il messaggio di errore
     * @param cause   l'eccezione che ha causato questo errore
     */
    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
}
