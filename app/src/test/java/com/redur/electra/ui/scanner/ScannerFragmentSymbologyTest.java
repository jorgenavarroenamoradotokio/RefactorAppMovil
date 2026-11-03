package com.redur.electra.ui.scanner;

import static org.junit.Assert.assertEquals;

import com.redur.electra.core.scanner.BarcodeSymbology;

import org.junit.Test;

public class ScannerFragmentSymbologyTest {

    @Test
    public void formatoCode128DeLaCamara_esCode128() {
        assertEquals(BarcodeSymbology.CODE_128, ScannerFragment.toSymbology("CODE_128"));
    }

    @Test
    public void otroFormatoDeLaCamara_esOtro() {
        assertEquals(BarcodeSymbology.OTHER, ScannerFragment.toSymbology("QR_CODE"));
    }

    @Test
    public void sinFormato_esDesconocido() {
        assertEquals(BarcodeSymbology.UNKNOWN, ScannerFragment.toSymbology(null));
    }
}
