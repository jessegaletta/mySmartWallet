package edu.epicode.mysmartwallet.repository;

import edu.epicode.mysmartwallet.exception.ItemNotFoundException;
import edu.epicode.mysmartwallet.model.BaseEntity;

import java.util.List;
import java.util.Optional;

/**
 * Interfaccia generica per le operazioni CRUD sui repository.
 * Definisce il contratto base per la persistenza delle entità nel sistema.
 * Utilizza Generics per garantire type-safety sulle operazioni.
 *
 * @param <T> il tipo di entità gestita, deve estendere {@link BaseEntity}
 * @author Jesse Galetta
 * @version 1.0
 */
public interface Repository<T extends BaseEntity> {

    /**
     * Salva un'entità nel repository.
     * Se l'entità esiste già (stesso ID), viene sovrascritta.
     *
     * @param entity l'entità da salvare
     */
    void save(T entity);

    /**
     * Cerca un'entità per il suo identificativo.
     *
     * @param id l'identificativo dell'entità da cercare
     * @return un Optional contenente l'entità se trovata, altrimenti Optional vuoto
     */
    Optional<T> findById(int id);

    /**
     * Restituisce tutte le entità presenti nel repository.
     *
     * @return una lista di tutte le entità, può essere vuota ma mai null
     */
    List<T> findAll();

    /**
     * Elimina un'entità dal repository dato il suo identificativo.
     *
     * @param id l'identificativo dell'entità da eliminare
     * @throws ItemNotFoundException se l'entità con l'ID specificato non esiste
     */
    void delete(int id) throws ItemNotFoundException;

    /**
     * Verifica se esiste un'entità con l'identificativo specificato.
     *
     * @param id l'identificativo da verificare
     * @return true se l'entità esiste, false altrimenti
     */
    boolean exists(int id);

    /**
     * Restituisce il numero totale di entità nel repository.
     *
     * @return il conteggio delle entità
     */
    int count();

    /**
     * Rimuove tutte le entità dal repository.
     */
    void clear();

    /**
     * Genera un nuovo identificativo univoco per una nuova entità.
     *
     * @return il prossimo ID disponibile
     */
    int generateNextId();
}
