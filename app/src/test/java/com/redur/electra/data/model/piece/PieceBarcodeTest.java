package com.redur.electra.data.model.piece;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.redur.electra.core.scanner.BarcodeSymbology;

import org.junit.Test;

public class PieceBarcodeTest {

    private static final String VALID = "0123456789ABCDEFGHIJ012345678";

    @Test
    public void code128De29Caracteres_esValido() {
        assertTrue(PieceBarcode.isValid(VALID, BarcodeSymbology.CODE_128));
    }

    @Test
    public void longitudDistintaDe29_noEsValido() {
        assertFalse(PieceBarcode.isValid(VALID.substring(1), BarcodeSymbology.CODE_128));
        assertFalse(PieceBarcode.isValid(VALID + "9", BarcodeSymbology.CODE_128));
    }

    @Test
    public void otroTipoDeCodigo_noEsValidoAunqueTenga29Caracteres() {
        assertFalse(PieceBarcode.isValid(VALID, BarcodeSymbology.OTHER));
    }

    @Test
    public void tipoDesconocido_esValidoSiCode128PuedeCodificarlo() {
        assertTrue(PieceBarcode.isValid(VALID, BarcodeSymbology.UNKNOWN));
    }

    @Test
    public void tipoDesconocido_conCaracteresFueraDeAscii_noEsValido() {
        String withAccent = "Ñ" + VALID.substring(1);

        assertFalse(PieceBarcode.isValid(withAccent, BarcodeSymbology.UNKNOWN));
    }
}
