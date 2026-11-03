package com.redur.electra.core.scanner;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;

public interface HardwareScannerManager extends DefaultLifecycleObserver {

    boolean isAvailable();
    interface ScanListener {
        @MainThread
        void onScanReceived(@NonNull String barcode);
    }

}
