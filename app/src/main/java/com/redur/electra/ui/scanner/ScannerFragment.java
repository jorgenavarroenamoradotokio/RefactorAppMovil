package com.redur.electra.ui.scanner;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;
import com.redur.electra.R;
import com.redur.electra.core.scanner.BarcodeSymbology;
import com.redur.electra.core.scanner.HardwareScannerManager;
import com.redur.electra.core.scanner.ScannerManagerFactory;
import com.redur.electra.databinding.FragmentScannerBinding;

import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

/**
 * Lector de códigos de barras reutilizable para las pantallas que escanean. Se añade como hijo con
 * un {@code FragmentContainerView} que cubra la zona de contenido de la pantalla y reúne las tres
 * formas de leer un CB:
 * <ul>
 *     <li>escáner hardware por broadcast (Honeywell, Datalogic), solo con la vista en RESUMED;</li>
 *     <li>escáner que teclea el CB y termina con Intro, en un campo oculto que mantiene el foco;</li>
 *     <li>cámara, con un botón flotante abajo a la derecha, solo sin escáner hardware.</li>
 * </ul>
 * Las lecturas llegan al fragment padre con el Fragment Result API: escucha con
 * {@link #setOnScanListener(FragmentManager, LifecycleOwner, OnScanListener)} sobre su
 * {@code getChildFragmentManager()}.
 */
@AndroidEntryPoint
public class ScannerFragment extends Fragment {

    public interface OnScanListener {
        void onBarcodeScanned(@NonNull String barcode, @NonNull BarcodeSymbology symbology);

        /** Se muestra el botón de cámara: la pantalla debe dejarle hueco bajo su contenido. */
        default void onCameraScanShown() {
        }
    }

    private static final String REQUEST_BARCODE = "scanner.barcode";
    private static final String REQUEST_CAMERA_SHOWN = "scanner.camera_shown";
    private static final String RESULT_BARCODE = "barcode";
    private static final String RESULT_SYMBOLOGY = "symbology";
    /** Nombre del formato Code 128 en ZXing ({@code BarcodeFormat.CODE_128}). */
    private static final String ZXING_FORMAT_CODE_128 = "CODE_128";

    @Inject
    ScannerManagerFactory scannerManagerFactory;

    // Se registran al construir el Fragment: así el resultado llega aunque se recree mientras
    // está abierto el diálogo de permisos o la cámara
    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::onCameraPermissionResult);
    private final ActivityResultLauncher<ScanOptions> cameraScan =
            registerForActivityResult(new ScanContract(), this::onCameraScanResult);

    @Nullable
    private FragmentScannerBinding binding;

    /**
     * Registra quién recibe las lecturas. Debe llamarse en {@code onViewCreated} del padre para
     * volver a engancharse tras una recreación.
     */
    public static void setOnScanListener(@NonNull FragmentManager childFragmentManager,
                                         @NonNull LifecycleOwner owner,
                                         @NonNull OnScanListener listener) {
        childFragmentManager.setFragmentResultListener(REQUEST_BARCODE, owner, (key, result) ->
                listener.onBarcodeScanned(Objects.requireNonNull(result.getString(RESULT_BARCODE)),
                        BarcodeSymbology.valueOf(Objects.requireNonNull(result.getString(RESULT_SYMBOLOGY)))));
        childFragmentManager.setFragmentResultListener(REQUEST_CAMERA_SHOWN, owner, (key, result) ->
                listener.onCameraScanShown());
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentScannerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentScannerBinding views = requireBinding();

        // Recibe el CB del escáner por teclado: tiene el foco sin abrir el teclado en pantalla
        views.inputScannerBarcode.setShowSoftInputOnFocus(false);
        // El escáner por teclado no informa del tipo de código
        views.inputScannerBarcode.setOnEditorActionListener((v, actionId, event) ->
                onEditorAction(actionId, event, () -> deliverBarcode(BarcodeSymbology.UNKNOWN)));

        // Solo entrega lecturas con la vista en RESUMED; el lifecycle de la vista lo libera
        HardwareScannerManager scanner = scannerManagerFactory.create(this::onBarcodeRead);
        getViewLifecycleOwner().getLifecycle().addObserver(scanner);
        if (!scanner.isAvailable()) {
            // Solo se anuncia al abrir la pantalla, no al recrearla
            showCameraScan(savedInstanceState == null);
        }

        if (savedInstanceState == null) {
            views.inputScannerBarcode.requestFocus();
        }
    }

    /** Se vacía el campo y se espera el CB siguiente, p. ej. al cancelar la lectura actual. */
    public void startNextReading() {
        FragmentScannerBinding views = requireBinding();
        views.inputScannerBarcode.setText(null);
        views.inputScannerBarcode.requestFocus();
    }

    /** Lectura que no llega tecleada en el campo (broadcast del escáner o cámara). */
    private void onBarcodeRead(@NonNull String barcode, @NonNull BarcodeSymbology symbology) {
        requireBinding().inputScannerBarcode.setText(barcode);
        deliverBarcode(symbology);
    }

    /** Entrega al padre el CB que hay en el campo. */
    private void deliverBarcode(@NonNull BarcodeSymbology symbology) {
        FragmentScannerBinding views = requireBinding();
        CharSequence text = views.inputScannerBarcode.getText();
        String barcode = text != null ? text.toString().trim() : "";
        // La lectura siguiente del escáner sustituye al CB en lugar de añadirse
        views.inputScannerBarcode.selectAll();
        if (barcode.isEmpty()) {
            return;
        }
        Bundle result = new Bundle();
        result.putString(RESULT_BARCODE, barcode);
        result.putString(RESULT_SYMBOLOGY, symbology.name());
        getParentFragmentManager().setFragmentResult(REQUEST_BARCODE, result);
    }

    /** Sin escáner hardware: la cámara es la forma de leer el CB. */
    private void showCameraScan(boolean animate) {
        View fab = requireBinding().fabScannerCamera;
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(v -> onScanCameraClicked());
        getParentFragmentManager().setFragmentResult(REQUEST_CAMERA_SHOWN, Bundle.EMPTY);
        if (animate) {
            animateFabEnter(fab);
        }
    }

    /** Entra subiendo un poco desde su sitio: se percibe como parte de la pantalla, no como un aviso. */
    private void animateFabEnter(@NonNull View fab) {
        fab.setAlpha(0f);
        fab.setTranslationY(getResources().getDimension(R.dimen.spacing_m));
        fab.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(getResources().getInteger(R.integer.motion_duration_enter))
                .setInterpolator(new DecelerateInterpolator());
    }

    /** Se desactiva hasta cerrar la cámara o resolver el permiso: evita abrirla dos veces. */
    private void onScanCameraClicked() {
        requireBinding().fabScannerCamera.setEnabled(false);
        cameraPermission.launch(Manifest.permission.CAMERA);
    }

    private void onCameraPermissionResult(boolean granted) {
        if (granted) {
            launchCameraScan();
            return;
        }
        FragmentScannerBinding views = requireBinding();
        views.fabScannerCamera.setEnabled(true);
        Snackbar snackbar = Snackbar.make(views.getRoot(), R.string.scanner_camera_denied, Snackbar.LENGTH_LONG)
                .setAnchorView(views.fabScannerCamera);
        // Sin justificación tras denegarlo, el sistema ya no vuelve a preguntar: solo queda Ajustes
        if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            snackbar.setAction(R.string.scanner_camera_settings, v -> openAppSettings());
        }
        snackbar.show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().getPackageName(), null));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Timber.e(e, "No se pueden abrir los ajustes de la app");
        }
    }

    private void launchCameraScan() {
        ScanOptions options = new ScanOptions()
                .setPrompt(getString(R.string.scanner_camera_prompt))
                .setBeepEnabled(true)
                // El visor sigue la orientación del terminal (ver CaptureActivity en el manifest)
                .setOrientationLocked(false);
        try {
            Timber.i("Abrimos la cámara para escanear");
            cameraScan.launch(options);
        } catch (ActivityNotFoundException e) {
            Timber.e(e, "No se puede abrir el visor de escaneo por cámara");
            requireBinding().fabScannerCamera.setEnabled(true);
        }
    }

    /** Sin contenido es que el operario cerró la cámara sin leer nada. */
    private void onCameraScanResult(@NonNull ScanIntentResult result) {
        requireBinding().fabScannerCamera.setEnabled(true);
        String barcode = result.getContents();
        if (barcode == null || barcode.isEmpty()) {
            Timber.d("Escaneo por cámara cancelado");
            return;
        }
        onBarcodeRead(barcode, toSymbology(result.getFormatName()));
    }

    @VisibleForTesting
    @NonNull
    static BarcodeSymbology toSymbology(@Nullable String zxingFormat) {
        if (zxingFormat == null || zxingFormat.isEmpty()) {
            return BarcodeSymbology.UNKNOWN;
        }
        return ZXING_FORMAT_CODE_128.equals(zxingFormat) ? BarcodeSymbology.CODE_128 : BarcodeSymbology.OTHER;
    }

    /**
     * Acción del teclado o Intro del escáner, que llega sin acción IME.
     *
     * @return si se ha consumido el evento
     */
    @VisibleForTesting
    static boolean onEditorAction(int actionId, @Nullable KeyEvent event, @NonNull Runnable action) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            action.run();
            return true;
        }
        if (actionId != EditorInfo.IME_ACTION_UNSPECIFIED
                || event == null
                || event.getKeyCode() != KeyEvent.KEYCODE_ENTER) {
            return false;
        }
        // De Intro llegan pulsación y suelta: se actúa una sola vez
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            action.run();
        }
        return true;
    }

    @NonNull
    private FragmentScannerBinding requireBinding() {
        if (binding == null) {
            throw new IllegalStateException("La vista del escáner no está disponible");
        }
        return binding;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.fabScannerCamera.animate().cancel();
        }
        binding = null;
    }
}
