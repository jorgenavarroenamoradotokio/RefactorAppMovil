package com.redur.electra.data.repository.piece.dock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import com.redur.electra.core.asyn.ResultCallback;
import com.redur.electra.core.error.AppError;
import com.redur.electra.core.error.Network;
import com.redur.electra.data.model.piece.dock.Dock;
import com.redur.electra.data.remote.api.PieceApiService;
import com.redur.electra.data.remote.dto.request.piece.dock.DockRequestDT0;
import com.redur.electra.data.remote.dto.response.ApiErrorDetailResponseDTO;
import com.redur.electra.data.remote.dto.response.ApiResponseDTO;
import com.redur.electra.data.remote.dto.response.piece.dock.DockResponseDT0;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DockRepositoryTest {

    private static final String BARCODE = "0123456789ABCDEFGHIJ012345678";
    private static final String NOT_FOUND_CODE = "ERROR_S02_ERROR_DATOS_NO_LOCALIZADOS";

    private final List<Optional<Dock>> successes = new ArrayList<>();
    private final List<AppError> errors = new ArrayList<>();
    private FakeCall call;
    private DockRepository repository;

    @Before
    public void setUp() {
        call = new FakeCall();
        repository = new DockRepository(new PieceApiService() {
            @Override
            public Call<ApiResponseDTO<DockResponseDT0>> getPieceDock(DockRequestDT0 request) {
                return call;
            }
        });
        repository.getDock(BARCODE, new ResultCallback<>() {
            @Override
            public void onSuccess(@NonNull Optional<Dock> result) {
                successes.add(result);
            }

            @Override
            public void onError(@NonNull AppError error) {
                errors.add(error);
            }
        });
    }

    @Test
    public void respuestaConMuelle_entregaElMuelle() {
        call.respond(Response.success(envelope(new DockResponseDT0("ZAR", "Z14", "50197", "España"))));

        assertEquals(List.of(Optional.of(new Dock("Z14", "ZAR", "50197", "España"))), successes);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void datosNoLocalizados_entregaVacio() {
        call.respond(Response.success(new ApiResponseDTO<>(0, null,
                List.of(new ApiErrorDetailResponseDTO(NOT_FOUND_CODE, "No localizado", null, "es")), null)));

        assertEquals(List.of(Optional.empty()), successes);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void datosNoLocalizadosConHttpError_entregaVacio() {
        String body = "{\"errorList\":[{\"errorCode\":\"" + NOT_FOUND_CODE + "\"}]}";
        call.respond(Response.error(404, ResponseBody.create(MediaType.get("application/json"), body)));

        assertEquals(List.of(Optional.empty()), successes);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void bultoSinMuelleAsignado_entregaVacio() {
        call.respond(Response.success(envelope(new DockResponseDT0("ZAR", " ", "50197", "España"))));

        assertEquals(List.of(Optional.empty()), successes);
    }

    @Test
    public void muelleSinDestinoCompleto_esUnError() {
        call.respond(Response.success(envelope(new DockResponseDT0(null, "Z14", "50197", "España"))));

        assertTrue(successes.isEmpty());
        assertEquals(List.of(new AppError.ApiError(null, null)), errors);
    }

    @Test
    public void otroErrorDeNegocio_seEntregaComoError() {
        call.respond(Response.success(new ApiResponseDTO<>(0, null,
                List.of(new ApiErrorDetailResponseDTO("ERROR_MU01", "CB vacío", null, "es")), null)));

        assertTrue(successes.isEmpty());
        assertEquals(List.of(new AppError.ApiError("ERROR_MU01", "CB vacío")), errors);
    }

    @Test
    public void falloDeRed_esErrorDeConexion() {
        call.fail(new IOException("sin red"));

        assertEquals(List.of(new AppError.NetworkError(Network.NO_CONNECTION)), errors);
    }

    @Test
    public void consultaCancelada_noEntregaNada() {
        call.cancel();

        call.respond(Response.success(envelope(new DockResponseDT0("ZAR", "Z14", "50197", "España"))));
        call.fail(new IOException("Canceled"));

        assertTrue(successes.isEmpty());
        assertTrue(errors.isEmpty());
    }

    private static ApiResponseDTO<DockResponseDT0> envelope(DockResponseDT0 data) {
        return new ApiResponseDTO<>(0, null, null, data);
    }

    /** Llamada que responde cuando lo indica el test, sin red. */
    private static final class FakeCall implements Call<ApiResponseDTO<DockResponseDT0>> {

        private Callback<ApiResponseDTO<DockResponseDT0>> callback;
        private boolean canceled;

        void respond(Response<ApiResponseDTO<DockResponseDT0>> response) {
            callback.onResponse(this, response);
        }

        void fail(Throwable t) {
            callback.onFailure(this, t);
        }

        @Override
        public Response<ApiResponseDTO<DockResponseDT0>> execute() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void enqueue(Callback<ApiResponseDTO<DockResponseDT0>> callback) {
            this.callback = callback;
        }

        @Override
        public boolean isExecuted() {
            return callback != null;
        }

        @Override
        public void cancel() {
            canceled = true;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public Call<ApiResponseDTO<DockResponseDT0>> clone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Request request() {
            return new Request.Builder().url("http://localhost/muelle").build();
        }

        @Override
        public Timeout timeout() {
            return Timeout.NONE;
        }
    }
}
