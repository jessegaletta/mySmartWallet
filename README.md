# MySmartWallet

## Panoramica

**MySmartWallet** è un'applicazione Java SE a riga di comando per la gestione della contabilità personale.
Sviluppata seguendo i principi della programmazione orientata agli oggetti, implementa numerosi design pattern
e best practice di sviluppo software per garantire manutenibilità, testabilità e sicurezza.

L'applicazione permette di gestire conti multipli in valute diverse, registrare transazioni con categorie
gerarchiche, effettuare trasferimenti con conversione automatica e generare report dettagliati.

## Funzionalità

- **Gestione Multi-Conto**: Creazione e gestione di conti multipli (conto corrente, contanti, PayPal, etc.)
- **Supporto Multi-Valuta**: Valute diverse (EUR, USD, GBP) con storico dei tassi di cambio nel tempo
- **Registrazione Transazioni**: Entrate, uscite e trasferimenti con descrizioni dettagliate e categorizzazione
- **Categorie Gerarchiche**: Sistema di categorie organizzate ad albero (es: Spese > Cibo > Ristoranti)
- **Conversione Automatica**: Conversione valuta automatica nei trasferimenti tra conti con valute diverse
- **Report e Statistiche**: Report filtrabili per periodo, categoria e tipo di transazione
- **Notifiche Real-time**: Notifiche immediate su variazioni di saldo e transazioni aggiunte
- **Persistenza CSV**: Salvataggio automatico dei dati su file CSV

## Tecnologie e Pattern Utilizzati

### Design Pattern Standard

| Pattern | Implementazione | Descrizione |
| ------- | --------------- | ----------- |
| **Factory** | `TransactionFactory` | Centralizza la creazione delle transazioni, nascondendo la complessità del processo di costruzione e validazione. Gestisce automaticamente l'assegnazione degli ID e supporta parametri opzionali per la conversione valuta. |
| **Composite** | `CategoryComponent`, `MacroCategory`, `StandardCategory` | Permette di trattare uniformemente categorie singole (foglie) e raggruppamenti di categorie (nodi). Consente gerarchie arbitrariamente profonde senza modificare il codice client. |
| **Iterator** | `MacroCategory.iterator()` con classe interna `CategoryIterator` | Implementa un attraversamento Depth-First Search (DFS) usando uno Stack, permettendo di iterare su gerarchie di qualsiasi profondità senza ricorsione esplicita. |
| **Exception Shielding** | Package `exception/` con `WalletException` e sottoclassi | Le eccezioni Java native vengono intercettate e wrappate in eccezioni di dominio personalizzate, fornendo messaggi comprensibili all'utente e impedendo la visualizzazione di stack trace tecnici. |

### Tecnologie Core

| Tecnologia | Implementazione | Descrizione |
| ---------- | --------------- | ----------- |
| **Collections** | `TreeMap`, `LinkedHashMap`, `ArrayList` | `TreeMap` per tassi di cambio ordinati per data con ricerca `floorEntry()`, `LinkedHashMap` per iterazione predicibile, `ArrayList` per transazioni e figli delle categorie. |
| **Generics** | `Repository<T extends BaseEntity>` | Repository generico type-safe riutilizzato per Account, Transaction, Currency e CategoryComponent senza necessità di cast. |
| **Java I/O** | `CsvService` con `BufferedReader/Writer` | I/O bufferizzato con encoding UTF-8, try-with-resources per rilascio automatico risorse, Path API moderna. |
| **Logging** | `AppLogger` con `java.util.logging` | Logging centralizzato con livelli (SEVERE, WARNING, INFO, FINE), output su file e console con formattazione personalizzata. |
| **JUnit Testing** | Test unitari in `src/test/java/` | Test per Builder, Composite, Repository, MoneyUtil, InputValidator, CsvService, ReportService, Strategy, Factory. |

### Programmazione Sicura

| Pratica | Implementazione | Descrizione |
| ------- | --------------- | ----------- |
| **Input Sanitization** | `InputValidator` | Ogni input utente viene validato prima dell'elaborazione: importi positivi, campi non vuoti, ID validi, parsing sicuro con normalizzazione. |
| **No Hardcoded Secrets** | - | Nessuna credenziale, chiave API o dato sensibile nel codice sorgente. Tutte le configurazioni sono parametrizzate tramite costanti. |
| **Controlled Exception Propagation** | Package `exception/` | Le eccezioni Java vengono sempre intercettate e wrappate in eccezioni di dominio. L'utente non vede mai stack trace o messaggi tecnici. |

### Design Pattern Opzionali

| Pattern | Implementazione | Descrizione |
| ------- | --------------- | ----------- |
| **Builder** | `Transaction.Builder` (classe interna statica) | Costruzione fluida di oggetti Transaction con molti campi, validazione centralizzata in `build()`, garantisce immutabilità dell'oggetto risultante. |
| **Strategy** | `ExchangeStrategy`, `HistoricalExchangeStrategy`, `FixedExchangeStrategy` | L'algoritmo di conversione valuta è intercambiabile a runtime: tassi storici in produzione, tassi fissi nei test, estendibile con API real-time. |
| **Singleton** | `DataStorage`, `CurrencyManager` | Accesso globale controllato ai dati con inizializzazione lazy thread-safe. `resetInstance()` permette isolamento nei test. |
| **Observer** | `BalanceObserver`, `ConsoleBalanceObserver` | Account notifica gli observer su variazioni saldo e nuove transazioni. Disaccoppia la logica di business dalle reazioni agli eventi. |
| **Template Method** | `DataGenerator`, `DemoDataGenerator`, `EmptyDataGenerator` | Lo scheletro dell'algoritmo di generazione dati è fisso, le sottoclassi definiscono i dettagli (dati demo o struttura vuota). |

### Tecnologie Opzionali

| Tecnologia | Implementazione | Descrizione |
| ---------- | --------------- | ----------- |
| **Stream API & Lambdas** | `ReportService` | Operazioni di filtraggio, trasformazione e aggregazione dichiarative: `filter()` per tipo/data/categoria, `map()` per trasformazioni, `collect()` con `groupingBy()` per raggruppamenti, `reduce()` per totali. |

## Struttura Progetto

```
edu.epicode.mysmartwallet/
├── model/                    # Entità base
│   ├── BaseEntity.java       # Classe astratta con int id
│   ├── Account.java          # Conto con valuta e transazioni
│   ├── Transaction.java      # Transazione immutabile (Builder)
│   ├── TransactionType.java  # Enum: INCOME, EXPENSE, TRANSFER
│   ├── Currency.java         # Valuta con storico tassi
│   └── category/             # Composite pattern
│       ├── CategoryComponent.java   # Classe astratta
│       ├── MacroCategory.java       # Nodo con figli
│       └── StandardCategory.java    # Foglia
│
├── repository/               # Data layer
│   ├── Repository.java       # Interfaccia generica
│   ├── InMemoryRepository.java # Implementazione con Map
│   └── DataStorage.java      # Singleton container
│
├── service/                  # Business logic
│   ├── WalletService.java    # Gestione conti
│   ├── ReportService.java    # Report con Stream API
│   ├── CurrencyManager.java  # Singleton gestione valute
│   ├── factory/
│   │   └── TransactionFactory.java
│   ├── strategy/
│   │   ├── ExchangeStrategy.java
│   │   ├── HistoricalExchangeStrategy.java
│   │   └── FixedExchangeStrategy.java
│   └── observer/
│       ├── BalanceObserver.java
│       └── ConsoleBalanceObserver.java
│
├── persistence/              # I/O layer
│   ├── CsvService.java       # Lettura/scrittura CSV
│   └── generator/
│       ├── DataGenerator.java
│       ├── DemoDataGenerator.java
│       └── EmptyDataGenerator.java
│
├── exception/                # Eccezioni custom
│   ├── WalletException.java
│   ├── InsufficientFundsException.java
│   ├── ItemNotFoundException.java
│   ├── InvalidInputException.java
│   ├── RateNotFoundException.java
│   └── StorageException.java
│
└── util/                     # Utility
    ├── AppLogger.java
    ├── MoneyUtil.java
    └── InputValidator.java
```

## Setup e Esecuzione

### Prerequisiti

| Requisito | Versione |
| --------- | -------- |
| Java JDK  | 21+      |
| Gradle    | 8+       |

### Comandi

| Comando * | Descrizione |
| ------- | ----------- |
| `./gradlew build` | Compila il progetto e esegue i test |
| `./gradlew test` | Esegue solo i test unitari |
| `./gradlew run --console=plain` | Avvia l'applicazione CLI |
| `./gradlew clean` | Pulisce i file di build |
| `./gradlew javadoc` | Genera la documentazione JavaDoc |

*: *Su Windows, ometti `./` davanti al comando (es. `gradlew build`).*

### Primo Avvio

Al primo avvio, l'applicazione chiederà se generare dati demo o partire con un database vuoto:

```
Nessun dato trovato. Vuoi generare dati demo? (s/n)
```

- **s**: Crea conti, categorie e transazioni di esempio (DemoDataGenerator)
- **n**: Crea solo la struttura minima con valuta EUR e categorie base (EmptyDataGenerator)

### Directory di Lavoro

```
mySmartWallet/
├── data/                    # File CSV (generati a runtime)
│   ├── accounts.csv         # Dati conti
│   ├── transactions.csv     # Dati transazioni
│   ├── categories.csv       # Dati categorie
│   └── rates.csv            # Tassi di cambio
├── logs/                    # File di log (generati a runtime)
│   └── app.log
└── docs/                    # Documentazione e diagrammi UML
```

## Diagrammi UML

- **Class Diagram**: [docs/class_diagram.md](docs/class_diagram.md) - Diagramma delle classi con tutti i pattern implementati
- **Architecture Diagram**: [docs/architecture_diagram.md](docs/architecture_diagram.md) - Architettura a strati e flussi dati

## Scelte di Design

### Perché BigDecimal per gli importi?

I tipi primitivi `float` e `double` usano la rappresentazione in virgola mobile
IEEE 754, che non può rappresentare esattamente molti valori decimali comuni.
Ad esempio, `0.1 + 0.2` in double non è esattamente `0.3` ma
`0.30000000000000004`. In un'applicazione finanziaria, questi errori di
arrotondamento possono accumularsi e causare discrepanze significative nei
bilanci. `BigDecimal` usa una rappresentazione decimale esatta, garantendo
precisione assoluta nei calcoli monetari. La classe `MoneyUtil` centralizza
tutte le operazioni BigDecimal con scale (2 decimali) e arrotondamento
(HALF_UP) consistenti.

### Perché Factory per le transazioni?

Le transazioni richiedono validazione degli input, assegnazione automatica degli ID
e possono avere parametri opzionali per la conversione valuta. La `TransactionFactory`
incapsula questa logica complessa, garantendo che ogni transazione creata sia
valida e consistente. Fornisce metodi specializzati per ogni tipo di transazione
(`createIncome`, `createExpense`, `createTransfer`) nascondendo l'uso interno
del Builder.

### Perché Composite per le categorie?

Il pattern Composite permette di trattare uniformemente categorie singole
(StandardCategory) e raggruppamenti di categorie (MacroCategory). Questo
consente di creare gerarchie arbitrariamente profonde (es: Spese > Cibo >
Ristoranti > Fast Food) senza modificare il codice client. L'iteratore
integrato permette di attraversare l'intera gerarchia in modo trasparente,
e il metodo `getFullPath()` restituisce il percorso completo della categoria.

### Perché Iterator per la navigazione delle categorie?

La struttura Composite richiede un meccanismo per visitare tutti i nodi.
L'Iterator implementa un attraversamento Depth-First Search (DFS) usando
uno Stack interno, evitando ricorsione esplicita. Questo permette di usare
il for-each su qualsiasi `MacroCategory`, iterando automaticamente su tutta
la gerarchia sottostante.

### Perché Strategy per la conversione valuta?

Il pattern Strategy disaccoppia l'algoritmo di conversione dal suo utilizzo.
Questo permette di:

- Usare tassi storici in produzione (`HistoricalExchangeStrategy`)
- Usare tassi fissi nei test (`FixedExchangeStrategy`) per risultati deterministici
- Aggiungere facilmente nuove strategie (es: API real-time) senza modificare
  il codice esistente

### Perché Observer per le notifiche di saldo?

Il pattern Observer disaccoppia la classe Account dalle reazioni agli eventi.
Quando una transazione viene aggiunta, Account notifica tutti gli observer
registrati senza sapere cosa faranno. `ConsoleBalanceObserver` stampa le
variazioni di saldo e avvisa in caso di saldo negativo. È facile aggiungere
nuovi observer (email, SMS, webhook) senza modificare Account.

### Perché Builder per le transazioni?

Transaction ha molti campi, alcuni opzionali (importo originale, valuta originale,
tasso di cambio per le conversioni). Il Builder permette costruzione fluida
con metodi `with*()`, validazione centralizzata nel metodo `build()`, e
garantisce l'immutabilità dell'oggetto risultante (tutti i campi `final`,
nessun setter).

### Perché Template Method per la generazione dati?

Lo scheletro dell'algoritmo di generazione (valute → categorie → account →
transazioni → salvataggio) è fisso, ma i dettagli variano. `DemoDataGenerator`
crea dati di esempio, `EmptyDataGenerator` crea solo la struttura minima.
Il metodo `generate()` è `final` per garantire la sequenza corretta, le
sottoclassi implementano solo i metodi astratti.

### Perché Exception Shielding?

L'utente non deve mai vedere stack trace Java grezzi o messaggi tecnici.
Ogni eccezione Java (`IOException`, `NumberFormatException`, etc.) viene
intercettata e wrappata in un'eccezione di dominio (`StorageException`,
`InvalidInputException`, etc.) con un messaggio comprensibile. Questo permette
anche una gestione stratificata degli errori.

## Limitazioni Note

- **Interfaccia CLI**: L'applicazione non dispone di interfaccia grafica, solo riga di comando
- **Persistenza CSV**: Nessun supporto per database relazionali; i dati sono salvati in file CSV
- **Single-User**: Non esiste sistema di autenticazione o supporto multi-utente
- **Tassi Manuali**: I tassi di cambio devono essere inseriti manualmente; nessuna integrazione con API esterne
- **Valute Limitate**: Supporto pre-configurato solo per EUR, USD, GBP (estendibile manualmente)
- **Nessun Backup Automatico**: Non esiste sistema automatico di backup dei dati

## Sviluppi Futuri

- **Interfaccia Grafica**: Sviluppo GUI con JavaFX o applicazione web
- **Database Relazionale**: Migrazione da CSV a database per maggiore robustezza
- **API Tassi Real-time**: Integrazione con API esterne per tassi di cambio aggiornati automaticamente
- **Export PDF**: Generazione report in formato PDF per stampa e archiviazione
- **Grafici Statistiche**: Visualizzazione grafica di entrate, uscite e andamenti temporali
- **Budget Planning**: Funzionalità di pianificazione budget mensile con avvisi
- **Multi-Utente**: Sistema di autenticazione e profili utente separati
- **Cloud Sync**: Sincronizzazione dati su cloud per accesso multi-dispositivo

## Autore

Jesse Galetta
