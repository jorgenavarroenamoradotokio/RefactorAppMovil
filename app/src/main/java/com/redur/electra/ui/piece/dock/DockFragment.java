package com.redur.electra.ui.piece.dock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.redur.electra.R;
import com.redur.electra.core.scanner.BarcodeSymbology;
import com.redur.electra.core.ui.UiState;
import com.redur.electra.data.model.piece.PieceBarcode;
import com.redur.electra.data.model.piece.dock.Dock;
import com.redur.electra.databinding.FragmentPieceDockBinding;
import com.redur.electra.ui.scanner.ScannerFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DockFragment extends Fragment {

    @Nullable
    private FragmentPieceDockBinding binding;
    private DockViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DockViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPieceDockBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentPieceDockBinding binding = requireBinding();
        binding.buttonBultoDockRetry.setOnClickListener(v -> viewModel.onRetryClicked());
        binding.buttonBultoDockCancel.setOnClickListener(v -> cancel());
        ScannerFragment.setOnScanListener(getChildFragmentManager(), getViewLifecycleOwner(),
                new ScannerFragment.OnScanListener() {
                    @Override
                    public void onBarcodeScanned(@NonNull String barcode, @NonNull BarcodeSymbology symbology) {
                        viewModel.onBarcodeScanned(barcode, symbology);
                    }

                    @Override
                    public void onCameraScanShown() {
                        reserveCameraScanSpace();
                    }
                });
        viewModel.getDockState().observe(getViewLifecycleOwner(), this::renderDock);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void renderDock(@NonNull UiState<DockState> state) {
        FragmentPieceDockBinding binding = requireBinding();

        // Un CB no válido no se guarda para consultar, pero también se muestra para cotejarlo
        String barcode = state instanceof UiState.Success<DockState> result ? result.data().barcode() : viewModel.getScannedBarcode();

        // El CB queda a la vista mientras se busca y con el resultado, para cotejarlo con la etiqueta
        binding.layoutBultoDockBarcode.setVisibility(barcode != null && !(state instanceof UiState.Idle) ? View.VISIBLE : View.GONE);
        binding.textBultoDockBarcode.setText(barcode);

        binding.layoutBultoDockWaiting.setVisibility(state instanceof UiState.Idle ? View.VISIBLE : View.GONE);
        binding.layoutBultoDockLoading.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);

        boolean isFound = state instanceof UiState.Success<DockState> success && success.data() instanceof DockState.Found;
        binding.layoutBultoDockFound.setVisibility(isFound ? View.VISIBLE : View.GONE);
        binding.layoutBultoDockError.setVisibility(View.GONE);

        if (state instanceof UiState.Success<DockState> success) {
            DockState data = success.data();
            if (data instanceof DockState.Found found) {
                showDock(found.dock());
            } else if (data instanceof DockState.InvalidBarcode) {
                showError(R.string.dock_invalid_barcode_title, getString(R.string.dock_invalid_barcode, PieceBarcode.LENGTH), false);
            } else {
                showError(R.string.dock_not_found_title, getString(R.string.dock_not_found), false);
            }
        } else if (state instanceof UiState.Error<DockState> error) {
            showError(R.string.dock_error_title, error.message().resolve(requireContext()), true);
        }
    }

    @NonNull
    private FragmentPieceDockBinding requireBinding() {
        if (binding == null) {
            throw new IllegalStateException("La vista de la puerta de salida no está disponible");
        }
        return binding;
    }

    private void showDock(@NonNull Dock dock) {
        FragmentPieceDockBinding binding = requireBinding();
        binding.textBultoDockCode.setText(dock.code());
        binding.textBultoDockDestination.setText(getString(R.string.dock_destination,
                dock.place(), dock.postalCode(), dock.country()));
        // La tarjeta oculta sus hijos a TalkBack: el grupo se lee con una única descripción
        binding.layoutBultoDockFound.setContentDescription(getString(R.string.dock_description,
                dock.code(), dock.place(), dock.postalCode(), dock.country()));
    }

    /** Sin muelle no hay reintento: el mismo bulto daría el mismo resultado, hay que leer otro. */
    private void showError(@StringRes int titleRes, @Nullable String message, boolean canRetry) {
        FragmentPieceDockBinding binding = requireBinding();
        binding.textBultoDockErrorTitle.setText(titleRes);
        binding.textBultoDockError.setText(message);
        binding.textBultoDockError.setVisibility(message != null ? View.VISIBLE : View.GONE);
        binding.buttonBultoDockRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
        binding.layoutBultoDockError.setVisibility(View.VISIBLE);
    }

    /**
     * Se descarta el muelle a la vista y se espera el CB del bulto siguiente.
     */
    private void cancel() {
        viewModel.onCancelClicked();
        Fragment scanner = getChildFragmentManager().findFragmentById(R.id.fragment_bulto_dock_scanner);
        if (scanner instanceof ScannerFragment scannerFragment) {
            scannerFragment.startNextReading();
        }
    }

    /**
     * Hueco al final del contenido para que el botón de cámara no tape el último elemento al desplazar.
     */
    private void reserveCameraScanSpace() {
        View content = requireBinding().layoutBultoDockContent;
        content.setPaddingRelative(content.getPaddingStart(), content.getPaddingTop(), content.getPaddingEnd(),
                getResources().getDimensionPixelSize(R.dimen.scanner_fab_clearance));
    }


}
