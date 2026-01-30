package edu.epicode.mysmartwallet.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Classe utility per operazioni sicure su importi monetari.
 * Utilizza BigDecimal per garantire precisione nei calcoli finanziari.
 *
 * <p>Tutte le operazioni rispettano la scala e l'arrotondamento configurati
 * per evitare errori di precisione tipici dei tipi floating-point.</p>
 *
 * @author Jesse Galetta
 * @version 1.0
 */
public final class MoneyUtil {

    /**
     * Valore zero per confronti e inizializzazioni.
     */
    public static final BigDecimal ZERO = BigDecimal.ZERO;

    /**
     * Scala di default per gli importi monetari (2 decimali).
     */
    public static final int DEFAULT_SCALE = 2;

    /**
     * Modalità di arrotondamento di default (arrotondamento commerciale).
     */
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    /**
     * Costruttore privato per impedire l'istanziazione.
     */
    private MoneyUtil() {
        throw new UnsupportedOperationException("Classe utility non istanziabile");
    }

    /**
     * Crea un BigDecimal da una stringa.
     *
     * @param amount la stringa rappresentante l'importo
     * @return il BigDecimal corrispondente con scala corretta
     * @throws NumberFormatException se la stringa non è un numero valido
     */
    public static BigDecimal of(String amount) {
        return new BigDecimal(amount).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Crea un BigDecimal da un valore double.
     * Utilizza valueOf per evitare problemi di precisione.
     *
     * @param amount il valore double
     * @return il BigDecimal corrispondente con scala corretta
     */
    public static BigDecimal of(double amount) {
        return BigDecimal.valueOf(amount).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Somma due importi.
     *
     * @param a il primo addendo
     * @param b il secondo addendo
     * @return la somma dei due importi
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Sottrae il secondo importo dal primo.
     *
     * @param a il minuendo
     * @param b il sottraendo
     * @return la differenza (a - b)
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return a.subtract(b).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Moltiplica due importi.
     *
     * @param a il primo fattore
     * @param b il secondo fattore
     * @return il prodotto con scala corretta
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return a.multiply(b).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Divide il primo importo per il secondo.
     *
     * @param a il dividendo
     * @param b il divisore
     * @return il quoziente con scala e arrotondamento corretti
     * @throws ArithmeticException se il divisore è zero
     */
    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return a.divide(b, DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Formatta un importo con il simbolo della valuta.
     *
     * @param amount l'importo da formattare
     * @param symbol il simbolo della valuta (es: "€", "$")
     * @return la stringa formattata (es: "100.00 €")
     */
    public static String format(BigDecimal amount, String symbol) {
        return amount.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING).toPlainString() + " " + symbol;
    }

    /**
     * Verifica se l'importo è positivo.
     *
     * @param amount l'importo da verificare
     * @return true se l'importo è maggiore di zero
     */
    public static boolean isPositive(BigDecimal amount) {
        return amount.compareTo(ZERO) > 0;
    }

    /**
     * Verifica se l'importo è negativo.
     *
     * @param amount l'importo da verificare
     * @return true se l'importo è minore di zero
     */
    public static boolean isNegative(BigDecimal amount) {
        return amount.compareTo(ZERO) < 0;
    }

    /**
     * Verifica se il primo importo è maggiore del secondo.
     *
     * @param a il primo importo
     * @param b il secondo importo
     * @return true se a è maggiore di b
     */
    public static boolean isGreaterThan(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) > 0;
    }

    // ==================== OPERAZIONI SU TASSI DI CAMBIO ====================

    /**
     * Crea un BigDecimal per un tasso di cambio da una stringa.
     * Non applica arrotondamento per preservare la precisione originale.
     *
     * @param rate la stringa rappresentante il tasso
     * @return il BigDecimal corrispondente senza arrotondamento
     * @throws NumberFormatException se la stringa non è un numero valido
     */
    public static BigDecimal ofRate(String rate) {
        return new BigDecimal(rate);
    }

    /**
     * Crea un BigDecimal per un tasso di cambio da un valore double.
     * Non applica arrotondamento per preservare la precisione originale.
     *
     * @param rate il valore double del tasso
     * @return il BigDecimal corrispondente senza arrotondamento
     */
    public static BigDecimal ofRate(double rate) {
        return BigDecimal.valueOf(rate);
    }

    /**
     * Divide due tassi di cambio mantenendo la massima precisione.
     * Utilizza MathContext.DECIMAL128 per evitare perdita di precisione.
     *
     * @param a il dividendo (tasso)
     * @param b il divisore (tasso)
     * @return il quoziente con precisione completa
     * @throws ArithmeticException se il divisore è zero
     */
    public static BigDecimal divideRates(BigDecimal a, BigDecimal b) {
        return a.divide(b, java.math.MathContext.DECIMAL128);
    }

    /**
     * Moltiplica un importo monetario per un tasso di cambio.
     * Il risultato viene arrotondato a 2 decimali perché rappresenta denaro.
     *
     * @param amount l'importo da convertire
     * @param rate   il tasso di cambio
     * @return l'importo convertito arrotondato alla scala monetaria
     */
    public static BigDecimal multiplyByRate(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }
}
