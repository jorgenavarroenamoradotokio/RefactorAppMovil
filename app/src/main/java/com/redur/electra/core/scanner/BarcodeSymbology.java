package com.redur.electra.core.scanner;

/**
 * Tipo de código de barras de una lectura, en la medida en que lo informa el lector. Solo se
 * distingue lo que necesitan las pantallas: Code 128 frente al resto.
 */
public enum BarcodeSymbology {
    /** Code 128, incluido GS1-128. */
    CODE_128,
    /** El lector informa de otro tipo (QR, EAN-13, DataMatrix...). */
    OTHER,
    /** El lector no informa del tipo, p. ej. un escáner que teclea el CB. */
    UNKNOWN
}
