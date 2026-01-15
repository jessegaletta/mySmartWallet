package edu.epicode.mysmartwallet.model.category;

import edu.epicode.mysmartwallet.model.BaseEntity;
import edu.epicode.mysmartwallet.model.Transaction;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

/**
 * Classe astratta componente del Composite pattern per le categorie.
 * Definisce l'interfaccia comune per categorie foglia (StandardCategory)
 * e categorie composite (MacroCategory).
 *
 * <p>Le categorie possono essere organizzate in una struttura gerarchica
 * ad albero per classificare le transazioni finanziarie.</p>
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public abstract class CategoryComponent extends BaseEntity {

    /**
     * Nome della categoria.
     */
    protected final String name;

    /**
     * Descrizione opzionale della categoria.
     */
    protected final String description;

    /**
     * ID della categoria padre, null se è una categoria radice.
     */
    protected final Integer parentId;

    /**
     * Costruttore protetto per le sottoclassi.
     *
     * @param id          l'identificativo univoco della categoria
     * @param name        il nome della categoria
     * @param description la descrizione della categoria
     * @param parentId    l'ID della categoria padre, null se radice
     */
    protected CategoryComponent(int id, String name, String description, Integer parentId) {
        super(id);
        this.name = name;
        this.description = description;
        this.parentId = parentId;
    }

    /**
     * Restituisce il nome della categoria.
     *
     * @return il nome
     */
    public String getName() {
        return name;
    }

    /**
     * Restituisce la descrizione della categoria.
     *
     * @return la descrizione
     */
    public String getDescription() {
        return description;
    }

    /**
     * Restituisce l'ID della categoria padre.
     *
     * @return l'ID del padre, null se categoria radice
     */
    public Integer getParentId() {
        return parentId;
    }

    /**
     * Indica se questa categoria è una foglia (senza figli).
     *
     * @return true se è una foglia, false se è un composite
     */
    public abstract boolean isLeaf();

    /**
     * Stampa la struttura della categoria con l'indentazione specificata.
     * Utile per visualizzare la gerarchia delle categorie.
     *
     * @param indent la stringa di indentazione da usare
     */
    public abstract void print(String indent);

    /**
     * Restituisce un iteratore per attraversare questa categoria
     * e tutte le sue sottocategorie.
     *
     * @return un Iterator sulle CategoryComponent
     */
    public abstract Iterator<CategoryComponent> iterator();

    /**
     * Calcola il totale degli importi delle transazioni associate
     * a questa categoria e a tutte le sue sottocategorie.
     *
     * @param transactions la lista di transazioni da analizzare
     * @return il totale degli importi
     */
    public abstract BigDecimal getTotalAmount(List<Transaction> transactions);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", parentId=" + parentId +
                '}';
    }
}
