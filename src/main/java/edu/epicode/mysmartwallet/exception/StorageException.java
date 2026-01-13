package edu.epicode.mysmartwallet.exception;

/**
 * Eccezione lanciata quando si verifica un errore durante le operazioni
 * di lettura o scrittura su file.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class StorageException extends WalletException {

    /**
     * Crea una nuova eccezione con il messaggio specificato.
     *
     * @param message il messaggio di errore
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Crea una nuova eccezione con il messaggio e la causa specificati.
     *
     * @param message il messaggio di errore
     * @param cause   l'eccezione che ha causato questo errore
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
