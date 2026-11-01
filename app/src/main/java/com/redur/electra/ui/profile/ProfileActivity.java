package com.redur.electra.ui.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.redur.electra.R;
import com.redur.electra.core.ui.BasicActivity;
import com.redur.electra.databinding.ActivityProfileBinding;
import com.redur.electra.ui.sheet.GenericBottomSheet;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileActivity extends BasicActivity<ActivityProfileBinding> {

    private static final String REQUEST_LOGOUT = "profile_logout";

    private ProfileViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupToolbar();
        renderTerminalId(viewModel.getTerminalId());
        setupActionLogout();
    }

    @NonNull
    @Override
    protected ActivityProfileBinding inflateBinding(@NonNull LayoutInflater inflater) {
        return ActivityProfileBinding.inflate(inflater);
    }

    @Nullable
    @Override
    protected View getAppBar() {
        return getBinding().appBar.getRoot();
    }

    @NonNull
    @Override
    protected View getBody() {
        return getBinding().body;
    }

    /**
     * Título y flecha de retroceso. Se abre desde Home, así que volver atrás regresa a ella; se pasa
     * por el dispatcher para respetar cualquier callback de retroceso registrado.
     */
    private void setupToolbar() {
        MaterialToolbar toolbar = getBinding().appBar.toolbar;
        toolbar.setTitle(R.string.profile_title);
        toolbar.setNavigationIcon(R.drawable.ic_app_arrow_back);
        toolbar.setNavigationContentDescription(R.string.profile_navigate_up);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    /**
     * Muestra el ID del terminal y, si existe, permite copiarlo pulsando la fila.
     */
    private void renderTerminalId(@Nullable String terminalId) {
        View row = getBinding().rowTerminalId;
        if (terminalId == null) {
            getBinding().textTerminalId.setText(R.string.profile_terminal_id_unavailable);
            row.setOnClickListener(null);
            row.setClickable(false);
            return;
        }
        getBinding().textTerminalId.setText(terminalId);
        row.setOnClickListener(v -> copyTerminalId(terminalId));
        // TalkBack anuncia "doble toque para copiar" en lugar de "para activar"
        ViewCompat.replaceAccessibilityAction(row, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                getString(R.string.profile_terminal_id_copy_action), null);
    }

    /**
     * Copia el ID al portapapeles. Desde Android 13 el sistema ya confirma la copia con su propio
     * aviso, así que solo se muestra uno propio en versiones anteriores para no duplicarlo.
     */
    private void copyTerminalId(@NonNull String terminalId) {
        ClipboardManager clipboard = getSystemService(ClipboardManager.class);
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.profile_terminal_id_label), terminalId));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.profile_terminal_id_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupActionLogout() {
        getBinding().buttonLogout.setOnClickListener(v -> GenericBottomSheet.builder(REQUEST_LOGOUT)
                .icon(R.drawable.ic_app_logout)
                .title(R.string.logout_title)
                .message(R.string.logout_message)
                .destructiveAction(R.string.logout_confirm)
                .show(getSupportFragmentManager()));
    }
}
