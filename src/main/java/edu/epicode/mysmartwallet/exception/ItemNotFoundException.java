package edu.epicode.mysmartwallet.exception;

/**
 * Eccezione lanciata quando un elemento richiesto non viene trovato
 * nel repository o nel sistema.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class ItemNotFoundException extends WalletException {

    /**
     * Crea una nuova eccezione con il messaggio specificato.
     *
     * @param message il messaggio di errore
     */
    public ItemNotFoundException(String message) {
        super(message);
    }

    /**
     * Crea una nuova eccezione con il messaggio e la causa specificati.
     *
     * @param message il messaggio di errore
     * @param cause   l'eccezione che ha causato questo errore
     */
    public ItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
