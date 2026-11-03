package com.redur.electra.data.repository.piece.dock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.redur.electra.core.asyn.Cancellable;
import com.redur.electra.core.asyn.ResultCallback;
import com.redur.electra.core.error.AppError;
import com.redur.electra.data.model.piece.dock.Dock;
import com.redur.electra.data.remote.api.PieceApiService;
import com.redur.electra.data.remote.dto.request.piece.dock.DockRequestDT0;
import com.redur.electra.data.remote.dto.response.ApiResponseDTO;
import com.redur.electra.data.remote.dto.response.piece.dock.DockResponseDT0;
import com.redur.electra.data.repository.BaseRepository;

import java.util.Optional;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class DockRepository extends BaseRepository {

    private final PieceApiService apiService;

    @Inject
    public DockRepository(PieceApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Consulta el muelle de salida del bulto. Llega vacío si el backend no localiza el bulto o este
     * no tiene muelle asignado.
     */
    @NonNull
    public Cancellable getDock(@NonNull String barcode, @NonNull ResultCallback<Optional<Dock>> callback) {
        Timber.i("[RED] Consultar muelle del bulto %s", barcode);

        // Pendiente: credenciales provisionales hasta que exista la capa de sesión
        DockRequestDT0 request = new DockRequestDT0("infjorna", "redur", "es", barcode);
        ResultCallback<Optional<Dock>> result = notFoundAsEmpty(callback);

        Call<ApiResponseDTO<DockResponseDT0>> call = apiService.getPieceDock(request);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponseDTO<DockResponseDT0>> call, @NonNull Response<ApiResponseDTO<DockResponseDT0>> response) {
                if (call.isCanceled()) {
                    return;
                }

                DockResponseDT0 data = extractData(response, result);
                if (data == null) {
                    return;
                }

                if (data.muelle() == null || data.muelle().isBlank()) {
                    Timber.i("[API] El bulto %s no tiene muelle asignado", barcode);
                    result.onSuccess(Optional.empty());
                    return;
                }

                Dock dock = toDock(data);
                if (dock == null) {
                    Timber.e("[API] Muelle %s sin destino completo para el bulto %s", data.muelle(), barcode);
                    result.onError(new AppError.ApiError(null, null));
                    return;
                }
                Timber.i("[API] El bulto %s sale por el muelle %s", barcode, dock.code());
                result.onSuccess(Optional.of(dock));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponseDTO<DockResponseDT0>> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                result.onError(toAppError(call, t));
            }
        });
        return call::cancel;
    }

    /** {@code null} si falta algún dato del destino: no se puede mostrar un muelle a medias. */
    @Nullable
    private static Dock toDock(@NonNull DockResponseDT0 data) {
        if (data.plzsId() == null || data.postalCode() == null || data.country() == null) {
            return null;
        }
        return new Dock(data.muelle(), data.plzsId(), data.postalCode(), data.country());
    }
}
