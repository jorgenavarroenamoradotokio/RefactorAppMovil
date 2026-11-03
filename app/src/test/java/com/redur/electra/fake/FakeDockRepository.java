package com.redur.electra.fake;

import androidx.annotation.NonNull;

import com.redur.electra.core.asyn.Cancellable;
import com.redur.electra.core.asyn.ResultCallback;
import com.redur.electra.core.error.AppError;
import com.redur.electra.data.model.piece.dock.Dock;
import com.redur.electra.data.repository.piece.dock.DockRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Repositorio que guarda las consultas para que el test decida cuándo y cómo responden. */
public class FakeDockRepository extends DockRepository {

    public final List<Request> requests = new ArrayList<>();

    public FakeDockRepository() {
        super(null);
    }

    @NonNull
    @Override
    public Cancellable getDock(@NonNull String barcode, @NonNull ResultCallback<Optional<Dock>> callback) {
        Request request = new Request(barcode, callback);
        requests.add(request);
        return request;
    }

    @NonNull
    public Request lastRequest() {
        return requests.get(requests.size() - 1);
    }

    public static final class Request implements Cancellable {
        public final String barcode;
        private final ResultCallback<Optional<Dock>> callback;
        private boolean cancelled;

        Request(String barcode, ResultCallback<Optional<Dock>> callback) {
            this.barcode = barcode;
            this.callback = callback;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void succeed(@NonNull Optional<Dock> dock) {
            callback.onSuccess(dock);
        }

        public void fail(@NonNull AppError error) {
            callback.onError(error);
        }
    }
}
