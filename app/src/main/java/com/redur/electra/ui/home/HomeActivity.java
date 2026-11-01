package com.redur.electra.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.redur.electra.BuildConfig;
import com.redur.electra.R;
import com.redur.electra.core.ui.BasicActivity;
import com.redur.electra.databinding.ActivityHomeBinding;
import com.redur.electra.ui.profile.ProfileActivity;
import com.redur.electra.ui.sheet.GenericBottomSheet;
import com.redur.electra.ui.toolbar.HomeToolbarController;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class HomeActivity extends BasicActivity<ActivityHomeBinding> implements HomeToolbarController.Listener {
    private static final String REQUEST_LOGOUT = "home_logout";
    private static final String REQUEST_SYSTEM = "home_system";

    private HomeViewModel viewModel;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        navController = Objects.requireNonNull(navHost).getNavController();

        // Incluimos el toolbar y la funcionalidad de los iconos asociados a ella
        getBinding().appBar.toolbar.setTitle(R.string.app_name);
        new HomeToolbarController(getBinding().appBar.toolbar, this).setup();

        setupBackNavigation();
        setupLogoutConfirmation();
    }

    @NonNull
    @Override
    protected ActivityHomeBinding inflateBinding(@NonNull LayoutInflater inflater) {
        return ActivityHomeBinding.inflate(inflater);
    }

    @Nullable
    @Override
    protected View getAppBar() {
        return getBinding().appBar.getRoot();
    }

    @NonNull
    @Override
    protected View getBody() {
        return getBinding().navHost;
    }

    /**
     * Funcionalidad del btn de perfil
     */
    @Override
    public void onProfileClick() {
        Timber.d("[TOOLBAR] perfil pulsado");
        startActivity(new Intent(this, ProfileActivity.class));
    }

    /**
     * Funcionalidad del btn de servidor conectado
     */
    @Override
    public void onEnvironmentClick() {
        Timber.d("[TOOLBAR] entorno pulsado");
        GenericBottomSheet.builder(REQUEST_SYSTEM)
                .icon(R.drawable.ic_app_server)
                .title(R.string.system_title)
                .message(R.string.system_message)
                .detail(R.string.system_environment, BuildConfig.ENVIRONMENT)
                .detail(R.string.system_server, BuildConfig.BASE_URL)
                .primaryAction(R.string.accept)
                .show(getSupportFragmentManager());
    }

    /**
     * Definimos el comportamiento del btn de retroceso: primero deshace la navegación interna y,
     * ya en Inicio, en lugar de salir de la app pregunta si se quiere cerrar sesión.
     * Pregunta si se quiere cerrar sesión. La respuesta llega a {@link #setupLogoutConfirmation()}.
     */
    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!navController.popBackStack()) {
                    GenericBottomSheet.builder(REQUEST_LOGOUT)
                            .icon(R.drawable.ic_app_logout)
                            .title(R.string.logout_title)
                            .message(R.string.logout_message)
                            .destructiveAction(R.string.logout_confirm)
                            .show(getSupportFragmentManager());
                }
            }
        });
    }

    /**
     * Escucha la respuesta de la hoja de cierre de sesión. Se registra en onCreate para seguir
     * recibiéndola si la Activity se recrea con la hoja abierta.
     */
    private void setupLogoutConfirmation() {
        GenericBottomSheet.setOnActionListener(getSupportFragmentManager(), REQUEST_LOGOUT, this, action -> {
            if (action == GenericBottomSheet.Action.DESTRUCTIVE) {
                // Pendiente: aún no existe capa de sesión que invalidar ni pantalla de login a la que volver
                Timber.i("Cierre de sesión confirmado");
            }
        });
    }
}
