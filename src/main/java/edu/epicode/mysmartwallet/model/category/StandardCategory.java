package edu.epicode.mysmartwallet.model.category;

import edu.epicode.mysmartwallet.model.Transaction;
import edu.epicode.mysmartwallet.util.AppLogger;
import edu.epicode.mysmartwallet.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Categoria standard (foglia) nel Composite pattern.
 * Rappresenta una categoria terminale senza sottocategorie.
 *
 * <p>Le StandardCategory sono le foglie dell'albero delle categorie
 * e possono essere assegnate direttamente alle transazioni.</p>
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class StandardCategory extends CategoryComponent {

    private static final Logger logger = AppLogger.getLogger(StandardCategory.class);

    /**
     * Crea una nuova categoria standard.
     *
     * @param id          l'identificativo univoco
     * @param name        il nome della categoria
     * @param description la descrizione della categoria
     * @param parentId    l'ID della categoria padre, null se radice
     */
    public StandardCategory(int id, String name, String description, Integer parentId) {
        super(id, name, description, parentId);
        logger.fine("Creata StandardCategory: " + name + " (id=" + id + ")");
    }

    /**
     * Indica che questa è una categoria foglia.
     *
     * @return sempre true per StandardCategory
     */
    @Override
    public boolean isLeaf() {
        return true;
    }

    /**
     * Stampa questa categoria con l'indentazione specificata.
     *
     * @param indent la stringa di indentazione
     */
    @Override
    public void print(String indent) {
        System.out.println(indent + "- " + name + " [" + id + "]");
    }

    /**
     * Restituisce un iteratore che contiene solo questa categoria.
     *
     * @return un Iterator con solo questa istanza
     */
    @Override
    public Iterator<CategoryComponent> iterator() {
        return Collections.singletonList((CategoryComponent) this).iterator();
    }

    /**
     * Calcola il totale delle transazioni associate a questa categoria.
     *
     * @param transactions la lista di transazioni da analizzare
     * @return la somma degli importi delle transazioni con categoryId uguale a questo id
     */
    @Override
    public BigDecimal getTotalAmount(List<Transaction> transactions) {
        BigDecimal total = MoneyUtil.ZERO;
        for (Transaction transaction : transactions) {
            if (transaction.getCategoryId() == this.id) {
                total = MoneyUtil.add(total, transaction.getAmount());
            }
        }
        logger.fine("Totale calcolato per categoria " + name + ": " + total);
        return total;
    }
}
