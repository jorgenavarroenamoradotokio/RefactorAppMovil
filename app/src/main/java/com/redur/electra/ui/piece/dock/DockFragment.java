package com.redur.electra.ui.piece.dock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.redur.electra.R;
import com.redur.electra.core.ui.UiState;
import com.redur.electra.data.model.piece.dock.Dock;
import com.redur.electra.databinding.FragmentPieceDockBinding;

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
        binding.buttonBultoDockCancel.setOnClickListener(v -> viewModel.onCancelClicked());
        viewModel.getDockState().observe(getViewLifecycleOwner(), this::renderDock);
        showCameraScan(savedInstanceState == null);
        viewModel.onBarcodeScanned("12345");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void renderDock(@NonNull UiState<DockState> state) {
        FragmentPieceDockBinding binding = requireBinding();
        String barcode = viewModel.getScannedBarcode();

        // El CB queda a la vista mientras se busca y con el resultado, para cotejarlo con la etiqueta
        binding.layoutBultoDockBarcode.setVisibility(barcode != null && !(state instanceof UiState.Idle)
                ? View.VISIBLE : View.GONE);
        binding.textBultoDockBarcode.setText(barcode);

        binding.layoutBultoDockWaiting.setVisibility(state instanceof UiState.Idle ? View.VISIBLE : View.GONE);
        binding.layoutBultoDockLoading.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);

        boolean isFound = state instanceof UiState.Success<DockState> success
                && success.data() instanceof DockState.Found;
        binding.layoutBultoDockFound.setVisibility(isFound ? View.VISIBLE : View.GONE);
        binding.layoutBultoDockError.setVisibility(View.GONE);

        if (state instanceof UiState.Success<DockState> success) {
            if (success.data() instanceof DockState.Found found) {
                showDock(found.dock());
            } else {
                showError(R.string.dock_not_found_title, null, false);
            }
        } else if (state instanceof UiState.Error<DockState> error) {
            showError(R.string.dock_error_title, error.message().resolve(requireContext()), true);
        }
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

    private void showCameraScan(boolean animate) {
        View fab = requireBinding().fabBultoDockScanCamera;
        fab.setVisibility(View.VISIBLE);
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

    @NonNull
    private FragmentPieceDockBinding requireBinding() {
        if (binding == null) {
            throw new IllegalStateException("La vista de la puerta de salida no está disponible");
        }
        return binding;
    }
}
