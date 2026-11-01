package com.redur.electra.ui.sheet;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.redur.electra.databinding.BottomGenericSheetBinding;
import com.redur.electra.databinding.ItemInfoDetailBinding;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Hoja inferior reutilizable: icono, título, mensaje, filas de detalle (etiqueta/valor) y hasta
 * tres acciones apiladas: principal, destructiva (en rojo) y secundaria. Sirve tanto para confirmar (cerrar sesión, imprimir) como para informar
 * (entorno conectado).
 * <p>
 * El contenido viaja en los argumentos, así que la hoja sobrevive a rotaciones y a la muerte
 * del proceso. Por el mismo motivo la acción pulsada no se notifica con un callback, sino con el
 * Fragment Result API: quien la muestra escucha con
 * {@link #setOnActionListener(FragmentManager, String, LifecycleOwner, OnActionListener)} usando
 * el mismo {@code requestKey}. Cerrarla sin pulsar ninguna acción no emite resultado.
 */
public class GenericBottomSheet extends BottomSheetDialogFragment {

    /**
     * Acción que ha cerrado la hoja.
     */
    public enum Action {
        PRIMARY,
        DESTRUCTIVE,
        SECONDARY
    }

    public interface OnActionListener {
        void onAction(@NonNull Action action);
    }

    private static final String ARG_REQUEST_KEY = "request_key";
    private static final String ARG_ICON = "icon";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_DETAIL_LABELS = "detail_labels";
    private static final String ARG_DETAIL_VALUES = "detail_values";
    private static final String ARG_PRIMARY = "primary";
    private static final String ARG_DESTRUCTIVE = "destructive";
    private static final String ARG_SECONDARY = "secondary";
    /**
     * Sufijo para guardar un texto como recurso: se resuelve al pintar y respeta el idioma.
     */
    private static final String SUFFIX_RES = "_res";

    private static final String RESULT_ACTION = "action";

    @Nullable
    private BottomGenericSheetBinding binding;
    private boolean actionDelivered;

    @NonNull
    public static Builder builder(@NonNull String requestKey) {
        return new Builder(requestKey);
    }

    /**
     * Registra quién recibe la acción pulsada. Debe llamarse en {@code onCreate} (Activity) o en
     * {@code onViewCreated} (Fragment) para volver a engancharse tras una recreación.
     */
    public static void setOnActionListener(@NonNull FragmentManager fragmentManager,
                                           @NonNull String requestKey,
                                           @NonNull LifecycleOwner owner,
                                           @NonNull OnActionListener listener) {
        fragmentManager.setFragmentResultListener(requestKey, owner, (key, result) ->
                listener.onAction(Action.valueOf(result.getString(RESULT_ACTION))));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        // El contenido es corto: se abre entera, sin el estado intermedio de media altura
        dialog.getBehavior().setSkipCollapsed(true);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        return dialog;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomGenericSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BottomGenericSheetBinding b = Objects.requireNonNull(binding);
        Bundle args = requireArguments();

        int icon = args.getInt(ARG_ICON);
        b.imageInfoIcon.setVisibility(icon != 0 ? View.VISIBLE : View.GONE);
        if (icon != 0) {
            b.imageInfoIcon.setImageResource(icon);
        }

        bindText(b.textInfoTitle, readText(args, ARG_TITLE));
        bindText(b.textInfoMessage, readText(args, ARG_MESSAGE));
        bindDetails(b, args);
        bindText(b.buttonInfoPrimary, readText(args, ARG_PRIMARY));
        bindText(b.buttonInfoDestructive, readText(args, ARG_DESTRUCTIVE));
        bindText(b.buttonInfoSecondary, readText(args, ARG_SECONDARY));

        b.buttonInfoPrimary.setOnClickListener(v -> deliver(Action.PRIMARY));
        b.buttonInfoDestructive.setOnClickListener(v -> deliver(Action.DESTRUCTIVE));
        b.buttonInfoSecondary.setOnClickListener(v -> deliver(Action.SECONDARY));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void bindDetails(@NonNull BottomGenericSheetBinding b, @NonNull Bundle args) {
        ArrayList<Integer> labels = args.getIntegerArrayList(ARG_DETAIL_LABELS);
        ArrayList<String> values = args.getStringArrayList(ARG_DETAIL_VALUES);
        boolean hasDetails = labels != null && values != null && !labels.isEmpty();
        b.layoutInfoDetails.setVisibility(hasDetails ? View.VISIBLE : View.GONE);
        if (!hasDetails) {
            return;
        }
        b.layoutInfoDetails.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < labels.size(); i++) {
            ItemInfoDetailBinding row = ItemInfoDetailBinding.inflate(inflater, b.layoutInfoDetails, true);
            row.textDetailLabel.setText(labels.get(i));
            row.textDetailValue.setText(values.get(i));
        }
    }

    /**
     * Entrega el resultado y cierra. Solo la primera pulsación cuenta: una doble pulsación (o
     * pulsar la otra acción mientras la hoja se cierra) no emite un segundo resultado.
     */
    private void deliver(@NonNull Action action) {
        if (actionDelivered) {
            return;
        }
        actionDelivered = true;

        Bundle result = new Bundle();
        result.putString(RESULT_ACTION, action.name());
        getParentFragmentManager().setFragmentResult(requireArguments().getString(ARG_REQUEST_KEY), result);
        dismiss();
    }

    @Nullable
    private CharSequence readText(@NonNull Bundle args, @NonNull String key) {
        int res = args.getInt(key + SUFFIX_RES);
        return res != 0 ? getText(res) : args.getCharSequence(key);
    }

    private static void bindText(@NonNull TextView view, @Nullable CharSequence text) {
        view.setVisibility(text != null ? View.VISIBLE : View.GONE);
        view.setText(text);
    }

    /**
     * Construye la hoja. Son obligatorios el título y al menos una acción principal o
     * destructiva; lo que no se indique se oculta (también el botón secundario).
     */
    public static final class Builder {

        private final String requestKey;
        private final Bundle args = new Bundle();
        private final ArrayList<Integer> detailLabels = new ArrayList<>();
        private final ArrayList<String> detailValues = new ArrayList<>();

        private Builder(@NonNull String requestKey) {
            this.requestKey = Objects.requireNonNull(requestKey, "requestKey");
            args.putString(ARG_REQUEST_KEY, requestKey);
        }

        @NonNull
        public Builder icon(@DrawableRes int icon) {
            args.putInt(ARG_ICON, icon);
            return this;
        }

        @NonNull
        public Builder title(@StringRes int title) {
            args.putInt(ARG_TITLE + SUFFIX_RES, title);
            return this;
        }

        @NonNull
        public Builder title(@NonNull CharSequence title) {
            args.putCharSequence(ARG_TITLE, title);
            return this;
        }

        @NonNull
        public Builder message(@StringRes int message) {
            args.putInt(ARG_MESSAGE + SUFFIX_RES, message);
            return this;
        }

        /**
         * Para mensajes con datos variables, ya formateados por quien muestra la hoja.
         */
        @NonNull
        public Builder message(@NonNull CharSequence message) {
            args.putCharSequence(ARG_MESSAGE, message);
            return this;
        }

        /**
         * Añade una fila etiqueta/valor (p. ej. "Servidor" → URL). Se muestran en orden.
         */
        @NonNull
        public Builder detail(@StringRes int label, @NonNull String value) {
            detailLabels.add(label);
            detailValues.add(Objects.requireNonNull(value, "value"));
            return this;
        }

        @NonNull
        public Builder primaryAction(@StringRes int label) {
            args.putInt(ARG_PRIMARY + SUFFIX_RES, label);
            return this;
        }

        /**
         * Acción irreversible (descartar, borrar, cerrar sesión). Se pinta en rojo.
         */
        @NonNull
        public Builder destructiveAction(@StringRes int label) {
            args.putInt(ARG_DESTRUCTIVE + SUFFIX_RES, label);
            return this;
        }

        /**
         * Acción discreta para salir sin hacer nada (cancelar). Opcional: si no se indica, no aparece.
         */
        @NonNull
        public Builder secondaryAction(@StringRes int label) {
            args.putInt(ARG_SECONDARY + SUFFIX_RES, label);
            return this;
        }

        @NonNull
        public GenericBottomSheet build() {
            if (!hasText(ARG_TITLE)) {
                throw new IllegalStateException("GenericBottomSheet requiere un título");
            }
            // Sin ninguna de las dos, la hoja solo podría cerrarse arrastrando y no emitiría resultado
            if (!hasText(ARG_PRIMARY) && !hasText(ARG_DESTRUCTIVE)) {
                throw new IllegalStateException("GenericBottomSheet requiere una acción principal o destructiva");
            }
            Bundle built = new Bundle(args);
            built.putIntegerArrayList(ARG_DETAIL_LABELS, new ArrayList<>(detailLabels));
            built.putStringArrayList(ARG_DETAIL_VALUES, new ArrayList<>(detailValues));
            GenericBottomSheet sheet = new GenericBottomSheet();
            sheet.setArguments(built);
            return sheet;
        }

        /**
         * Muestra la hoja usando el {@code requestKey} como tag. Si ya hay una abierta con esa
         * clave (doble pulsación del botón que la abre) o el estado ya se ha guardado, no hace nada.
         */
        public void show(@NonNull FragmentManager fragmentManager) {
            if (fragmentManager.isStateSaved() || fragmentManager.findFragmentByTag(requestKey) != null) {
                return;
            }
            build().show(fragmentManager, requestKey);
        }

        private boolean hasText(@NonNull String key) {
            return args.containsKey(key) || args.containsKey(key + SUFFIX_RES);
        }
    }
}
