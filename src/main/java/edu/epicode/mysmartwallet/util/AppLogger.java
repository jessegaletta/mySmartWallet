package edu.epicode.mysmartwallet.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

/**
 * Wrapper per java.util.logging.Logger.
 * Fornisce configurazione centralizzata del logging per l'applicazione MySmartWallet.
 *
 * Il formato dei log è: [DATA] [LIVELLO] [CLASSE] - messaggio
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public final class AppLogger {

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "app.log";
    private static boolean initialized = false;

    /**
     * Costruttore privato per impedire l'istanziazione.
     */
    private AppLogger() {
        throw new UnsupportedOperationException("Classe utility non istanziabile");
    }

    /**
     * Configura il sistema di logging con FileHandler per scrivere su logs/app.log.
     * Deve essere chiamato all'avvio dell'applicazione.
     */
    public static synchronized void setup() {
        if (initialized) {
            return;
        }

        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            Logger rootLogger = Logger.getLogger("");

            // Rimuove handler di default
            for (Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }

            // Configura FileHandler
            FileHandler fileHandler = new FileHandler(
                    LOG_DIR + "/" + LOG_FILE,
                    true  // append
            );
            fileHandler.setFormatter(new CustomFormatter());
            fileHandler.setLevel(Level.ALL);

            // Configura ConsoleHandler
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new CustomFormatter());
            consoleHandler.setLevel(Level.INFO);

            rootLogger.addHandler(fileHandler);
            rootLogger.addHandler(consoleHandler);
            rootLogger.setLevel(Level.ALL);

            initialized = true;
        } catch (IOException e) {
            System.err.println("Impossibile configurare il logging: " + e.getMessage());
        }
    }

    /**
     * Ottiene un Logger per la classe specificata.
     *
     * @param clazz la classe per cui ottenere il logger
     * @return il Logger configurato per la classe
     */
    public static Logger getLogger(Class<?> clazz) {
        if (!initialized) {
            setup();
        }
        return Logger.getLogger(clazz.getName());
    }

    /**
     * Formatter personalizzato per il formato: [DATA] [LIVELLO] [CLASSE] - messaggio
     */
    private static class CustomFormatter extends Formatter {
        private static final DateTimeFormatter DATE_FORMAT =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb.append("[")
              .append(LocalDateTime.now().format(DATE_FORMAT))
              .append("] [")
              .append(record.getLevel().getName())
              .append("] [")
              .append(getSimpleClassName(record.getLoggerName()))
              .append("] - ")
              .append(formatMessage(record))
              .append(System.lineSeparator());

            if (record.getThrown() != null) {
                sb.append("Eccezione: ")
                  .append(record.getThrown().toString())
                  .append(System.lineSeparator());
            }

            return sb.toString();
        }

        private String getSimpleClassName(String fullClassName) {
            if (fullClassName == null) {
                return "Unknown";
            }
            int lastDot = fullClassName.lastIndexOf('.');
            return lastDot > 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
        }
    }
}
