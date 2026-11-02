package com.redur.electra.ui.piece.dock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.redur.electra.core.ui.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DockViewModelTest {

    private static final String BARCODE = "123456789";
    private static final String OTHER_BARCODE = "987654321";

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutor = new InstantTaskExecutorRule();

    private SavedStateHandle savedState;
    private DockViewModel viewModel;

    @Before
    public void setUp() {
        savedState = new SavedStateHandle();
        viewModel = new DockViewModel(savedState);
    }

    @Test
    public void alIniciar_esperaLectura() {
        assertTrue(viewModel.getDockState().getValue() instanceof UiState.Idle);
    }

    @Test
    public void leerBulto_muestraSuMuelleYGuardaElCb() {
        viewModel.onBarcodeScanned("  " + BARCODE + " ");

        assertEquals(found(BARCODE), viewModel.getDockState().getValue());
        assertEquals(BARCODE, savedState.get(DockViewModel.KEY_BARCODE));
    }

    @Test
    public void cbVacio_seIgnora() {
        viewModel.onBarcodeScanned("   ");
        viewModel.onBarcodeScanned(null);

        assertTrue(viewModel.getDockState().getValue() instanceof UiState.Idle);
        assertNull(savedState.get(DockViewModel.KEY_BARCODE));
    }

    @Test
    public void leerOtroBulto_sustituyeAlAnterior() {
        viewModel.onBarcodeScanned(BARCODE);

        viewModel.onBarcodeScanned(OTHER_BARCODE);

        assertEquals(found(OTHER_BARCODE), viewModel.getDockState().getValue());
    }

    @Test
    public void cancelar_vuelveAEsperarYOlvidaElCb() {
        viewModel.onBarcodeScanned(BARCODE);

        viewModel.onCancelClicked();

        assertTrue(viewModel.getDockState().getValue() instanceof UiState.Idle);
        assertNull(savedState.get(DockViewModel.KEY_BARCODE));
    }

    @Test
    public void reintentarSinFallo_noRepiteLaConsulta() {
        viewModel.onBarcodeScanned(BARCODE);
        UiState<DockState> before = viewModel.getDockState().getValue();

        viewModel.onRetryClicked();

        assertSame(before, viewModel.getDockState().getValue());
    }

    @Test
    public void trasMuerteDelProceso_recuperaElMuelle() {
        SavedStateHandle restored = new SavedStateHandle();
        restored.set(DockViewModel.KEY_BARCODE, BARCODE);

        DockViewModel recreated = new DockViewModel(restored);

        assertEquals(found(BARCODE), recreated.getDockState().getValue());
    }

    private static UiState<DockState> found(String barcode) {
        return new UiState.Success<>(new DockState.Found(barcode, DockViewModel.MOCK_DOCK));
    }
}
