package edu.epicode.mysmartwallet.persistence;

import edu.epicode.mysmartwallet.exception.InvalidInputException;
import edu.epicode.mysmartwallet.exception.StorageException;
import edu.epicode.mysmartwallet.model.Account;
import edu.epicode.mysmartwallet.model.Transaction;
import edu.epicode.mysmartwallet.model.TransactionType;
import edu.epicode.mysmartwallet.repository.DataStorage;
import edu.epicode.mysmartwallet.util.MoneyUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per la classe CsvService.
 * Verifica le operazioni di lettura e scrittura su file CSV.
 *
 * @author Jesse Galetta
 * @version 1.0
 */
class CsvServiceTest {

    @TempDir
    Path tempDir;

    private List<Account> originalAccounts;
    private List<Transaction> originalTransactions;

    @BeforeEach
    void setUp() throws StorageException {
        // Reset singleton per ogni test
        DataStorage.resetInstance();

        // Salva eventuali dati esistenti per ripristinarli dopo i test
        try {
            originalAccounts = CsvService.loadAccounts();
            originalTransactions = CsvService.loadTransactions();
        } catch (StorageException e) {
            originalAccounts = new ArrayList<>();
            originalTransactions = new ArrayList<>();
        }
    }

    @AfterEach
    void tearDown() throws StorageException, IOException {
        DataStorage.resetInstance();

        // Ripristina i dati originali o elimina i file di test
        if (!originalAccounts.isEmpty()) {
            CsvService.saveAccounts(originalAccounts);
        } else {
            Files.deleteIfExists(Path.of("data/accounts.csv"));
        }

        if (!originalTransactions.isEmpty()) {
            CsvService.saveTransactions(originalTransactions);
        } else {
            Files.deleteIfExists(Path.of("data/transactions.csv"));
        }
    }

    @Test
    @DisplayName("saveAccounts e loadAccounts - round-trip corretto")
    void testAccountsRoundTrip() throws StorageException {
        // Crea account di test
        List<Account> testAccounts = new ArrayList<>();
        testAccounts.add(new Account(1, "Conto Principale", 1, MoneyUtil.of("1000.00")));
        testAccounts.add(new Account(2, "Conto Risparmio", 1, MoneyUtil.of("5000.00")));
        testAccounts.add(new Account(3, "Conto Dollari", 2, MoneyUtil.of("2500.00")));

        // Salva
        CsvService.saveAccounts(testAccounts);

        // Ricarica
        List<Account> loadedAccounts = CsvService.loadAccounts();

        // Verifica
        assertEquals(3, loadedAccounts.size());

        Account loaded1 = loadedAccounts.stream()
                .filter(a -> a.getId() == 1).findFirst().orElse(null);
        assertNotNull(loaded1);
        assertEquals("Conto Principale", loaded1.getName());
        assertEquals(1, loaded1.getCurrencyId());
        assertEquals(MoneyUtil.of("1000.00"), loaded1.getInitialBalance());

        Account loaded2 = loadedAccounts.stream()
                .filter(a -> a.getId() == 2).findFirst().orElse(null);
        assertNotNull(loaded2);
        assertEquals("Conto Risparmio", loaded2.getName());
        assertEquals(MoneyUtil.of("5000.00"), loaded2.getInitialBalance());
    }

    @Test
    @DisplayName("saveTransactions e loadTransactions - round-trip corretto")
    void testTransactionsRoundTrip() throws StorageException, InvalidInputException {
        // Crea transazioni di test
        List<Transaction> testTransactions = new ArrayList<>();

        testTransactions.add(new Transaction.Builder()
                .withId(1)
                .withAccountId(1)
                .withType(TransactionType.INCOME)
                .withDate(LocalDate.of(2024, 1, 15))
                .withAmount(MoneyUtil.of("1500.00"))
                .withDescription("Stipendio gennaio")
                .withCategoryId(10)
                .build());

        testTransactions.add(new Transaction.Builder()
                .withId(2)
                .withAccountId(1)
                .withType(TransactionType.EXPENSE)
                .withDate(LocalDate.of(2024, 1, 20))
                .withAmount(MoneyUtil.of("50.00"))
                .withDescription("Spesa supermercato")
                .withCategoryId(20)
                .build());

        testTransactions.add(new Transaction.Builder()
                .withId(3)
                .withAccountId(2)
                .withType(TransactionType.TRANSFER_OUT)
                .withDate(LocalDate.of(2024, 2, 1))
                .withAmount(MoneyUtil.of("200.00"))
                .withDescription("Trasferimento risparmio")
                .withCategoryId(30)
                .build());

        // Salva
        CsvService.saveTransactions(testTransactions);

        // Ricarica
        List<Transaction> loadedTransactions = CsvService.loadTransactions();

        // Verifica
        assertEquals(3, loadedTransactions.size());

        Transaction loaded1 = loadedTransactions.stream()
                .filter(t -> t.getId() == 1).findFirst().orElse(null);
        assertNotNull(loaded1);
        assertEquals(TransactionType.INCOME, loaded1.getType());
        assertEquals(LocalDate.of(2024, 1, 15), loaded1.getDate());
        assertEquals(MoneyUtil.of("1500.00"), loaded1.getAmount());
        assertEquals("Stipendio gennaio", loaded1.getDescription());
        assertEquals(10, loaded1.getCategoryId());
        assertEquals(1, loaded1.getAccountId());

        Transaction loaded2 = loadedTransactions.stream()
                .filter(t -> t.getId() == 2).findFirst().orElse(null);
        assertNotNull(loaded2);
        assertEquals(TransactionType.EXPENSE, loaded2.getType());
    }

    @Test
    @DisplayName("loadAccounts restituisce lista vuota per file mancante")
    void testLoadAccountsMissingFile() throws StorageException, IOException {
        // Salva lista vuota per "eliminare" i dati esistenti
        CsvService.saveAccounts(new ArrayList<>());

        // Elimina il file (se esiste)
        Path accountsFile = Path.of("data/accounts.csv");
        Files.deleteIfExists(accountsFile);

        // Carica - non deve lanciare eccezioni
        List<Account> accounts = assertDoesNotThrow(() -> CsvService.loadAccounts());

        // Deve restituire lista vuota
        assertTrue(accounts.isEmpty());
    }

    @Test
    @DisplayName("loadTransactions restituisce lista vuota per file mancante")
    void testLoadTransactionsMissingFile() throws StorageException, IOException {
        // Salva lista vuota per "eliminare" i dati esistenti
        CsvService.saveTransactions(new ArrayList<>());

        // Elimina il file (se esiste)
        Path transactionsFile = Path.of("data/transactions.csv");
        Files.deleteIfExists(transactionsFile);

        // Carica - non deve lanciare eccezioni
        List<Transaction> transactions = assertDoesNotThrow(() -> CsvService.loadTransactions());

        // Deve restituire lista vuota
        assertTrue(transactions.isEmpty());
    }

    @Test
    @DisplayName("saveAccounts gestisce caratteri speciali nelle descrizioni")
    void testAccountsWithSpecialCharacters() throws StorageException {
        List<Account> testAccounts = new ArrayList<>();
        testAccounts.add(new Account(1, "Conto con accenti àèìòù", 1, MoneyUtil.of("100.00")));
        testAccounts.add(new Account(2, "Conto con simboli: @#$%&*()", 1, MoneyUtil.of("200.00")));

        CsvService.saveAccounts(testAccounts);
        List<Account> loaded = CsvService.loadAccounts();

        assertEquals(2, loaded.size());

        Account withAccents = loaded.stream()
                .filter(a -> a.getId() == 1).findFirst().orElse(null);
        assertNotNull(withAccents);
        assertEquals("Conto con accenti àèìòù", withAccents.getName());

        Account withSymbols = loaded.stream()
                .filter(a -> a.getId() == 2).findFirst().orElse(null);
        assertNotNull(withSymbols);
        assertEquals("Conto con simboli: @#$%&*()", withSymbols.getName());
    }

    @Test
    @DisplayName("saveTransactions gestisce descrizioni vuote")
    void testTransactionsWithEmptyDescription() throws StorageException, InvalidInputException {
        List<Transaction> testTransactions = new ArrayList<>();

        testTransactions.add(new Transaction.Builder()
                .withId(1)
                .withAccountId(1)
                .withType(TransactionType.EXPENSE)
                .withDate(LocalDate.now())
                .withAmount(MoneyUtil.of("25.00"))
                .withDescription("")
                .withCategoryId(10)
                .build());

        CsvService.saveTransactions(testTransactions);
        List<Transaction> loaded = CsvService.loadTransactions();

        assertEquals(1, loaded.size());
        assertEquals("", loaded.get(0).getDescription());
    }

    @Test
    @DisplayName("Operazioni su file CSV in directory temporanea con @TempDir")
    void testFileOperationsWithTempDir() throws IOException {
        // Questo test dimostra l'uso di @TempDir per operazioni su file
        Path tempFile = tempDir.resolve("test_data.csv");

        // Scrivi dati di test
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            writer.write("ID;Nome;Valore");
            writer.newLine();
            writer.write("1;Test;100.00");
            writer.newLine();
        }

        // Verifica che il file esista
        assertTrue(Files.exists(tempFile));

        // Leggi e verifica contenuto
        List<String> lines = Files.readAllLines(tempFile, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertEquals("ID;Nome;Valore", lines.get(0));
        assertEquals("1;Test;100.00", lines.get(1));

        // Il file viene automaticamente eliminato alla fine del test grazie a @TempDir
    }

    @Test
    @DisplayName("Verifica consistenza dati dopo multiple operazioni")
    void testDataConsistencyAfterMultipleOperations() throws StorageException, InvalidInputException {
        // Prima operazione: salva account
        List<Account> accounts1 = new ArrayList<>();
        accounts1.add(new Account(1, "Conto A", 1, MoneyUtil.of("1000.00")));
        CsvService.saveAccounts(accounts1);

        // Seconda operazione: aggiungi altro account
        List<Account> accounts2 = new ArrayList<>();
        accounts2.add(new Account(1, "Conto A", 1, MoneyUtil.of("1000.00")));
        accounts2.add(new Account(2, "Conto B", 1, MoneyUtil.of("2000.00")));
        CsvService.saveAccounts(accounts2);

        // Carica e verifica che ci siano entrambi
        List<Account> loaded = CsvService.loadAccounts();
        assertEquals(2, loaded.size());

        // Salva lista vuota
        CsvService.saveAccounts(new ArrayList<>());

        // Carica e verifica che sia vuota (solo header)
        List<Account> emptyLoaded = CsvService.loadAccounts();
        assertTrue(emptyLoaded.isEmpty());
    }

    @Test
    @DisplayName("saveAccounts crea directory data se non esiste")
    void testSaveAccountsCreatesDirectory() throws StorageException, IOException {
        // Questo test verifica che la directory data/ venga creata se necessario
        // La funzione ensureDataDirectory() gestisce questo caso

        List<Account> testAccounts = new ArrayList<>();
        testAccounts.add(new Account(1, "Test", 1, MoneyUtil.of("100.00")));

        // Non deve lanciare eccezioni anche se la directory non esiste
        assertDoesNotThrow(() -> CsvService.saveAccounts(testAccounts));

        // Verifica che la directory esista dopo il salvataggio
        assertTrue(Files.exists(Path.of("data")));
    }

    @Test
    @DisplayName("loadAccounts gestisce correttamente righe vuote nel file")
    void testLoadAccountsSkipsEmptyLines() throws StorageException, IOException {
        // Crea file con righe vuote
        Path accountsFile = Path.of("data/accounts.csv");
        Files.createDirectories(accountsFile.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(accountsFile, StandardCharsets.UTF_8)) {
            writer.write("ID;Nome;CurrencyId;InitialBalance");
            writer.newLine();
            writer.write("1;Conto Test;1;500.00");
            writer.newLine();
            writer.write("");  // riga vuota
            writer.newLine();
            writer.write("2;Altro Conto;1;300.00");
            writer.newLine();
        }

        // Carica - deve ignorare le righe vuote
        List<Account> accounts = CsvService.loadAccounts();

        assertEquals(2, accounts.size());
    }

    @Test
    @DisplayName("Transazioni con diversi tipi vengono salvate e caricate correttamente")
    void testAllTransactionTypes() throws StorageException, InvalidInputException {
        List<Transaction> transactions = new ArrayList<>();

        transactions.add(new Transaction.Builder()
                .withId(1)
                .withAccountId(1)
                .withType(TransactionType.INCOME)
                .withDate(LocalDate.now())
                .withAmount(MoneyUtil.of("100.00"))
                .withDescription("Entrata")
                .withCategoryId(1)
                .build());

        transactions.add(new Transaction.Builder()
                .withId(2)
                .withAccountId(1)
                .withType(TransactionType.EXPENSE)
                .withDate(LocalDate.now())
                .withAmount(MoneyUtil.of("50.00"))
                .withDescription("Uscita")
                .withCategoryId(2)
                .build());

        transactions.add(new Transaction.Builder()
                .withId(3)
                .withAccountId(1)
                .withType(TransactionType.TRANSFER_OUT)
                .withDate(LocalDate.now())
                .withAmount(MoneyUtil.of("25.00"))
                .withDescription("Trasferimento")
                .withCategoryId(3)
                .build());

        CsvService.saveTransactions(transactions);
        List<Transaction> loaded = CsvService.loadTransactions();

        assertEquals(3, loaded.size());

        long incomeCount = loaded.stream()
                .filter(t -> t.getType() == TransactionType.INCOME).count();
        long expenseCount = loaded.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE).count();
        long transferOutCount = loaded.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_OUT).count();

        assertEquals(1, incomeCount);
        assertEquals(1, expenseCount);
        assertEquals(1, transferOutCount);
    }
}
