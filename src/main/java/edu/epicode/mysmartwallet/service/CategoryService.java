package edu.epicode.mysmartwallet.service;

import edu.epicode.mysmartwallet.exception.InvalidInputException;
import edu.epicode.mysmartwallet.exception.ItemNotFoundException;
import edu.epicode.mysmartwallet.exception.WalletException;
import edu.epicode.mysmartwallet.model.category.CategoryComponent;
import edu.epicode.mysmartwallet.model.category.MacroCategory;
import edu.epicode.mysmartwallet.model.category.StandardCategory;
import edu.epicode.mysmartwallet.repository.DataStorage;
import edu.epicode.mysmartwallet.repository.Repository;
import edu.epicode.mysmartwallet.util.AppLogger;
import edu.epicode.mysmartwallet.util.InputValidator;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Servizio per la gestione delle categorie.
 *
 * Fornisce le funzionalità per la gestione delle categorie gerarchiche:
 * <ul>
 *   <li>Creazione di macro-categorie e categorie standard</li>
 *   <li>Gestione delle relazioni parent-child</li>
 *   <li>Eliminazione con verifica vincoli</li>
 *   <li>Ricerca e navigazione dell'albero</li>
 * </ul>
 * 
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public class CategoryService {

    private static final Logger logger = AppLogger.getLogger(CategoryService.class);

    private final Repository<CategoryComponent> categoryRepository;
    private final DataStorage dataStorage;

    /**
     * Crea un nuovo CategoryService recuperando i repository da DataStorage.
     */
    public CategoryService() {
        this.dataStorage = DataStorage.getInstance();
        this.categoryRepository = dataStorage.getCategoryRepository();
        logger.info("CategoryService inizializzato");
    }

    /**
     * Restituisce tutte le categorie.
     *
     * @return lista di tutte le categorie
     */
    public List<CategoryComponent> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Cerca una categoria per ID.
     *
     * @param categoryId l'ID della categoria
     * @return la categoria trovata
     * @throws ItemNotFoundException se la categoria non esiste
     */
    public CategoryComponent getCategory(int categoryId) throws ItemNotFoundException {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Categoria con ID " + categoryId + " non trovata"));
    }

    /**
     * Cerca una categoria per ID, restituendo un Optional.
     *
     * @param categoryId l'ID della categoria
     * @return Optional con la categoria se trovata
     */
    public Optional<CategoryComponent> findCategory(int categoryId) {
        return categoryRepository.findById(categoryId);
    }

    /**
     * Crea una nuova macro-categoria (categoria con sottocategorie).
     *
     * @param name        il nome della categoria
     * @param description la descrizione
     * @param parentId    l'ID del parent (null per root)
     * @return la categoria creata
     * @throws InvalidInputException se i parametri non sono validi
     * @throws WalletException       se il parent non e' valido
     */
    public MacroCategory createMacroCategory(String name, String description, Integer parentId)
            throws InvalidInputException, WalletException {

        InputValidator.validateNotEmpty(name, "nome categoria");

        validateParent(parentId);

        int newId = categoryRepository.generateNextId();
        MacroCategory newCategory = new MacroCategory(newId, name, description, parentId);

        categoryRepository.save(newCategory);
        linkToParent(newCategory, parentId);

        logger.info("Creata macro-categoria: " + name + " (ID: " + newId + ")");
        return newCategory;
    }

    /**
     * Crea una nuova categoria standard (foglia).
     *
     * @param name        il nome della categoria
     * @param description la descrizione
     * @param parentId    l'ID del parent (null per root)
     * @return la categoria creata
     * @throws InvalidInputException se i parametri non sono validi
     * @throws WalletException       se il parent non e' valido
     */
    public StandardCategory createStandardCategory(String name, String description, Integer parentId)
            throws InvalidInputException, WalletException {

        InputValidator.validateNotEmpty(name, "nome categoria");

        validateParent(parentId);

        int newId = categoryRepository.generateNextId();
        StandardCategory newCategory = new StandardCategory(newId, name, description, parentId);

        categoryRepository.save(newCategory);
        linkToParent(newCategory, parentId);

        logger.info("Creata categoria standard: " + name + " (ID: " + newId + ")");
        return newCategory;
    }

    /**
     * Valida il parent specificato.
     *
     * @param parentId l'ID del parent da validare
     * @throws WalletException se il parent non e' valido
     */
    private void validateParent(Integer parentId) throws WalletException {
        if (parentId == null) {
            return;
        }

        Optional<CategoryComponent> parentOpt = categoryRepository.findById(parentId);

        if (parentOpt.isEmpty()) {
            throw new ItemNotFoundException("Categoria parent con ID " + parentId + " non trovata");
        }

        if (parentOpt.get().isLeaf()) {
            throw new InvalidInputException(
                    "La categoria selezionata e' una foglia e non puo' avere figli");
        }
    }

    /**
     * Collega una categoria al suo parent.
     *
     * @param category la categoria da collegare
     * @param parentId l'ID del parent
     */
    private void linkToParent(CategoryComponent category, Integer parentId) {
        if (parentId == null) {
            return;
        }

        categoryRepository.findById(parentId).ifPresent(parent -> {
            if (parent instanceof MacroCategory) {
                ((MacroCategory) parent).addChild(category);
                logger.fine("Collegata categoria " + category.getId() + " al parent " + parentId);
            }
        });
    }

    /**
     * Verifica se una categoria puo' essere eliminata.
     *
     * @param categoryId l'ID della categoria
     * @return risultato della verifica con eventuali dettagli sul blocco
     */
    public DeleteCheckResult canDelete(int categoryId) {
        Optional<CategoryComponent> categoryOpt = categoryRepository.findById(categoryId);

        if (categoryOpt.isEmpty()) {
            return new DeleteCheckResult(false, "Categoria non trovata");
        }

        CategoryComponent category = categoryOpt.get();

        // Verifica se ha figli (solo per MacroCategory)
        if (category instanceof MacroCategory) {
            MacroCategory macro = (MacroCategory) category;
            if (!macro.getChildren().isEmpty()) {
                return new DeleteCheckResult(false,
                        "La categoria ha " + macro.getChildren().size() + " sottocategorie collegate");
            }
        }

        // Verifica se ci sono transazioni che usano questa categoria
        long transactionCount = dataStorage.getTransactionRepository().findAll().stream()
                .filter(t -> t.getCategoryId() == categoryId)
                .count();

        if (transactionCount > 0) {
            return new DeleteCheckResult(false,
                    "La categoria e' usata in " + transactionCount + " transazioni");
        }

        return new DeleteCheckResult(true, null);
    }

    /**
     * Elimina una categoria.
     *
     * @param categoryId l'ID della categoria da eliminare
     * @throws ItemNotFoundException  se la categoria non esiste
     * @throws InvalidInputException  se la categoria non puo' essere eliminata
     */
    public void deleteCategory(int categoryId) throws ItemNotFoundException, InvalidInputException {
        DeleteCheckResult check = canDelete(categoryId);
        if (!check.canDelete()) {
            throw new InvalidInputException("Impossibile eliminare: " + check.getReason());
        }

        CategoryComponent category = getCategory(categoryId);

        // Rimuovi dal parent se presente
        Integer parentId = category.getParentId();
        if (parentId != null) {
            categoryRepository.findById(parentId).ifPresent(parent -> {
                if (parent instanceof MacroCategory) {
                    ((MacroCategory) parent).removeChild(categoryId);
                    logger.fine("Rimosso riferimento al figlio " + categoryId + " dal parent " + parentId);
                }
            });
        }

        // Elimina dal repository
        categoryRepository.delete(categoryId);

        logger.info("Eliminata categoria: " + category.getName() + " (ID: " + categoryId + ")");
    }

    /**
     * Risultato della verifica di eliminabilita' di una categoria.
     */
    public static class DeleteCheckResult {
        private final boolean canDelete;
        private final String reason;

        /**
         * Costruisce un nuovo risultato di verifica.
         *
         * @param canDelete true se la categoria puo' essere eliminata
         * @param reason    motivazione del risultato
         */
        public DeleteCheckResult(boolean canDelete, String reason) {
            this.canDelete = canDelete;
            this.reason = reason;
        }

        /**
         * Indica se la categoria puo' essere eliminata.
         *
         * @return true se eliminabile, false altrimenti
         */
        public boolean canDelete() {
            return canDelete;
        }

        /**
         * Restituisce la motivazione del risultato.
         *
         * @return descrizione del motivo
         */
        public String getReason() {
            return reason;
        }
    }
}
