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

import com.redur.electra.core.ui.SystemBarInsets;
import com.redur.electra.databinding.ActivityHomeBinding;
import com.redur.electra.ui.toolbar.HomeToolbarController;

import timber.log.Timber;

public class HomeActivity extends AppCompatActivity implements HomeToolbarController.Listener {
    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Forzamos iconos de color blanco
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));

        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets();
        // Incluimos el toolbar y la funcionalidad de los iconos asociados a ella
        new HomeToolbarController(binding.appBar.toolbar, this).setup();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, windowInsets) -> {
            Insets bars = SystemBarInsets.of(windowInsets);
            binding.appBar.getRoot().setPadding(bars.left, bars.top, bars.right, 0);
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
    }

    /**
     * Definimos el comportamiento del btn de retroceso
     */
    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });
    }
}
