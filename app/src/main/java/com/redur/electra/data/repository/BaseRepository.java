package com.redur.electra.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.redur.electra.core.asyn.ResultCallback;
import com.redur.electra.core.error.AppError;
import com.redur.electra.core.error.Network;
import com.redur.electra.data.remote.dto.response.ApiErrorDetailResponseDTO;
import com.redur.electra.data.remote.dto.response.ApiResponseDTO;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.Optional;

import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Base para consumir el backend: interpreta el sobre {@link ApiResponseDTO}
 * y traduce los fallos a {@link AppError}.
 */
public class BaseRepository {

    private static final String HTTP_ERROR_PREFIX = "HTTP_";
    private static final Gson ERROR_BODY_GSON = new Gson();
    /** El backend no tiene datos para la consulta. */
    private static final String DATA_NOT_FOUND_CODE = "ERROR_S02_ERROR_DATOS_NO_LOCALIZADOS";

    /**
     * Envuelve {@code callback} para que "datos no localizados" llegue como éxito vacío: en una
     * consulta, no encontrar el dato es un resultado de negocio y no un fallo.
     */
    @NonNull
    protected static <T> ResultCallback<Optional<T>> notFoundAsEmpty(@NonNull ResultCallback<Optional<T>> callback) {
        return new ResultCallback<>() {
            @Override
            public void onSuccess(@NonNull Optional<T> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(@NonNull AppError error) {
                if (error instanceof AppError.ApiError api && DATA_NOT_FOUND_CODE.equals(api.code())) {
                    callback.onSuccess(Optional.empty());
                } else {
                    callback.onError(error);
                }
            }
        };
    }

    /**
     * Valida la respuesta y devuelve su {@code data}. Si la respuesta no es válida notifica el
     * error a {@code callback} y devuelve {@code null}: en ese caso el llamador no debe volver a
     * invocar el callback.
     */
    @Nullable
    protected static <T> T extractData(@NonNull Response<ApiResponseDTO<T>> response, @NonNull ResultCallback<?> callback) {
        String endpoint = describe(response.raw().request());
        if (!response.isSuccessful()) {
            callback.onError(toHttpError(response, endpoint));
            return null;
        }

        ApiResponseDTO<T> body = response.body();
        if (body == null) {
            Timber.e("[RED] %s%s sin cuerpo en %s", HTTP_ERROR_PREFIX, response.code(), endpoint);
            callback.onError(new AppError.ApiError(HTTP_ERROR_PREFIX + response.code(), null));
            return null;
        }

        ApiErrorDetailResponseDTO apiError = firstError(body.errorList());
        if (apiError != null) {
            Timber.e("[API] Error %s en %s: %s", apiError.code(), endpoint, apiError.description());
            callback.onError(new AppError.ApiError(apiError.code(), apiError.description()));
            return null;
        }

        if (body.data() == null) {
            Timber.e("[API] Respuesta sin datos en %s: %s", endpoint, body.errorText());
            callback.onError(new AppError.ApiError(null, body.errorText()));
            return null;
        }
        return body.data();
    }

    /**
     * Traduce el fallo de una llamada a {@link AppError} y lo registra con el endpoint afectado.
     * Solo debe llamarse si la llamada no se ha cancelado: una cancelación no es un error.
     */
    @NonNull
    protected static AppError toAppError(@NonNull Call<?> call, @NonNull Throwable t) {
        String endpoint = describe(call.request());
        // SocketTimeoutException hereda de InterruptedIOException
        if (t instanceof InterruptedIOException) {
            // Sin traza completa: en un fallo de red basta con el tipo y el mensaje
            Timber.w("[RED] Timeout en %s: %s", endpoint, t.toString());
            return new AppError.NetworkError(Network.TIMEOUT);
        }
        if (t instanceof IOException) {
            Timber.w("[RED] Sin conexión con el backend en %s: %s", endpoint, t.toString());
            return new AppError.NetworkError(Network.NO_CONNECTION);
        }
        // Respuesta imposible de interpretar (p. ej. JSON inesperado): no es un fallo de red
        Timber.e(t, "[API] Respuesta del backend no interpretable en %s", endpoint);
        return new AppError.ApiError(null, null);
    }

    /**
     * Un HTTP no 2xx puede traer igualmente el sobre {@link ApiResponseDTO} (p. ej. un 403 por
     * credenciales incorrectas): si incluye un error de negocio se prioriza su código; si no, el
     * error se identifica por el código HTTP.
     */
    @NonNull
    private static AppError toHttpError(@NonNull Response<?> response, @NonNull String endpoint) {
        ApiErrorDetailResponseDTO apiError = firstError(parseErrorBody(response.errorBody(), endpoint));
        if (apiError != null && apiError.code() != null) {
            Timber.e("[API] %s%s con error %s en %s", HTTP_ERROR_PREFIX, response.code(), apiError.code(), endpoint);
            return new AppError.ApiError(apiError.code(), apiError.description());
        }
        Timber.e("[RED] %s%s en %s", HTTP_ERROR_PREFIX, response.code(), endpoint);
        return new AppError.ApiError(HTTP_ERROR_PREFIX + response.code(), null);
    }

    /**
     * Lista de errores del cuerpo de error, o {@code null} si no hay cuerpo o no es el sobre esperado.
     */
    @Nullable
    private static List<ApiErrorDetailResponseDTO> parseErrorBody(@Nullable ResponseBody errorBody, @NonNull String endpoint) {
        if (errorBody == null) {
            return null;
        }
        // Retrofit ya ha volcado el cuerpo de error a memoria: leerlo no bloquea con red
        try (errorBody) {
            ApiResponseDTO<?> envelope = ERROR_BODY_GSON.fromJson(errorBody.string(), ApiResponseDTO.class);
            if (envelope == null) {
                return null;
            }

            return envelope.errorList();
        } catch (IOException | JsonParseException e) {
            Timber.w("[API] Cuerpo de error no interpretable en %s: %s", endpoint, e.toString());
            return null;
        }
    }
    
    @Nullable
    private static ApiErrorDetailResponseDTO firstError(@Nullable List<ApiErrorDetailResponseDTO> errors) {
        if (errors == null) {
            return null;
        }
        for (ApiErrorDetailResponseDTO error : errors) {
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    /**
     * identifica la operación sin volcar parámetros
     */
    @NonNull
    private static String describe(@Nullable Request request) {
        if (request == null) {
            return "endpoint desconocido";
        }
        return request.method() + " " + request.url().encodedPath();
    }
}