package edu.epicode.mysmartwallet.repository;

import edu.epicode.mysmartwallet.exception.ItemNotFoundException;
import edu.epicode.mysmartwallet.model.BaseEntity;
import edu.epicode.mysmartwallet.util.AppLogger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Implementazione in memoria del repository generico.
 * Utilizza una LinkedHashMap per mantenere l'ordine di inserimento delle entità
 * e un AtomicInteger per la generazione thread-safe degli identificativi.
 *
 * @param <T> il tipo di entità gestita, deve estendere {@link BaseEntity}
 * @author Jesse Galetta
 * @version 1.0
 */
public class InMemoryRepository<T extends BaseEntity> implements Repository<T> {

    private static final Logger logger = AppLogger.getLogger(InMemoryRepository.class);

    /**
     * Storage delle entità indicizzate per ID.
     * LinkedHashMap mantiene l'ordine di inserimento.
     */
    private final Map<Integer, T> storage;

    /**
     * Contatore atomico per la generazione di ID univoci.
     */
    private final AtomicInteger idCounter;

    /**
     * Costruttore che inizializza lo storage vuoto e il contatore ID a 1.
     */
    public InMemoryRepository() {
        this.storage = new LinkedHashMap<>();
        this.idCounter = new AtomicInteger(1);
        logger.fine("Repository in memoria inizializzato");
    }

    /**
     * {@inheritDoc}
     * Aggiorna automaticamente il contatore degli ID per evitare collisioni
     * quando vengono caricate entità con ID già assegnati (es. da file CSV).
     */
    @Override
    public void save(T entity) {
        storage.put(entity.getId(), entity);
        // Aggiorna il contatore per evitare collisioni con ID già esistenti
        int entityId = entity.getId();
        idCounter.updateAndGet(current -> Math.max(current, entityId + 1));
        logger.fine("Entità salvata con ID: " + entity.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<T> findById(int id) {
        T entity = storage.get(id);
        if (entity != null) {
            logger.fine("Entità trovata con ID: " + id);
        } else {
            logger.fine("Nessuna entità trovata con ID: " + id);
        }
        return Optional.ofNullable(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> findAll() {
        List<T> result = new ArrayList<>(storage.values());
        logger.fine("Restituite " + result.size() + " entità");
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(int id) throws ItemNotFoundException {
        if (!storage.containsKey(id)) {
            logger.warning("Tentativo di eliminazione entità inesistente con ID: " + id);
            throw new ItemNotFoundException("Entità con ID " + id + " non trovata");
        }
        storage.remove(id);
        logger.fine("Entità eliminata con ID: " + id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(int id) {
        return storage.containsKey(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int count() {
        return storage.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        int previousCount = storage.size();
        storage.clear();
        logger.fine("Repository svuotato, rimosse " + previousCount + " entità");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int generateNextId() {
        int nextId = idCounter.getAndIncrement();
        logger.fine("Generato nuovo ID: " + nextId);
        return nextId;
    }
}
