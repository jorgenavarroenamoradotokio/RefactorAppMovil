package com.redur.electra.ui.toolbar;

import androidx.annotation.NonNull;

import com.google.android.material.appbar.MaterialToolbar;
import com.redur.electra.R;

public final class HomeToolbarController {

    /**
     * Acciones del menú de la toolbar de Home. Quien crea el controller decide qué hacer en cada una.
     */
    public interface Listener {
        void onProfileClick();

        void onEnvironmentClick();
    }

    private final MaterialToolbar toolbar;
    private final Listener listener;

    public HomeToolbarController(@NonNull MaterialToolbar toolbar, @NonNull Listener listener) {
        this.toolbar = toolbar;
        this.listener = listener;
    }

    public void setup() {
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.action_profile) {
                listener.onProfileClick();
                return true;
            }

            if (id == R.id.action_environment) {
                listener.onEnvironmentClick();
                return true;
            }

            return false;
        });
    }
}
