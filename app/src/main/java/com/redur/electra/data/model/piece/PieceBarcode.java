package com.redur.electra.data.model.piece;

import androidx.annotation.NonNull;

import com.redur.electra.core.scanner.BarcodeSymbology;

/**
 * Reglas del CB de un bulto: 29 caracteres impresos en Code 128.
 */
public final class PieceBarcode {

    public static final int LENGTH = 29;
    /** Code 128 codifica el ASCII completo (juegos A, B y C). */
    private static final char CODE_128_MAX_CHAR = 127;

    private PieceBarcode() {
    }

    /**
     * Si el lector informa del tipo, debe ser Code 128. Si no lo informa (escáner por teclado), al
     * menos todos los caracteres deben poder codificarse en Code 128.
     */
    public static boolean isValid(@NonNull String barcode, @NonNull BarcodeSymbology symbology) {
        if (barcode.length() != LENGTH) {
            return false;
        }
        return switch (symbology) {
            case CODE_128 -> true;
            case OTHER -> false;
            case UNKNOWN -> isCode128Encodable(barcode);
        };
    }

    private static boolean isCode128Encodable(@NonNull String barcode) {
        for (int i = 0; i < barcode.length(); i++) {
            if (barcode.charAt(i) > CODE_128_MAX_CHAR) {
                return false;
            }
        }
        return true;
    }
}
