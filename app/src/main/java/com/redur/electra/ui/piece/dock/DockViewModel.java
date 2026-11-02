package com.redur.electra.ui.piece.dock;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.redur.electra.core.error.AppError;
import com.redur.electra.core.ui.ErrorUiMapper;
import com.redur.electra.core.ui.UiState;
import com.redur.electra.data.model.piece.dock.Dock;

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
    // Provisional: hasta que exista el servicio de muelles
    static final Dock MOCK_DOCK = new Dock("Z14", "ZAR", "50197", "España");
    private final MutableLiveData<UiState<DockState>> dockState = new MutableLiveData<>(new UiState.Idle<>());
    private final SavedStateHandle savedState;

    @Inject
    public DockViewModel(SavedStateHandle savedState) {
        this.savedState = savedState;
        String barcode = savedState.get(KEY_BARCODE);
        if (barcode != null) {
            Timber.i("[ESTADO] Se recupera la consulta de muelle del bulto %s", barcode);
            loadDock(barcode);
        }
    }

    /**
     * Estado de la consulta del muelle (Idle → Loading → Success/Error). Que el bulto no tenga
     * muelle no es un error: llega como {@link DockState.NotFound} dentro de Success.
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
     * Busca el muelle del bulto leído; sustituye al anterior. Un CB vacío se ignora.
     */
    @MainThread
    public void onBarcodeScanned(@Nullable String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return;
        }
        String code = barcode.trim();
        Timber.i("[ACCION] Consultar muelle del bulto %s", code);
        loadDock(code);
    }

    /**
     * Descarta la lectura actual: se espera el bulto siguiente.
     */
    @MainThread
    public void onCancelClicked() {
        Timber.i("[ACCION] Cancelar lectura de muelle");
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

    private void loadDock(@NonNull String barcode) {
        savedState.set(KEY_BARCODE, barcode);
        dockState.setValue(new UiState.Loading<>());
        // Provisional: aquí irá la llamada al servicio; su callback usará onDockLoaded/onDockFailed
        onDockLoaded(barcode, MOCK_DOCK);
    }

    private void onDockLoaded(@NonNull String barcode, @Nullable Dock dock) {
        DockState result = dock != null
                ? new DockState.Found(barcode, dock)
                : new DockState.NotFound(barcode);
        dockState.setValue(new UiState.Success<>(result));
    }

    // El CB sigue en savedState para que onRetryClicked pueda repetir la consulta
    private void onDockFailed(@NonNull AppError error) {
        Timber.w("[ESTADO] No se pudo consultar el muelle del bulto %s", savedState.<String>get(KEY_BARCODE));
        dockState.setValue(new UiState.Error<>(ErrorUiMapper.toUiText(error)));
    }
}
