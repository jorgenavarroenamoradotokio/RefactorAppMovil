package com.redur.electra.core.asyn;

/**
 * Operación asíncrona en curso que se puede cancelar. Tras cancelarla su {@link ResultCallback}
 * ya no se invoca.
 */
public interface Cancellable {

    void cancel();
}
