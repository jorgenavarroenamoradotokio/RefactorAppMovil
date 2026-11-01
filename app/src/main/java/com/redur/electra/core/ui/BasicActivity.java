package com.redur.electra.core.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewbinding.ViewBinding;

/**
 * Base de todas las Activities de la app: dibuja de borde a borde e infla su layout con ViewBinding.
 * <p>
 * Las subclases indican cómo inflar su binding y, opcionalmente, qué vista hace de AppBar y cuál de
 * cuerpo. Deben seguir anotándose con {@code @AndroidEntryPoint}: Hilt solo procesa clases concretas.
 *
 * @param <VB> binding del layout de la Activity
 */
public abstract class BasicActivity<VB extends ViewBinding> extends AppCompatActivity {

    private VB binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Forzamos iconos de color blanco
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));

        super.onCreate(savedInstanceState);
        binding = inflateBinding(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets();
    }

    @NonNull
    protected abstract VB inflateBinding(@NonNull LayoutInflater inflater);

    @NonNull
    protected final VB getBinding() {
        return binding;
    }

    /**
     * Vista cuyo fondo se extiende bajo la status bar. Sin AppBar, es el cuerpo quien baja.
     */
    @Nullable
    protected View getAppBar() {
        return null;
    }

    /**
     * Vista que aloja el contenido de la pantalla y debe quedar libre de barras y teclado.
     */
    @NonNull
    protected View getBody() {
        return binding.getRoot();
    }

    /**
     * Aparta el contenido de las barras del sistema, necesario al trabajar de borde a borde.
     * <p>
     * Con {@link EdgeToEdge} la ventana se dibuja detrás de la status bar, la barra de navegación y
     * el notch, y Android ya no desplaza el contenido por nosotros. A partir de targetSdk 35 el
     * sistema lo impone aunque no se active, así que este reparto de márgenes es obligatorio.
     * <p>
     * Los insets se consumen para que ninguna vista hija vuelva a aplicarlos.
     */
    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, windowInsets) -> {
            Insets bars = SystemBarInsets.of(windowInsets);
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            onApplySystemBarInsets(bars, ime);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * Reparte los márgenes entre AppBar y cuerpo. Las pantallas con una distribución distinta pueden
     * sobrescribirlo.
     * <p>
     * El fondo del AppBar se extiende bajo la status bar y solo su contenido baja lo necesario. El
     * cuerpo deja libre abajo lo que ocupe más, la botonera o el teclado: con edge-to-edge
     * {@code adjustResize} ya no redimensiona la ventana, por eso el teclado se gestiona aquí. Se
     * toma el máximo y no la suma porque el teclado se dibuja por encima de la botonera.
     */
    protected void onApplySystemBarInsets(@NonNull Insets bars, @NonNull Insets ime) {
        View appBar = getAppBar();
        int bodyTop = bars.top;
        if (appBar != null) {
            appBar.setPadding(bars.left, bars.top, bars.right, 0);
            bodyTop = 0;
        }
        getBody().setPadding(bars.left, bodyTop, bars.right, Math.max(bars.bottom, ime.bottom));
    }
}
