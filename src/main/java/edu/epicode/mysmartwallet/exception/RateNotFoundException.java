package edu.epicode.mysmartwallet.exception;

/**
 * Eccezione lanciata quando un tasso di cambio richiesto non viene trovato.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class RateNotFoundException extends WalletException {

    /**
     * Crea una nuova eccezione con il messaggio specificato.
     *
     * @param message il messaggio di errore
     */
    public RateNotFoundException(String message) {
        super(message);
    }

    /**
     * Crea una nuova eccezione con il messaggio e la causa specificati.
     *
     * @param message il messaggio di errore
     * @param cause   l'eccezione che ha causato questo errore
     */
    public RateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
