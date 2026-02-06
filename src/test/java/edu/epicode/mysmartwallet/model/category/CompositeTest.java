package edu.epicode.mysmartwallet.model.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per il Composite pattern delle categorie.
 * Verifica il corretto funzionamento di MacroCategory e StandardCategory.
 *
 * Struttura test:
 * <pre>
 * Spese (Macro, id=1)
 * ├── Cibo (Macro, id=2)
 * │   ├── Ristoranti (Standard, id=3)
 * │   └── Supermercato (Standard, id=4)
 * └── Trasporti (Standard, id=5)
 * </pre>
 * 
 *
 * @author Jesse Galetta
 * @version 1.0
 */
class CompositeTest {

    private MacroCategory spese;
    private MacroCategory cibo;
    private StandardCategory ristoranti;
    private StandardCategory supermercato;
    private StandardCategory trasporti;

    @BeforeEach
    void setUp() {
        // Creazione struttura gerarchica
        spese = new MacroCategory(1, "Spese", "Tutte le spese", null);
        cibo = new MacroCategory(2, "Cibo", "Spese alimentari", 1);
        ristoranti = new StandardCategory(3, "Ristoranti", "Pasti fuori casa", 2);
        supermercato = new StandardCategory(4, "Supermercato", "Spesa alimentare", 2);
        trasporti = new StandardCategory(5, "Trasporti", "Mezzi di trasporto", 1);

        // Costruzione albero
        cibo.addChild(ristoranti);
        cibo.addChild(supermercato);
        spese.addChild(cibo);
        spese.addChild(trasporti);
    }

    @Test
    @DisplayName("Creazione MacroCategory e StandardCategory")
    void testCategoryCreation() {
        assertNotNull(spese);
        assertNotNull(ristoranti);

        assertEquals("Spese", spese.getName());
        assertEquals("Ristoranti", ristoranti.getName());
        assertEquals(1, spese.getId());
        assertEquals(3, ristoranti.getId());
        assertNull(spese.getParentId());
        assertEquals(Integer.valueOf(2), ristoranti.getParentId());
    }

    @Test
    @DisplayName("isLeaf restituisce valori corretti")
    void testIsLeaf() {
        assertFalse(spese.isLeaf(), "MacroCategory non deve essere foglia");
        assertFalse(cibo.isLeaf(), "MacroCategory non deve essere foglia");
        assertTrue(ristoranti.isLeaf(), "StandardCategory deve essere foglia");
        assertTrue(supermercato.isLeaf(), "StandardCategory deve essere foglia");
        assertTrue(trasporti.isLeaf(), "StandardCategory deve essere foglia");
    }

    @Test
    @DisplayName("addChild costruisce struttura gerarchica")
    void testAddChild() {
        List<CategoryComponent> speseChildren = spese.getChildren();
        assertEquals(2, speseChildren.size());

        List<CategoryComponent> ciboChildren = cibo.getChildren();
        assertEquals(2, ciboChildren.size());

        // Verifica copia difensiva
        speseChildren.clear();
        assertEquals(2, spese.getChildren().size(), "getChildren deve restituire copia difensiva");
    }

    @Test
    @DisplayName("removeChild rimuove sottocategoria")
    void testRemoveChild() {
        MacroCategory testCategory = new MacroCategory(100, "Test", "Test", null);
        StandardCategory child1 = new StandardCategory(101, "Child1", "Desc", 100);
        StandardCategory child2 = new StandardCategory(102, "Child2", "Desc", 100);

        testCategory.addChild(child1);
        testCategory.addChild(child2);
        assertEquals(2, testCategory.getChildren().size());

        assertTrue(testCategory.removeChild(101));
        assertEquals(1, testCategory.getChildren().size());
        assertEquals("Child2", testCategory.getChildren().get(0).getName());

        assertFalse(testCategory.removeChild(999), "removeChild con id inesistente deve restituire false");
    }

    @Test
    @DisplayName("Iterator attraversa tutto l'albero in DFS")
    void testIterator() {
        Iterator<CategoryComponent> iterator = spese.iterator();
        List<String> visited = new ArrayList<>();

        while (iterator.hasNext()) {
            CategoryComponent component = iterator.next();
            visited.add(component.getName());
        }

        assertEquals(5, visited.size(), "L'iteratore deve visitare tutti i 5 nodi");
        assertEquals("Spese", visited.get(0), "Deve partire dalla radice");
        assertTrue(visited.contains("Cibo"));
        assertTrue(visited.contains("Ristoranti"));
        assertTrue(visited.contains("Supermercato"));
        assertTrue(visited.contains("Trasporti"));
    }

    @Test
    @DisplayName("Iterator di StandardCategory contiene solo se stessa")
    void testStandardCategoryIterator() {
        Iterator<CategoryComponent> iterator = ristoranti.iterator();
        assertTrue(iterator.hasNext());

        CategoryComponent component = iterator.next();
        assertEquals(ristoranti, component);

        assertFalse(iterator.hasNext(), "StandardCategory iterator deve contenere solo se stessa");
    }

    @Test
    @DisplayName("print produce output strutturato")
    void testPrint() {
        // Il test verifica che print non lanci eccezioni
        // L'output va su System.out e viene verificato manualmente
        assertDoesNotThrow(() -> spese.print(""));
        assertDoesNotThrow(() -> ristoranti.print("  "));
    }

    @Test
    @DisplayName("toString restituisce formato corretto")
    void testToString() {
        String speseString = spese.toString();
        assertTrue(speseString.contains("MacroCategory"));
        assertTrue(speseString.contains("Spese"));
        assertTrue(speseString.contains("id=1"));

        String ristorantiString = ristoranti.toString();
        assertTrue(ristorantiString.contains("StandardCategory"));
        assertTrue(ristorantiString.contains("Ristoranti"));
    }

    @Test
    @DisplayName("Struttura profonda funziona correttamente")
    void testDeepStructure() {
        // Crea struttura più profonda
        MacroCategory root = new MacroCategory(10, "Root", "Radice", null);
        MacroCategory level1 = new MacroCategory(11, "Level1", "Livello 1", 10);
        MacroCategory level2 = new MacroCategory(12, "Level2", "Livello 2", 11);
        StandardCategory leaf = new StandardCategory(13, "Leaf", "Foglia", 12);

        level2.addChild(leaf);
        level1.addChild(level2);
        root.addChild(level1);

        // Verifica che l'iteratore attraversi tutti i livelli
        Iterator<CategoryComponent> iterator = root.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        assertEquals(4, count, "Deve attraversare tutti i 4 nodi");
    }
}
