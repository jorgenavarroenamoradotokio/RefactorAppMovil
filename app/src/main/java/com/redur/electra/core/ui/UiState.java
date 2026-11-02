package com.redur.electra.core.ui;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Estado genérico de una operación asíncrona lanzada desde la UI (login, envío, carga...).
 * {@code T} es el resultado que entrega la operación al terminar bien; los resultados de negocio
 * (p. ej. "no encontrado") viajan dentro de {@link Success}, no como {@link Error}.
 * Los estados propios de un formulario (errores por campo) se modelan aparte en cada feature.
 */
public sealed interface UiState<T>
        permits UiState.Idle, UiState.Loading, UiState.Success, UiState.Error {

    record Idle<T>() implements UiState<T> {
    }

    record Loading<T>() implements UiState<T> {
    }

    record Success<T>(@NonNull T data) implements UiState<T> {
        public Success {
            Objects.requireNonNull(data, "data");
        }
    }

    /** El mensaje se resuelve en la UI; se obtiene con {@link ErrorUiMapper#toUiText}. */
    record Error<T>(@NonNull UiText message) implements UiState<T> {
        public Error {
            Objects.requireNonNull(message, "message");
        }
    }
}
