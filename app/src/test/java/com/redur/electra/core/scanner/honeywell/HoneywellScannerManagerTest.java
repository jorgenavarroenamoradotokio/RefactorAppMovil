package com.redur.electra.core.scanner.honeywell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Intent;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ApplicationProvider;

import com.redur.electra.core.scanner.BarcodeSymbology;
import com.redur.electra.fake.FakeLifecycleOwner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)
public class HoneywellScannerManagerTest {

    private final List<String> scans = new ArrayList<>();
    private final List<BarcodeSymbology> symbologies = new ArrayList<>();
    private Application context;
    private FakeLifecycleOwner owner;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        owner = new FakeLifecycleOwner();
        owner.getLifecycle().addObserver(new HoneywellScannerManager(context, this::onScan));
        owner.moveTo(Lifecycle.State.RESUMED);
    }

    @Test
    public void lecturaComoTexto_seEntregaAlListener() {
        send(new Intent(HoneywellScannerManager.ACTION)
                .putExtra(HoneywellScannerManager.EXTRA_DATA, "B0001"));

        assertEquals(List.of("B0001"), scans);
    }

    @Test
    public void lecturaSoloEnBytes_seDecodificaConElCharsetIndicado() {
        send(new Intent(HoneywellScannerManager.ACTION)
                .putExtra(HoneywellScannerManager.EXTRA_DATA_BYTES, "Ñ-01".getBytes(StandardCharsets.ISO_8859_1))
                .putExtra(HoneywellScannerManager.EXTRA_CHARSET, "ISO-8859-1"));

        assertEquals(List.of("Ñ-01"), scans);
    }

    @Test
    public void charsetDesconocido_seDecodificaComoUtf8() {
        send(new Intent(HoneywellScannerManager.ACTION)
                .putExtra(HoneywellScannerManager.EXTRA_DATA_BYTES, "Ñ-01".getBytes(StandardCharsets.UTF_8))
                .putExtra(HoneywellScannerManager.EXTRA_CHARSET, "no-existe"));

        assertEquals(List.of("Ñ-01"), scans);
    }

    @Test
    public void intentSinCodigo_noLlegaAlListener() {
        send(new Intent(HoneywellScannerManager.ACTION));

        assertTrue(scans.isEmpty());
    }

    @Test
    public void enPausa_noSeEntreganLecturas() {
        owner.moveTo(Lifecycle.State.STARTED);

        send(new Intent(HoneywellScannerManager.ACTION)
                .putExtra(HoneywellScannerManager.EXTRA_DATA, "B0001"));

        assertTrue(scans.isEmpty());
    }

    @Test
    public void alVolverDePausa_seEntreganLecturasUnaSolaVez() {
        owner.moveTo(Lifecycle.State.STARTED);
        owner.moveTo(Lifecycle.State.RESUMED);

        send(new Intent(HoneywellScannerManager.ACTION)
                .putExtra(HoneywellScannerManager.EXTRA_DATA, "B0001"));

        assertEquals(List.of("B0001"), scans);
    }

    @Test
    public void aimIdDeCode128_seEntregaComoCode128() {
        send(new Intent(HoneywellScannerManager.ACTION)
                .putExtra(HoneywellScannerManager.EXTRA_DATA, "B0001")
                .putExtra(HoneywellScannerManager.EXTRA_AIM_ID, "]C0"));

        assertEquals(List.of(BarcodeSymbology.CODE_128), symbologies);
    }

    @Test
    public void aimIdDeOtroTipo_seEntregaComoOtro() {
        send(new Intent(HoneywellScannerManager.ACTION)
                .putExtra(HoneywellScannerManager.EXTRA_DATA, "B0001")
                .putExtra(HoneywellScannerManager.EXTRA_AIM_ID, "]Q1")
                .putExtra(HoneywellScannerManager.EXTRA_CODE_ID, "j"));

        assertEquals(List.of(BarcodeSymbology.OTHER), symbologies);
    }

    @Test
    public void sinAimId_seUsaElCodeIdDeHoneywell() {
        send(new Intent(HoneywellScannerManager.ACTION)
                .putExtra(HoneywellScannerManager.EXTRA_DATA, "B0001")
                .putExtra(HoneywellScannerManager.EXTRA_CODE_ID, "j"));

        assertEquals(List.of(BarcodeSymbology.CODE_128), symbologies);
    }

    @Test
    public void sinTipoInformado_seEntregaComoDesconocido() {
        send(new Intent(HoneywellScannerManager.ACTION)
                .putExtra(HoneywellScannerManager.EXTRA_DATA, "B0001"));

        assertEquals(List.of(BarcodeSymbology.UNKNOWN), symbologies);
    }

    private void onScan(String barcode, BarcodeSymbology symbology) {
        scans.add(barcode);
        symbologies.add(symbology);
    }

    private void send(Intent intent) {
        context.sendBroadcast(intent);
        shadowOf(Looper.getMainLooper()).idle();
    }
}
