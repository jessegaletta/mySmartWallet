# Architecture Diagram - MySmartWallet

Diagramma architetturale a strati dell'applicazione MySmartWallet.

## Architettura a Strati

```mermaid
flowchart TB
    subgraph PRESENTATION["PRESENTATION LAYER"]
        Main["Main.java"]
        CLI["CLI / Menu"]
        Main --> CLI
    end

    subgraph SERVICE["SERVICE LAYER"]
        WS["WalletService"]
        RS["ReportService"]
        CS["CategoryService"]
        CM["CurrencyManager<br/>(Singleton)"]
        TF["TransactionFactory<br/>(Factory)"]
        ES["ExchangeStrategy<br/>(Strategy)"]

        WS --> CM
        WS --> TF
        WS --> ES
        RS --> CM
    end

    subgraph MODEL["DOMAIN / MODEL LAYER"]
        Account["Account<br/>(Observer)"]
        Transaction["Transaction<br/>(Builder)"]
        Currency["Currency"]
        Category["CategoryComponent<br/>(Composite + Iterator)"]
        Observer["BalanceObserver"]

        Account --> Transaction
        Account --> Observer
    end

    subgraph REPOSITORY["REPOSITORY LAYER"]
        DS["DataStorage<br/>(Singleton)"]
        Repo["Repository&lt;T&gt;<br/>(Generics)"]
        IMR["InMemoryRepository&lt;T&gt;"]

        DS --> Repo
        Repo --> IMR
    end

    subgraph PERSISTENCE["PERSISTENCE LAYER"]
        CSV["CsvService<br/>(Java I/O)"]
        DG["DataGenerator<br/>(Template Method)"]
        FS[("File System<br/>CSV Files")]

        DG --> CSV
        CSV --> FS
    end

    subgraph CROSSCUTTING["CROSS-CUTTING CONCERNS"]
        Logger["AppLogger<br/>(Logging)"]
        Validator["InputValidator<br/>(Sanitization)"]
        Money["MoneyUtil<br/>(BigDecimal)"]
        Exceptions["Exceptions<br/>(Shielding)"]
    end

    PRESENTATION --> SERVICE
    SERVICE --> MODEL
    SERVICE --> REPOSITORY
    REPOSITORY --> PERSISTENCE

    CROSSCUTTING -.-> PRESENTATION
    CROSSCUTTING -.-> SERVICE
    CROSSCUTTING -.-> MODEL
    CROSSCUTTING -.-> REPOSITORY
    CROSSCUTTING -.-> PERSISTENCE
```

## Dettaglio Service Layer

```mermaid
flowchart LR
    subgraph Services
        WS["WalletService"]
        RS["ReportService"]
        CS["CategoryService"]
    end

    subgraph Managers
        CM["CurrencyManager<br/>(Singleton)"]
    end

    subgraph Factories
        TF["TransactionFactory"]
    end

    subgraph Strategies
        ES["ExchangeStrategy"]
        HES["HistoricalExchangeStrategy"]
        FES["FixedExchangeStrategy"]

        ES --> HES
        ES --> FES
    end

    WS --> CM
    WS --> TF
    WS --> ES
    RS --> CM
```

## Dettaglio Model Layer

```mermaid
flowchart TB
    subgraph Entities
        BE["BaseEntity<br/>(abstract)"]
        Account["Account"]
        Transaction["Transaction"]
        Currency["Currency"]
        CC["CategoryComponent<br/>(abstract)"]

        BE --> Account
        BE --> Transaction
        BE --> Currency
        BE --> CC
    end

    subgraph CompositePattern["Composite Pattern"]
        CC2["CategoryComponent"]
        MC["MacroCategory"]
        SC["StandardCategory"]

        CC2 --> MC
        CC2 --> SC
        MC -->|"children"| CC2
    end

    subgraph BuilderPattern["Builder Pattern"]
        TB["Transaction.Builder"]
        T["Transaction"]

        TB -->|"build()"| T
    end

    subgraph ObserverPattern["Observer Pattern"]
        BO["BalanceObserver"]
        CBO["ConsoleBalanceObserver"]
        A["Account"]

        BO --> CBO
        A -->|"notifies"| BO
    end
```

## Dettaglio Repository Layer

```mermaid
flowchart TB
    subgraph Singleton
        DS["DataStorage"]
    end

    subgraph Repositories
        AR["Repository&lt;Account&gt;"]
        TR["Repository&lt;Transaction&gt;"]
        CR["Repository&lt;CategoryComponent&gt;"]
        CuR["Repository&lt;Currency&gt;"]
    end

    subgraph Implementation
        IMR["InMemoryRepository&lt;T&gt;<br/>LinkedHashMap storage"]
    end

    DS --> AR
    DS --> TR
    DS --> CR
    DS --> CuR

    AR --> IMR
    TR --> IMR
    CR --> IMR
    CuR --> IMR
```

## Dettaglio Persistence Layer

```mermaid
flowchart TB
    subgraph TemplateMethod["Template Method Pattern"]
        DG["DataGenerator<br/>(abstract)"]
        DDG["DemoDataGenerator"]
        EDG["EmptyDataGenerator"]

        DG --> DDG
        DG --> EDG
    end

    subgraph IO["Java I/O"]
        CSV["CsvService"]
        BR["BufferedReader"]
        BW["BufferedWriter"]

        CSV --> BR
        CSV --> BW
    end

    subgraph Files["File System"]
        ACC[("accounts.csv")]
        TRN[("transactions.csv")]
        CAT[("categories.csv")]
        RAT[("rates.csv")]
    end

    DDG --> CSV
    EDG --> CSV
    CSV --> ACC
    CSV --> TRN
    CSV --> CAT
    CSV --> RAT
```

## Flusso Dati: Aggiunta Transazione

```mermaid
sequenceDiagram
    participant U as User
    participant CLI as CLI/Menu
    participant IV as InputValidator
    participant WS as WalletService
    participant TF as TransactionFactory
    participant TB as Transaction.Builder
    participant A as Account
    participant BO as BalanceObserver
    participant DS as DataStorage
    participant CSV as CsvService

    U->>CLI: Input dati transazione
    CLI->>IV: Validazione input
    IV-->>CLI: Input valido
    CLI->>WS: addTransaction(...)
    WS->>TF: createExpense(...)
    TF->>TB: new Builder()
    TB->>TB: with*() methods
    TB->>TB: build()
    TB-->>TF: Transaction
    TF-->>WS: Transaction
    WS->>A: addTransaction(t)
    A->>A: calcola nuovo saldo
    A->>BO: onBalanceChanged(...)
    A->>BO: onTransactionAdded(...)
    A->>DS: transactionRepository.save(t)
    WS->>CSV: saveAll()
    CSV-->>WS: Saved
    WS-->>CLI: Success
    CLI-->>U: Conferma operazione
```

## Flusso Dati: Trasferimento tra Conti

```mermaid
sequenceDiagram
    participant U as User
    participant WS as WalletService
    participant CM as CurrencyManager
    participant ES as ExchangeStrategy
    participant TF as TransactionFactory
    participant A1 as Account (from)
    participant A2 as Account (to)
    participant CSV as CsvService

    U->>WS: transfer(fromId, toId, amount)
    WS->>WS: Verifica fondi sufficienti

    alt Valute diverse
        WS->>CM: getCurrency(fromCurrencyId)
        WS->>CM: getCurrency(toCurrencyId)
        WS->>ES: convert(amount, from, to, date)
        ES-->>WS: Importo convertito
    end

    WS->>TF: createTransfer(...)
    TF-->>WS: [Transaction out, Transaction in]
    WS->>A1: addTransaction(out)
    WS->>A2: addTransaction(in)
    WS->>CSV: saveAll()
    WS-->>U: Trasferimento completato
```

## Flusso Dati: Generazione Report

```mermaid
sequenceDiagram
    participant U as User
    participant RS as ReportService
    participant DS as DataStorage
    participant A as Account
    participant Stream as Stream API

    U->>RS: getTotalExpenses(accountId, from, to)
    RS->>DS: accountRepository.findById(accountId)
    DS-->>RS: Account
    RS->>A: getTransactions()
    A-->>RS: List<Transaction>
    RS->>Stream: stream()
    Stream->>Stream: filter(type == EXPENSE)
    Stream->>Stream: filter(date in range)
    Stream->>Stream: map(amount)
    Stream->>Stream: reduce(BigDecimal::add)
    Stream-->>RS: BigDecimal totale
    RS-->>U: Totale spese
```

## Pattern Utilizzati per Layer

| Layer | Pattern | Implementazione |
| ----- | ------- | --------------- |
| Presentation | - | Main.java, CLI Menu |
| Service | Factory | TransactionFactory |
| Service | Strategy | ExchangeStrategy |
| Service | Singleton | CurrencyManager |
| Model | Builder | Transaction.Builder |
| Model | Composite | CategoryComponent |
| Model | Iterator | MacroCategory.iterator() |
| Model | Observer | BalanceObserver |
| Repository | Singleton | DataStorage |
| Repository | Generics | Repository<T extends BaseEntity> |
| Persistence | Template Method | DataGenerator |
| Persistence | Java I/O | CsvService |
| Cross-Cutting | Exception Shielding | Package exception/ |
| Cross-Cutting | Logging | AppLogger |

## Tecnologie per Layer

| Layer | Tecnologia | Utilizzo |
| ----- | ---------- | -------- |
| Service | Stream API | ReportService - filter, map, collect, groupingBy |
| Model | Collections | TreeMap (rates), ArrayList (transactions, children) |
| Repository | Collections | LinkedHashMap (storage) |
| Repository | Generics | Repository<T extends BaseEntity> |
| Persistence | Java I/O | BufferedReader/Writer, UTF-8, try-with-resources |
| Cross-Cutting | Logging | java.util.logging via AppLogger |
| Cross-Cutting | Validation | InputValidator - sanitization |
| Testing | JUnit 5 | Test unitari per tutte le componenti |
