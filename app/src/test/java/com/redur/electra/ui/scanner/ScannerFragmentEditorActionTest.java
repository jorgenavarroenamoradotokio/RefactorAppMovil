package com.redur.electra.ui.scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35)
public class ScannerFragmentEditorActionTest {

    private final AtomicInteger reads = new AtomicInteger();

    @Test
    public void accionBuscarDelTeclado_entregaLectura() {
        assertTrue(ScannerFragment.onEditorAction(EditorInfo.IME_ACTION_SEARCH, null, reads::incrementAndGet));

        assertEquals(1, reads.get());
    }

    @Test
    public void introDelEscaner_pulsacionYSuelta_entreganUnaSolaLectura() {
        boolean downConsumed = ScannerFragment.onEditorAction(EditorInfo.IME_ACTION_UNSPECIFIED,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER), reads::incrementAndGet);
        boolean upConsumed = ScannerFragment.onEditorAction(EditorInfo.IME_ACTION_UNSPECIFIED,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER), reads::incrementAndGet);

        assertTrue(downConsumed);
        assertTrue(upConsumed);
        assertEquals(1, reads.get());
    }

    @Test
    public void otraTecla_noEntregaLecturaNiConsumeEvento() {
        assertFalse(ScannerFragment.onEditorAction(EditorInfo.IME_ACTION_UNSPECIFIED,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB), reads::incrementAndGet));

        assertEquals(0, reads.get());
    }

    @Test
    public void otraAccionIme_noEntregaLectura() {
        assertFalse(ScannerFragment.onEditorAction(EditorInfo.IME_ACTION_DONE, null, reads::incrementAndGet));

        assertEquals(0, reads.get());
    }
}
