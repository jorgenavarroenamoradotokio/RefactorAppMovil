package com.redur.electra.ui.home;

import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.redur.electra.BuildConfig;
import com.redur.electra.R;
import com.redur.electra.core.ui.SystemBarInsets;
import com.redur.electra.databinding.ActivityHomeBinding;
import com.redur.electra.ui.sheet.GenericBottomSheet;
import com.redur.electra.ui.toolbar.HomeToolbarController;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity implements HomeToolbarController.Listener {
    private static final String REQUEST_LOGOUT = "home_logout";
    private static final String REQUEST_SYSTEM = "home_system";

    private ActivityHomeBinding binding;
    private HomeViewModel viewModel;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Forzamos iconos de color blanco
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));

        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        navController = Objects.requireNonNull(navHost).getNavController();

        applyWindowInsets();

        // Incluimos el toolbar y la funcionalidad de los iconos asociados a ella
        new HomeToolbarController(binding.appBar.toolbar, this).setup();

        setupBackNavigation();
        setupLogoutConfirmation();
    }

    /**
     * Aparta el contenido de las barras del sistema, necesario al trabajar de borde a borde.
     * <p>
     * Con {@link EdgeToEdge} la ventana se dibuja detrás de la status bar, la barra de navegación y
     * el notch, y Android ya no desplaza el contenido por nosotros. A partir de targetSdk 35 el
     * sistema lo impone aunque no se active, así que este reparto de márgenes es obligatorio.
     * <p>
     * El fondo del AppBar se extiende bajo la status bar y solo su contenido baja lo necesario. El
     * contenido deja libre abajo lo que ocupe más, la botonera o el teclado: con edge-to-edge
     * {@code adjustResize} ya no redimensiona la ventana, por eso el teclado se gestiona aquí. Se
     * toma el máximo y no la suma porque el teclado se dibuja por encima de la botonera.
     * <p>
     * Los insets se consumen para que ninguna vista hija vuelva a aplicarlos.
     */

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, windowInsets) -> {
            Insets bars = SystemBarInsets.of(windowInsets);
            binding.appBar.getRoot().setPadding(bars.left, bars.top, bars.right, 0);
            binding.navHost.setPadding(bars.left, 0, bars.right, 0);

            // Con edge-to-edge adjustResize no encoge la ventana: el contenido deja sitio al teclado
            // para que los campos y las acciones del pie no queden debajo
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            binding.content.setPadding(0, 0, 0, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * Funcionalidad del btn de perfil
     */
    @Override
    public void onProfileClick() {
        Timber.d("Toolbar: perfil pulsado");
    }

    /**
     * Funcionalidad del btn de servidor conectado
     */
    @Override
    public void onEnvironmentClick() {
        Timber.d("Toolbar: entorno pulsado");
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
                Timber.d("Ha pulsado el btn de retroceso");
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
