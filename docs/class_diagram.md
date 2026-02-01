# Class Diagram - MySmartWallet

Diagramma delle classi dell'applicazione MySmartWallet con rappresentazione delle relazioni tra componenti.

## Package model

### Gerarchia BaseEntity

```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        #int id
        +getId() int
        +equals(Object) boolean
        +hashCode() int
    }

    class Account {
        -String name
        -int currencyId
        -BigDecimal initialBalance
        -List~Transaction~ transactions
        -List~BalanceObserver~ observers
        +getBalance() BigDecimal
        +addTransaction(Transaction)
        +addObserver(BalanceObserver)
        +removeObserver(BalanceObserver)
    }

    class Transaction {
        -LocalDate date
        -String description
        -BigDecimal amount
        -TransactionType type
        -int categoryId
        -int accountId
        -BigDecimal originalAmount
        -int originalCurrencyId
        -BigDecimal exchangeRate
        +getters()
    }

    class Currency {
        -String code
        -String name
        -String symbol
        -TreeMap~LocalDate,BigDecimal~ rateHistory
        +addRate(LocalDate, BigDecimal)
        +getRateForDate(LocalDate) BigDecimal
        +getRateHistory() Map
    }

    class TransactionType {
        <<enumeration>>
        INCOME
        EXPENSE
        TRANSFER
        +getDescription() String
    }

    BaseEntity <|-- Account
    BaseEntity <|-- Transaction
    BaseEntity <|-- Currency
    Account "1" --> "*" Transaction : contiene
    Transaction --> TransactionType
```

### Builder Pattern (Transaction)

```mermaid
classDiagram
    class Transaction {
        -LocalDate date
        -String description
        -BigDecimal amount
        -TransactionType type
        -int categoryId
        -int accountId
        -Transaction(Builder)
    }

    class Builder {
        <<static inner class>>
        -int id
        -LocalDate date
        -String description
        -BigDecimal amount
        -TransactionType type
        -int categoryId
        -int accountId
        +withId(int) Builder
        +withDate(LocalDate) Builder
        +withAmount(BigDecimal) Builder
        +withType(TransactionType) Builder
        +withDescription(String) Builder
        +withCategoryId(int) Builder
        +withAccountId(int) Builder
        +build() Transaction
    }

    Transaction ..> Builder : inner class
```

### Composite Pattern (Categories)

```mermaid
classDiagram
    class CategoryComponent {
        <<abstract>>
        #String name
        #String description
        #Integer parentId
        +getName() String
        +getDescription() String
        +getParentId() Integer
        +getFullPath() String
        +isLeaf() boolean*
        +print(String indent)*
        +iterator() Iterator*
    }

    class MacroCategory {
        -List~CategoryComponent~ children
        +addChild(CategoryComponent)
        +removeChild(int)
        +getChildren() List
        +isLeaf() boolean
        +iterator() Iterator
    }

    class StandardCategory {
        +isLeaf() boolean
        +iterator() Iterator
    }

    class CategoryIterator {
        <<inner class>>
        -Stack~Iterator~ iteratorStack
        -CategoryComponent nextElement
        +hasNext() boolean
        +next() CategoryComponent
    }

    BaseEntity <|-- CategoryComponent
    CategoryComponent <|-- MacroCategory
    CategoryComponent <|-- StandardCategory
    MacroCategory "1" --> "*" CategoryComponent : children
    MacroCategory ..> CategoryIterator : inner class
```

## Package repository

```mermaid
classDiagram
    class Repository~T~ {
        <<interface>>
        +save(T) T
        +findById(int) Optional~T~
        +findAll() List~T~
        +delete(int) boolean
        +clear()
        +generateNextId() int
    }

    class InMemoryRepository~T~ {
        -Map~Integer,T~ entities
        -AtomicInteger nextId
        +save(T) T
        +findById(int) Optional~T~
        +findAll() List~T~
        +delete(int) boolean
    }

    class DataStorage {
        <<Singleton>>
        -static DataStorage instance
        -Repository~Account~ accountRepository
        -Repository~Transaction~ transactionRepository
        -Repository~CategoryComponent~ categoryRepository
        -Repository~Currency~ currencyRepository
        -DataStorage()
        +static getInstance() DataStorage
        +static resetInstance()
        +getAccountRepository()
        +getTransactionRepository()
        +getCategoryRepository()
        +getCurrencyRepository()
    }

    Repository~T~ <|.. InMemoryRepository~T~
    DataStorage --> Repository : contains 4x
```

## Package service

### Strategy Pattern (Exchange)

```mermaid
classDiagram
    class ExchangeStrategy {
        <<interface>>
        +convert(BigDecimal, Currency, Currency, LocalDate) BigDecimal
    }

    class HistoricalExchangeStrategy {
        -CurrencyManager currencyManager
        +convert(BigDecimal, Currency, Currency, LocalDate) BigDecimal
    }

    class FixedExchangeStrategy {
        -BigDecimal fixedRate
        +FixedExchangeStrategy()
        +FixedExchangeStrategy(BigDecimal)
        +convert(BigDecimal, Currency, Currency, LocalDate) BigDecimal
    }

    ExchangeStrategy <|.. HistoricalExchangeStrategy
    ExchangeStrategy <|.. FixedExchangeStrategy
```

### Observer Pattern (Balance)

```mermaid
classDiagram
    class BalanceObserver {
        <<interface>>
        +onBalanceChanged(Account, BigDecimal, BigDecimal)
        +onTransactionAdded(Account, Transaction)
    }

    class ConsoleBalanceObserver {
        +onBalanceChanged(Account, BigDecimal, BigDecimal)
        +onTransactionAdded(Account, Transaction)
    }

    class Account {
        -List~BalanceObserver~ observers
        +addObserver(BalanceObserver)
        +removeObserver(BalanceObserver)
        -notifyBalanceChanged(BigDecimal, BigDecimal)
        -notifyTransactionAdded(Transaction)
    }

    BalanceObserver <|.. ConsoleBalanceObserver
    Account --> BalanceObserver : notifies
```

### Factory Pattern

```mermaid
classDiagram
    class TransactionFactory {
        -static AtomicInteger nextId
        +static createIncome(...) Transaction
        +static createExpense(...) Transaction
        +static createTransfer(...) List~Transaction~
        +static resetIdCounter()
    }

    class Transaction {
        <<created by factory>>
    }

    TransactionFactory ..> Transaction : creates
```

### Singleton Pattern

```mermaid
classDiagram
    class CurrencyManager {
        <<Singleton>>
        -static CurrencyManager instance
        -Repository~Currency~ currencyRepository
        -CurrencyManager()
        +static getInstance() CurrencyManager
        +static resetInstance()
        +getCurrency(String) Optional~Currency~
        +getCurrencyById(int) Optional~Currency~
        +initializeDefaultCurrencies()
    }

    class DataStorage {
        <<Singleton>>
        -static DataStorage instance
        +static getInstance() DataStorage
        +static resetInstance()
    }
```

## Package persistence

### Template Method Pattern

```mermaid
classDiagram
    class DataGenerator {
        <<abstract>>
        #DataStorage dataStorage
        +generate()* final
        #createCurrencies()* abstract
        #createCategories()* abstract
        #createAccounts()* abstract
        #createTransactions()* abstract
        #saveData()
    }

    class DemoDataGenerator {
        #createCurrencies()
        #createCategories()
        #createAccounts()
        #createTransactions()
    }

    class EmptyDataGenerator {
        #createCurrencies()
        #createCategories()
        #createAccounts()
        #createTransactions()
    }

    DataGenerator <|-- DemoDataGenerator
    DataGenerator <|-- EmptyDataGenerator
```

### CsvService

```mermaid
classDiagram
    class CsvService {
        <<utility>>
        -static String SEPARATOR
        -static String DATA_DIR
        -static DateTimeFormatter DATE_FORMAT
        +static saveAccounts(List~Account~)
        +static loadAccounts() List~Account~
        +static saveTransactions(List~Transaction~)
        +static loadTransactions() List~Transaction~
        +static saveCategories(List~CategoryComponent~)
        +static loadCategories() List~CategoryComponent~
        +static saveCurrencies(List~Currency~)
        +static loadCurrencies() List~Currency~
        +static saveAll()
        +static loadAll()
    }
```

## Package exception

### Exception Shielding

```mermaid
classDiagram
    class WalletException {
        +WalletException(String)
        +WalletException(String, Throwable)
    }

    class InsufficientFundsException {
        +InsufficientFundsException(String)
    }

    class ItemNotFoundException {
        +ItemNotFoundException(String)
    }

    class InvalidInputException {
        +InvalidInputException(String)
        +InvalidInputException(String, Throwable)
    }

    class RateNotFoundException {
        +RateNotFoundException(String)
    }

    class StorageException {
        +StorageException(String)
        +StorageException(String, Throwable)
    }

    RuntimeException <|-- WalletException
    WalletException <|-- InsufficientFundsException
    WalletException <|-- ItemNotFoundException
    WalletException <|-- InvalidInputException
    WalletException <|-- RateNotFoundException
    WalletException <|-- StorageException
```

## Relazioni Principali

| Relazione | Descrizione |
| --------- | ----------- |
| `Account` → `Transaction` | Composizione: Account contiene lista di transazioni |
| `Account` → `BalanceObserver` | Observer: Account notifica i cambiamenti di saldo |
| `MacroCategory` → `CategoryComponent` | Composite: MacroCategory contiene figli |
| `WalletService` → `ExchangeStrategy` | Strategy: Algoritmo di conversione intercambiabile |
| `DataStorage` → `Repository` | Singleton: Container centralizzato dei repository |
| `Transaction.Builder` → `Transaction` | Builder: Costruzione fluida di transazioni |
| `TransactionFactory` → `Transaction` | Factory: Creazione centralizzata di transazioni |
| `DataGenerator` → Sottoclassi | Template Method: Algoritmo con dettagli variabili |
