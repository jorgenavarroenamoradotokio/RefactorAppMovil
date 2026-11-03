package com.redur.electra.core.scanner.honeywell;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.redur.electra.core.scanner.BarcodeSymbology;
import com.redur.electra.core.scanner.BroadcastScannerManager;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import timber.log.Timber;

/**
 * Lecturas del Data Intent de Honeywell. La acción no es la de fábrica: hay que configurarla en el
 * terminal (Settings &gt; Scanning &gt; Internal Scanner &gt; Data Processing Settings &gt;
 * Data Intent) con el valor de {@link #ACTION}.
 */
public class HoneywellScannerManager extends BroadcastScannerManager {

    static final String ACTION = "com.honeywell.decode.intent.action.EDIT_DATA";
    static final String EXTRA_DATA = "data";
    static final String EXTRA_DATA_BYTES = "dataBytes";
    static final String EXTRA_CHARSET = "charset";
    /** Identificador AIM: "]C" seguido del modificador es la familia Code 128 (con GS1-128). */
    static final String EXTRA_AIM_ID = "aimId";
    /** Code ID propio de Honeywell: 'j' es Code 128 e 'I' GS1-128. */
    static final String EXTRA_CODE_ID = "codeId";
    private static final String AIM_CODE_128_PREFIX = "]C";
    private static final Set<String> CODE_IDS_CODE_128 = Set.of("j", "I");

    public HoneywellScannerManager(@NonNull Context context, @NonNull ScanListener listener) {
        super(context, listener);
    }

    @NonNull
    @Override
    protected IntentFilter createIntentFilter() {
        return new IntentFilter(ACTION);
    }

    @Nullable
    @Override
    protected String extractBarcode(@NonNull Intent intent) {
        String data = intent.getStringExtra(EXTRA_DATA);
        if (data != null && !data.isEmpty()) {
            return data;
        }

        byte[] bytes = intent.getByteArrayExtra(EXTRA_DATA_BYTES);
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return new String(bytes, resolveCharset(intent.getStringExtra(EXTRA_CHARSET)));
    }

    @NonNull
    @Override
    protected BarcodeSymbology extractSymbology(@NonNull Intent intent) {
        String aimId = intent.getStringExtra(EXTRA_AIM_ID);
        if (aimId != null && !aimId.isEmpty()) {
            return aimId.startsWith(AIM_CODE_128_PREFIX) ? BarcodeSymbology.CODE_128 : BarcodeSymbology.OTHER;
        }
        String codeId = intent.getStringExtra(EXTRA_CODE_ID);
        if (codeId != null && !codeId.isEmpty()) {
            return CODE_IDS_CODE_128.contains(codeId) ? BarcodeSymbology.CODE_128 : BarcodeSymbology.OTHER;
        }
        return BarcodeSymbology.UNKNOWN;
    }

    @NonNull
    private static Charset resolveCharset(@Nullable String charsetName) {
        if (charsetName == null || charsetName.isEmpty()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(charsetName);
        } catch (IllegalArgumentException e) {
            Timber.w(e, "Charset '%s' no soportado; se usa UTF-8", charsetName);
            return StandardCharsets.UTF_8;
        }
    }
}
