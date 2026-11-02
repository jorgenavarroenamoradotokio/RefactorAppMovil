package com.redur.electra.ui.piece.dock;

import androidx.annotation.NonNull;

import com.redur.electra.core.ui.UiText;
import com.redur.electra.data.model.piece.dock.Dock;

import java.util.Objects;

public sealed interface DockState permits
        DockState.Found,
        DockState.NotFound {

    /**
     * Muelle del bulto: la lectura se puede grabar.
     */
    record Found(@NonNull String barcode, @NonNull Dock dock) implements DockState {
        public Found {
            Objects.requireNonNull(barcode, "barcode");
            Objects.requireNonNull(dock, "dock");
        }
    }

    /**
     * El bulto no tiene muelle asignado. Se puede leer otro.
     */
    record NotFound(@NonNull String barcode) implements DockState {
        public NotFound {
            Objects.requireNonNull(barcode, "barcode");
        }
    }
}
