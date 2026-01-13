package edu.epicode.mysmartwallet.exception;

/**
 * Eccezione base per tutte le eccezioni dell'applicazione MySmartWallet.
 * Fornisce una gerarchia uniforme per la gestione degli errori.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class WalletException extends Exception {

    /**
     * Crea una nuova eccezione con il messaggio specificato.
     *
     * @param message il messaggio di errore
     */
    public WalletException(String message) {
        super(message);
    }

    /**
     * Crea una nuova eccezione con il messaggio e la causa specificati.
     *
     * @param message il messaggio di errore
     * @param cause   l'eccezione che ha causato questo errore
     */
    public WalletException(String message, Throwable cause) {
        super(message, cause);
    }
}
