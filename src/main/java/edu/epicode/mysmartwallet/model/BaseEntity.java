package edu.epicode.mysmartwallet.model;

import java.util.Objects;

/**
 * Classe base astratta per tutte le entità del sistema.
 * Fornisce gestione dell'identificativo univoco e implementazioni
 * standard di equals, hashCode e toString.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public abstract class BaseEntity {

    /**
     * Identificativo univoco dell'entità.
     */
    protected int id;

    /**
     * Costruttore protetto per le sottoclassi.
     *
     * @param id l'identificativo univoco dell'entità
     */
    protected BaseEntity(int id) {
        this.id = id;
    }

    /**
     * Restituisce l'identificativo dell'entità.
     *
     * @return l'identificativo univoco
     */
    public int getId() {
        return id;
    }

    /**
     * Confronta questa entità con un'altra basandosi sull'ID.
     *
     * @param obj l'oggetto da confrontare
     * @return true se gli oggetti hanno lo stesso ID e tipo
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BaseEntity that = (BaseEntity) obj;
        return id == that.id;
    }

    /**
     * Calcola l'hash code basato sull'ID.
     *
     * @return l'hash code dell'entità
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Restituisce una rappresentazione testuale dell'entità.
     *
     * @return stringa con nome classe e ID
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + "}";
    }
}
