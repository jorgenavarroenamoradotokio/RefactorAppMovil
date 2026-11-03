package com.redur.electra.ui.piece.dock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.redur.electra.core.error.AppError;
import com.redur.electra.core.error.Network;
import com.redur.electra.core.scanner.BarcodeSymbology;
import com.redur.electra.core.ui.UiState;
import com.redur.electra.data.model.piece.dock.Dock;
import com.redur.electra.fake.FakeDockRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;

public class DockViewModelTest {

    private static final String BARCODE = "0123456789ABCDEFGHIJ012345678";
    private static final String OTHER_BARCODE = "9876543210ABCDEFGHIJ876543210";
    private static final Dock DOCK = new Dock("Z14", "ZAR", "50197", "España");

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutor = new InstantTaskExecutorRule();

    private SavedStateHandle savedState;
    private FakeDockRepository repository;
    private DockViewModel viewModel;

    @Before
    public void setUp() {
        savedState = new SavedStateHandle();
        repository = new FakeDockRepository();
        viewModel = new DockViewModel(savedState, repository);
    }

    @Test
    public void alIniciar_esperaLectura() {
        assertTrue(viewModel.getDockState().getValue() instanceof UiState.Idle);
        assertTrue(repository.requests.isEmpty());
    }

    @Test
    public void leerBulto_consultaElMuelleYGuardaElCb() {
        viewModel.onBarcodeScanned("  " + BARCODE + " ", BarcodeSymbology.CODE_128);

        assertTrue(viewModel.getDockState().getValue() instanceof UiState.Loading);
        assertEquals(BARCODE, repository.lastRequest().barcode);
        assertEquals(BARCODE, savedState.get(DockViewModel.KEY_BARCODE));
    }

    @Test
    public void respuestaConMuelle_loMuestra() {
        viewModel.onBarcodeScanned(BARCODE, BarcodeSymbology.CODE_128);

        repository.lastRequest().succeed(Optional.of(DOCK));

        assertEquals(success(new DockState.Found(BARCODE, DOCK)), viewModel.getDockState().getValue());
    }

    @Test
    public void respuestaSinMuelle_avisaDeQueNoSeEncuentra() {
        viewModel.onBarcodeScanned(BARCODE, BarcodeSymbology.CODE_128);

        repository.lastRequest().succeed(Optional.empty());

        assertEquals(success(new DockState.NotFound(BARCODE)), viewModel.getDockState().getValue());
    }

    @Test
    public void cbConLongitudIncorrecta_seRechazaSinConsultar() {
        String shortBarcode = BARCODE.substring(1);

        viewModel.onBarcodeScanned(shortBarcode, BarcodeSymbology.CODE_128);

        assertEquals(success(new DockState.InvalidBarcode(shortBarcode)), viewModel.getDockState().getValue());
        assertTrue(repository.requests.isEmpty());
        assertNull(savedState.get(DockViewModel.KEY_BARCODE));
    }

    @Test
    public void cbQueNoEsCode128_seRechazaSinConsultar() {
        viewModel.onBarcodeScanned(BARCODE, BarcodeSymbology.OTHER);

        assertEquals(success(new DockState.InvalidBarcode(BARCODE)), viewModel.getDockState().getValue());
        assertTrue(repository.requests.isEmpty());
    }

    @Test
    public void cbTecleadoSinTipo_seConsultaSiCumpleLaLongitud() {
        viewModel.onBarcodeScanned(BARCODE, BarcodeSymbology.UNKNOWN);

        assertEquals(BARCODE, repository.lastRequest().barcode);
    }

    @Test
    public void cbNoValidoConConsultaEnCurso_laCancela() {
        viewModel.onBarcodeScanned(BARCODE, BarcodeSymbology.CODE_128);
        FakeDockRepository.Request pending = repository.lastRequest();

        viewModel.onBarcodeScanned("B0001", BarcodeSymbology.CODE_128);

        assertTrue(pending.isCancelled());
    }

    @Test
    public void cbVacio_seIgnora() {
        viewModel.onBarcodeScanned("   ", BarcodeSymbology.CODE_128);
        viewModel.onBarcodeScanned(null, BarcodeSymbology.CODE_128);

        assertTrue(viewModel.getDockState().getValue() instanceof UiState.Idle);
        assertNull(savedState.get(DockViewModel.KEY_BARCODE));
    }

    @Test
    public void leerOtroBulto_cancelaLaConsultaAnteriorYLaSustituye() {
        viewModel.onBarcodeScanned(BARCODE, BarcodeSymbology.CODE_128);
        FakeDockRepository.Request first = repository.lastRequest();

        viewModel.onBarcodeScanned(OTHER_BARCODE, BarcodeSymbology.CODE_128);
        repository.lastRequest().succeed(Optional.of(DOCK));

        assertTrue(first.isCancelled());
        assertEquals(success(new DockState.Found(OTHER_BARCODE, DOCK)), viewModel.getDockState().getValue());
    }

    @Test
    public void cancelar_vuelveAEsperarOlvidaElCbYCancelaLaConsulta() {
        viewModel.onBarcodeScanned(BARCODE, BarcodeSymbology.CODE_128);

        viewModel.onCancelClicked();

        assertTrue(viewModel.getDockState().getValue() instanceof UiState.Idle);
        assertNull(savedState.get(DockViewModel.KEY_BARCODE));
        assertTrue(repository.lastRequest().isCancelled());
    }

    @Test
    public void fallo_muestraErrorYReintentarRepiteLaConsulta() {
        viewModel.onBarcodeScanned(BARCODE, BarcodeSymbology.CODE_128);
        repository.lastRequest().fail(new AppError.NetworkError(Network.TIMEOUT));
        assertTrue(viewModel.getDockState().getValue() instanceof UiState.Error);

        viewModel.onRetryClicked();

        assertEquals(2, repository.requests.size());
        assertEquals(BARCODE, repository.lastRequest().barcode);
        assertTrue(viewModel.getDockState().getValue() instanceof UiState.Loading);
    }

    @Test
    public void reintentarSinFallo_noRepiteLaConsulta() {
        viewModel.onBarcodeScanned(BARCODE, BarcodeSymbology.CODE_128);
        repository.lastRequest().succeed(Optional.of(DOCK));
        UiState<DockState> before = viewModel.getDockState().getValue();

        viewModel.onRetryClicked();

        assertSame(before, viewModel.getDockState().getValue());
        assertEquals(1, repository.requests.size());
    }

    @Test
    public void trasMuerteDelProceso_vuelveAConsultarElMuelle() {
        SavedStateHandle restored = new SavedStateHandle();
        restored.set(DockViewModel.KEY_BARCODE, BARCODE);
        FakeDockRepository restoredRepository = new FakeDockRepository();

        DockViewModel recreated = new DockViewModel(restored, restoredRepository);
        restoredRepository.lastRequest().succeed(Optional.of(DOCK));

        assertEquals(success(new DockState.Found(BARCODE, DOCK)), recreated.getDockState().getValue());
    }

    @Test
    public void respuestaTrasTerminar_noHayConsultaPendiente() {
        viewModel.onBarcodeScanned(BARCODE, BarcodeSymbology.CODE_128);
        FakeDockRepository.Request request = repository.lastRequest();
        request.succeed(Optional.of(DOCK));

        viewModel.onCancelClicked();

        assertFalse(request.isCancelled());
    }

    private static UiState<DockState> success(DockState state) {
        return new UiState.Success<>(state);
    }
}
