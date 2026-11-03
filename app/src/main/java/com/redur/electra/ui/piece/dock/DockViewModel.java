package com.redur.electra.ui.piece.dock;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.redur.electra.core.asyn.Cancellable;
import com.redur.electra.core.asyn.ResultCallback;
import com.redur.electra.core.error.AppError;
import com.redur.electra.core.scanner.BarcodeSymbology;
import com.redur.electra.core.ui.ErrorUiMapper;
import com.redur.electra.core.ui.UiState;
import com.redur.electra.data.model.piece.PieceBarcode;
import com.redur.electra.data.model.piece.dock.Dock;
import com.redur.electra.data.repository.piece.dock.DockRepository;

import java.util.Optional;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import timber.log.Timber;

/**
 * Puerta de salida: por cada bulto leído se muestra su muelle hasta leer el siguiente o cancelar.
 * El CB leído se guarda en {@link SavedStateHandle} para recuperar el muelle si el sistema mata
 * el proceso.
 */
@HiltViewModel
public class DockViewModel extends ViewModel {

    static final String KEY_BARCODE = "dock.barcode";
    private final MutableLiveData<UiState<DockState>> dockState = new MutableLiveData<>(new UiState.Idle<>());
    private final SavedStateHandle savedState;
    private final DockRepository dockRepository;
    @Nullable
    private Cancellable pendingLoad;

    @Inject
    public DockViewModel(SavedStateHandle savedState, DockRepository dockRepository) {
        this.savedState = savedState;
        this.dockRepository = dockRepository;
        // Solo se guardan CB ya validados: se puede consultar directamente
        String barcode = savedState.get(KEY_BARCODE);
        if (barcode != null) {
            Timber.i("[ESTADO] Se recupera la consulta de muelle del bulto %s", barcode);
            loadDock(barcode);
        }
    }

    /**
     * Estado de la consulta del muelle (Idle → Loading → Success/Error). Que el bulto no tenga
     * muelle o que el CB no sea válido no es un error: llega dentro de Success.
     */
    public LiveData<UiState<DockState>> getDockState() {
        return dockState;
    }

    /**
     * CB de la lectura en curso o cuyo muelle se muestra; {@code null} a la espera de un bulto.
     */
    @Nullable
    public String getScannedBarcode() {
        return savedState.get(KEY_BARCODE);
    }

    /**
     * Busca el muelle del bulto leído; sustituye al anterior. Un CB vacío se ignora y uno que no
     * cumple {@link PieceBarcode} se rechaza sin consultar al servidor.
     */
    @MainThread
    public void onBarcodeScanned(@Nullable String barcode, @NonNull BarcodeSymbology symbology) {
        if (barcode == null || barcode.isBlank()) {
            return;
        }

        String code = barcode.trim();
        if (!PieceBarcode.isValid(code, symbology)) {
            Timber.w("[ACCION] CB no válido (%d caracteres, %s): %s", code.length(), symbology, code);
            cancelPendingLoad();
            savedState.remove(KEY_BARCODE);
            dockState.setValue(new UiState.Success<>(new DockState.InvalidBarcode(code)));
            return;
        }

        Timber.i("[ACCION] Consultar muelle del bulto %s", code);
        loadDock(code);
    }

    /**
     * Descarta la lectura actual: se espera el bulto siguiente.
     */
    @MainThread
    public void onCancelClicked() {
        Timber.i("[ACCION] Cancelar lectura de muelle");
        cancelPendingLoad();
        savedState.remove(KEY_BARCODE);
        dockState.setValue(new UiState.Idle<>());
    }

    /**
     * Vuelve a consultar el último CB tras un fallo de red o del servidor. Si no hay fallo (p. ej.
     * con la consulta en curso o el bulto sin muelle) no hace nada: sin muelle hay que leer otro.
     */
    @MainThread
    public void onRetryClicked() {
        String barcode = savedState.get(KEY_BARCODE);
        if (dockState.getValue() instanceof UiState.Error && barcode != null) {
            Timber.i("[ACCION] Reintentar muelle del bulto %s", barcode);
            loadDock(barcode);
        }
    }

    @Override
    protected void onCleared() {
        cancelPendingLoad();
    }

    private void loadDock(@NonNull String barcode) {
        cancelPendingLoad();
        savedState.set(KEY_BARCODE, barcode);
        dockState.setValue(new UiState.Loading<>());
        pendingLoad = dockRepository.getDock(barcode, new ResultCallback<>() {
            @Override
            public void onSuccess(@NonNull Optional<Dock> dock) {
                pendingLoad = null;
                onDockLoaded(barcode, dock);
            }

            @Override
            public void onError(@NonNull AppError error) {
                pendingLoad = null;
                onDockFailed(barcode, error);
            }
        });
    }

    /**
     * Evita que la respuesta de un bulto anterior sustituya a la del actual.
     */
    private void cancelPendingLoad() {
        if (pendingLoad != null) {
            pendingLoad.cancel();
            pendingLoad = null;
        }
    }

    private void onDockLoaded(@NonNull String barcode, @NonNull Optional<Dock> dock) {
        DockState result = dock.<DockState>map(found -> new DockState.Found(barcode, found)).orElseGet(() -> new DockState.NotFound(barcode));
        dockState.setValue(new UiState.Success<>(result));
    }

    // El CB sigue en savedState para que onRetryClicked pueda repetir la consulta
    private void onDockFailed(@NonNull String barcode, @NonNull AppError error) {
        Timber.w("[ESTADO] No se pudo consultar el muelle del bulto %s", barcode);
        dockState.setValue(new UiState.Error<>(ErrorUiMapper.toUiText(error)));
    }
}
