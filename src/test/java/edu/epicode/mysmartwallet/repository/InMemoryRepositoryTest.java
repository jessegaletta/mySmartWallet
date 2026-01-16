package edu.epicode.mysmartwallet.repository;

import edu.epicode.mysmartwallet.exception.ItemNotFoundException;
import edu.epicode.mysmartwallet.model.BaseEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per la classe InMemoryRepository.
 * Verifica tutte le operazioni CRUD e la generazione degli ID.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
class InMemoryRepositoryTest {

    /**
     * Classe di test che estende BaseEntity per i test del repository.
     */
    private static class TestEntity extends BaseEntity {
        private final String name;

        public TestEntity(int id, String name) {
            super(id);
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private InMemoryRepository<TestEntity> repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepository<>();
    }

    @Test
    @DisplayName("save() e findById() funzionano correttamente")
    void testSaveAndFindById() {
        TestEntity entity = new TestEntity(1, "Test");
        repository.save(entity);

        Optional<TestEntity> result = repository.findById(1);

        assertTrue(result.isPresent());
        assertEquals("Test", result.get().getName());
        assertEquals(1, result.get().getId());
    }

    @Test
    @DisplayName("findById() restituisce Optional vuoto per ID inesistente")
    void testFindByIdNotFound() {
        Optional<TestEntity> result = repository.findById(999);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findAll() restituisce tutte le entità")
    void testFindAll() {
        repository.save(new TestEntity(1, "Primo"));
        repository.save(new TestEntity(2, "Secondo"));
        repository.save(new TestEntity(3, "Terzo"));

        List<TestEntity> result = repository.findAll();

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("findAll() restituisce lista vuota se repository vuoto")
    void testFindAllEmpty() {
        List<TestEntity> result = repository.findAll();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("delete() rimuove l'entità correttamente")
    void testDelete() throws ItemNotFoundException {
        repository.save(new TestEntity(1, "Test"));
        assertTrue(repository.exists(1));

        repository.delete(1);

        assertFalse(repository.exists(1));
        assertEquals(0, repository.count());
    }

    @Test
    @DisplayName("delete() lancia ItemNotFoundException per ID inesistente")
    void testDeleteNotFound() {
        assertThrows(ItemNotFoundException.class, () -> repository.delete(999));
    }

    @Test
    @DisplayName("exists() restituisce true per entità esistente")
    void testExistsTrue() {
        repository.save(new TestEntity(1, "Test"));

        assertTrue(repository.exists(1));
    }

    @Test
    @DisplayName("exists() restituisce false per entità inesistente")
    void testExistsFalse() {
        assertFalse(repository.exists(1));
    }

    @Test
    @DisplayName("count() restituisce il numero corretto di entità")
    void testCount() {
        assertEquals(0, repository.count());

        repository.save(new TestEntity(1, "Primo"));
        assertEquals(1, repository.count());

        repository.save(new TestEntity(2, "Secondo"));
        assertEquals(2, repository.count());
    }

    @Test
    @DisplayName("clear() svuota il repository")
    void testClear() {
        repository.save(new TestEntity(1, "Primo"));
        repository.save(new TestEntity(2, "Secondo"));
        assertEquals(2, repository.count());

        repository.clear();

        assertEquals(0, repository.count());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("generateNextId() incrementa ad ogni chiamata")
    void testGenerateNextId() {
        int firstId = repository.generateNextId();
        int secondId = repository.generateNextId();
        int thirdId = repository.generateNextId();

        assertEquals(1, firstId);
        assertEquals(2, secondId);
        assertEquals(3, thirdId);
    }

    @Test
    @DisplayName("save() sovrascrive entità esistente con stesso ID")
    void testSaveOverwrite() {
        repository.save(new TestEntity(1, "Originale"));
        repository.save(new TestEntity(1, "Modificato"));

        Optional<TestEntity> result = repository.findById(1);

        assertTrue(result.isPresent());
        assertEquals("Modificato", result.get().getName());
        assertEquals(1, repository.count());
    }
}
