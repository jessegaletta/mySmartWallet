package edu.epicode.mysmartwallet.model.category;

import edu.epicode.mysmartwallet.util.AppLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.logging.Logger;

/**
 * Categoria macro (composite) nel Composite pattern.
 * Rappresenta una categoria che può contenere sottocategorie.
 *
 * Le MacroCategory sono i nodi interni dell'albero delle categorie
 * e possono contenere sia altre MacroCategory che StandardCategory.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class MacroCategory extends CategoryComponent {

    private static final Logger logger = AppLogger.getLogger(MacroCategory.class);

    /**
     * Lista delle sottocategorie.
     */
    private final List<CategoryComponent> children;

    /**
     * Crea una nuova macro categoria.
     *
     * @param id          l'identificativo univoco
     * @param name        il nome della categoria
     * @param description la descrizione della categoria
     * @param parentId    l'ID della categoria padre, null se radice
     */
    public MacroCategory(int id, String name, String description, Integer parentId) {
        super(id, name, description, parentId);
        this.children = new ArrayList<>();
        logger.fine("Creata MacroCategory: " + name + " (id=" + id + ")");
    }

    /**
     * Aggiunge una sottocategoria a questa macro categoria.
     *
     * @param child la sottocategoria da aggiungere
     */
    public void addChild(CategoryComponent child) {
        children.add(child);
        logger.fine("Aggiunta sottocategoria " + child.getName() + " a " + name);
    }

    /**
     * Rimuove una sottocategoria dato il suo ID.
     *
     * @param childId l'ID della sottocategoria da rimuovere
     * @return true se la sottocategoria è stata rimossa, false altrimenti
     */
    public boolean removeChild(int childId) {
        boolean removed = children.removeIf(child -> child.getId() == childId);
        if (removed) {
            logger.fine("Rimossa sottocategoria con id=" + childId + " da " + name);
        }
        return removed;
    }

    /**
     * Restituisce una copia difensiva della lista dei figli.
     *
     * @return una nuova lista contenente i figli
     */
    public List<CategoryComponent> getChildren() {
        return new ArrayList<>(children);
    }

    /**
     * Indica che questa non è una categoria foglia.
     *
     * @return sempre false per MacroCategory
     */
    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * Stampa questa categoria e tutte le sottocategorie in modo ricorsivo.
     *
     * @param indent la stringa di indentazione corrente
     */
    @Override
    public void print(String indent) {
        System.out.println(indent + "+ " + name + " [" + id + "]");
        String childIndent = indent + "  ";
        for (CategoryComponent child : children) {
            child.print(childIndent);
        }
    }

    /**
     * Restituisce un iteratore che attraversa tutto l'albero in profondità (DFS).
     * L'iteratore visita prima questa categoria, poi ricorsivamente tutti i figli.
     *
     * @return un Iterator DFS su tutte le CategoryComponent dell'albero
     */
    @Override
    public Iterator<CategoryComponent> iterator() {
        return new CategoryIterator(this);
    }

    /**
     * Iteratore personalizzato per attraversare l'albero delle categorie in profondità.
     * Utilizza uno Stack per implementare l'attraversamento DFS iterativo.
     */
    private static class CategoryIterator implements Iterator<CategoryComponent> {

        /**
         * Stack di iteratori per gestire la visita DFS.
         */
        private final Stack<Iterator<CategoryComponent>> iteratorStack;

        /**
         * Il prossimo elemento da restituire.
         */
        private CategoryComponent nextElement;

        /**
         * Flag che indica se abbiamo già calcolato il prossimo elemento.
         */
        private boolean hasNextComputed;

        /**
         * Crea un nuovo iteratore a partire dalla categoria radice.
         *
         * @param root la categoria radice da cui iniziare l'attraversamento
         */
        CategoryIterator(MacroCategory root) {
            this.iteratorStack = new Stack<>();
            this.nextElement = root;
            this.hasNextComputed = true;
        }

        @Override
        public boolean hasNext() {
            if (hasNextComputed) {
                return nextElement != null;
            }
            computeNext();
            hasNextComputed = true;
            return nextElement != null;
        }

        @Override
        public CategoryComponent next() {
            if (!hasNext()) {
                throw new NoSuchElementException("Nessun altro elemento nell'iteratore");
            }

            CategoryComponent current = nextElement;

            // Se il corrente è una MacroCategory, aggiungi i suoi figli allo stack
            if (current instanceof MacroCategory) {
                MacroCategory macro = (MacroCategory) current;
                if (!macro.children.isEmpty()) {
                    iteratorStack.push(macro.children.iterator());
                }
            }

            hasNextComputed = false;
            return current;
        }

        /**
         * Calcola il prossimo elemento da restituire.
         */
        private void computeNext() {
            nextElement = null;

            while (!iteratorStack.isEmpty()) {
                Iterator<CategoryComponent> currentIterator = iteratorStack.peek();
                if (currentIterator.hasNext()) {
                    nextElement = currentIterator.next();
                    return;
                } else {
                    iteratorStack.pop();
                }
            }
        }
    }
}
