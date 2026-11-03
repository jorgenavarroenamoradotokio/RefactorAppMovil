package com.redur.electra.ui.piece.dock;

import androidx.annotation.NonNull;

import com.redur.electra.core.ui.UiText;
import com.redur.electra.data.model.piece.dock.Dock;

import java.util.Objects;

public sealed interface DockState permits
        DockState.Found,
        DockState.NotFound,
        DockState.InvalidBarcode {

    /**
     * CB leído al que corresponde el resultado.
     */
    @NonNull
    String barcode();

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
     * El bulto no existe o no tiene muelle asignado. Se puede leer otro.
     */
    record NotFound(@NonNull String barcode) implements DockState {
        public NotFound {
            Objects.requireNonNull(barcode, "barcode");
        }
    }

    /**
     * La lectura no es un CB de bulto (longitud o tipo de código): no se consulta el muelle.
     */
    record InvalidBarcode(@NonNull String barcode) implements DockState {
        public InvalidBarcode {
            Objects.requireNonNull(barcode, "barcode");
        }
    }
}
