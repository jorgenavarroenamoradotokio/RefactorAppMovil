package com.redur.electra.core.error;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public sealed interface AppError permits AppError.NetworkError, AppError.ApiError, AppError.DeviceError {

    /** Fallo de comunicación: no se llegó a obtener respuesta del servidor. */
    record NetworkError(@NonNull Network type) implements AppError {
        public NetworkError {
            Objects.requireNonNull(type, "type");
        }
    }

    /** El servidor respondió con un error de negocio identificado por {@code code}. */
    record ApiError(@Nullable String code, @Nullable String serverMessage) implements AppError {
    }

    /** No se pudo conectar a dispositivos externos */
    record DeviceError(@NonNull Device failure) implements AppError {
        public DeviceError {
            Objects.requireNonNull(failure, "failure");
        }
    }

}
