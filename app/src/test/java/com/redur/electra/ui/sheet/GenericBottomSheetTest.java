package com.redur.electra.ui.sheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import android.app.Application;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.redur.electra.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35)
public class GenericBottomSheetTest {

    private static final String REQUEST_KEY = "test_sheet";
    private FragmentManager fragmentManager;
    private final List<GenericBottomSheet.Action> received = new ArrayList<>();

    @Before
    public void setUp() {
        ActivityController<AppCompatActivity> controller = Robolectric.buildActivity(AppCompatActivity.class);
        // Los componentes Material necesitan su tema antes de crear la Activity
        controller.get().setTheme(R.style.Theme_Electra);
        AppCompatActivity activity = controller.setup().get();
        fragmentManager = activity.getSupportFragmentManager();
        GenericBottomSheet.setOnActionListener(fragmentManager, REQUEST_KEY, activity, received::add);
    }

    @Test
    public void pulsarPrincipal_emiteResultadoYCierra() {
        GenericBottomSheet sheet = showConfirmation();

        sheet.requireView().findViewById(R.id.button_info_primary).performClick();
        ShadowLooper.idleMainLooper();

        assertEquals(List.of(GenericBottomSheet.Action.PRIMARY), received);
        assertNull(fragmentManager.findFragmentByTag(REQUEST_KEY));
    }

    @Test
    public void dobleClic_soloEmiteUnResultado() {
        GenericBottomSheet sheet = showConfirmation();
        View primary = sheet.requireView().findViewById(R.id.button_info_primary);
        View secondary = sheet.requireView().findViewById(R.id.button_info_secondary);

        primary.performClick();
        secondary.performClick();
        ShadowLooper.idleMainLooper();

        assertEquals(List.of(GenericBottomSheet.Action.PRIMARY), received);
    }

    @Test
    public void sinSecundariaNiDetalles_seOcultan() {
        GenericBottomSheet.builder(REQUEST_KEY)
                .title(R.string.toolbar_environment)
                .primaryAction(R.string.toolbar_profile)
                .show(fragmentManager);
        ShadowLooper.idleMainLooper();
        View root = findSheet().requireView();

        assertEquals(View.GONE, root.findViewById(R.id.button_info_secondary).getVisibility());
        assertEquals(View.GONE, root.findViewById(R.id.button_info_destructive).getVisibility());
        assertEquals(View.GONE, root.findViewById(R.id.layout_info_details).getVisibility());
        assertEquals(View.GONE, root.findViewById(R.id.text_info_message).getVisibility());
    }

    @Test
    public void mostrarDosVeces_noApilaHojas() {
        GenericBottomSheet.Builder builder = GenericBottomSheet.builder(REQUEST_KEY)
                .title(R.string.toolbar_environment)
                .primaryAction(R.string.toolbar_profile);

        builder.show(fragmentManager);
        ShadowLooper.idleMainLooper();
        GenericBottomSheet first = findSheet();
        builder.show(fragmentManager);
        ShadowLooper.idleMainLooper();

        assertEquals(first, findSheet());
        assertEquals(1, fragmentManager.getFragments().size());
    }

    @Test
    public void pulsarDestructiva_emiteDestructive() {
        GenericBottomSheet sheet = showConfirmation();

        sheet.requireView().findViewById(R.id.button_info_destructive).performClick();
        ShadowLooper.idleMainLooper();

        assertEquals(List.of(GenericBottomSheet.Action.DESTRUCTIVE), received);
    }

    @Test
    public void soloDestructiva_esValidaYOcultaLaPrincipal() {
        GenericBottomSheet.builder(REQUEST_KEY)
                .title(R.string.logout_title)
                .destructiveAction(R.string.logout_confirm)
                .secondaryAction(R.string.cancel)
                .show(fragmentManager);
        ShadowLooper.idleMainLooper();
        View root = findSheet().requireView();

        assertEquals(View.GONE, root.findViewById(R.id.button_info_primary).getVisibility());
        assertEquals(View.VISIBLE, root.findViewById(R.id.button_info_destructive).getVisibility());
    }

    @Test
    public void sinPrincipalNiDestructiva_falla() {
        assertThrows(IllegalStateException.class, () -> GenericBottomSheet.builder(REQUEST_KEY)
                .title(R.string.logout_title)
                .secondaryAction(R.string.cancel)
                .build());
    }

    @Test
    public void sinTitulo_falla() {
        assertThrows(IllegalStateException.class, () -> GenericBottomSheet.builder(REQUEST_KEY)
                .primaryAction(R.string.toolbar_profile)
                .build());
    }

    private GenericBottomSheet showConfirmation() {
        GenericBottomSheet.builder(REQUEST_KEY)
                .icon(R.drawable.ic_app_server)
                .title(R.string.toolbar_environment)
                .message("Mensaje")
                .detail(R.string.toolbar_environment, "DEV")
                .primaryAction(R.string.toolbar_profile)
                .destructiveAction(R.string.logout_confirm)
                .secondaryAction(R.string.toolbar_environment)
                .show(fragmentManager);
        ShadowLooper.idleMainLooper();
        return findSheet();
    }

    private GenericBottomSheet findSheet() {
        return (GenericBottomSheet) fragmentManager.findFragmentByTag(REQUEST_KEY);
    }
}
