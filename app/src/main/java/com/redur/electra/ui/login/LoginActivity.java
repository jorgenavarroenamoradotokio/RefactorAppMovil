package com.redur.electra.ui.login;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.redur.electra.BuildConfig;
import com.redur.electra.R;
import com.redur.electra.core.ui.BasicActivity;
import com.redur.electra.core.ui.UiState;
import com.redur.electra.core.ui.UiText;
import com.redur.electra.databinding.ActivityLoginBinding;
import com.redur.electra.ui.home.HomeActivity;

import java.util.function.Consumer;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends BasicActivity<ActivityLoginBinding> {

    private LoginViewModel viewModel;
    // Paddings del layout: los insets se suman a ellos, no los sustituyen
    private int headerPaddingTop;
    private int formPaddingBottom;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Se leen antes del primer reparto de insets, que llega en el primer layout
        headerPaddingTop = getBinding().header.getPaddingTop();
        formPaddingBottom = getBinding().containerForm.getPaddingBottom();

        // La transición de entrada solo se reproduce en la primera apertura
        if (savedInstanceState == null) {
            playEnterAnimation();
        }

        getBinding().textVersion.setText(getString(R.string.login_version, BuildConfig.VERSION_NAME));
        setupListeners();

        viewModel.getFormState().observe(this, this::renderForm);
        viewModel.getLoginState().observe(this, this::renderLogin);
    }

    @NonNull
    @Override
    protected ActivityLoginBinding inflateBinding(@NonNull LayoutInflater inflater) {
        return ActivityLoginBinding.inflate(inflater);
    }

    /**
     * La franja de marca se extiende bajo la status bar y el scroll se acorta con el teclado. Se usa
     * margen y no padding porque NestedScrollView calcula lo visible con su altura total; al abrirse
     * el teclado se desplaza además el campo con foco completo.
     */
    @Override
    protected void onApplySystemBarInsets(@NonNull Insets bars, @NonNull Insets ime) {
        ActivityLoginBinding binding = getBinding();
        View root = binding.getRoot();
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) root.getLayoutParams();
        boolean imeOpened = ime.bottom > 0 && params.bottomMargin == 0;

        if (params.bottomMargin != ime.bottom) {
            params.bottomMargin = ime.bottom;
            root.setLayoutParams(params);
        }
        root.setPadding(bars.left, 0, bars.right, 0);

        binding.header.setPadding(
                binding.header.getPaddingLeft(),
                headerPaddingTop + bars.top,
                binding.header.getPaddingRight(),
                binding.header.getPaddingBottom());

        // Con el teclado abierto, su inset ya cubre la barra de navegación
        binding.containerForm.setPadding(
                binding.containerForm.getPaddingLeft(),
                binding.containerForm.getPaddingTop(),
                binding.containerForm.getPaddingRight(),
                formPaddingBottom + Math.max(0, bars.bottom - ime.bottom));

        // Tras el layout con el nuevo margen: antes, el scroll aún no conoce su nueva altura
        if (imeOpened) {
            root.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    view.removeOnLayoutChangeListener(this);
                    scrollFocusedFieldIntoView();
                }
            });
        }
    }

    /**
     * Muestra el campo completo (incluida su línea de error), no solo el cursor.
     */
    private void scrollFocusedFieldIntoView() {
        View focus = getCurrentFocus();
        if (focus == null) return;
        View target = focus;
        while (!(target instanceof TextInputLayout) && target.getParent() instanceof View parent) {
            target = parent;
        }
        if (!(target instanceof TextInputLayout)) {
            target = focus;
        }
        target.requestRectangleOnScreen(new Rect(0, 0, target.getWidth(), target.getHeight()));
    }

    /**
     * Entrada del panel desde abajo, breve y con ease-out.
     */
    private void playEnterAnimation() {
        View panel = getBinding().containerForm;
        panel.setAlpha(0f);
        panel.setTranslationY(getResources().getDimension(R.dimen.motion_enter_offset));
        panel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(getResources().getInteger(R.integer.motion_duration_enter))
                .setInterpolator(new DecelerateInterpolator());
    }

    private void setupListeners() {
        ActivityLoginBinding binding = getBinding();
        binding.buttonLogin.setOnClickListener(view -> submitLogin());

        // La acción "Hecho" del teclado en la contraseña envía el formulario
        binding.inputPassword.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitLogin();
                return true;
            }
            return false;
        });

        // Al corregir un campo desaparece su error
        binding.inputUsername.addTextChangedListener(afterChanged(viewModel::onUsernameChanged));
        binding.inputPassword.addTextChangedListener(afterChanged(viewModel::onPasswordChanged));

        binding.buttonForgotPassword.setOnClickListener(view -> Snackbar
                .make(binding.getRoot(), R.string.login_forgot_password_unavailable, Snackbar.LENGTH_LONG)
                .show());
    }

    /**
     * El foco solo se mueve como respuesta al envío, nunca al re-renderizar mientras se escribe.
     */
    private void submitLogin() {
        LoginFormState validation = viewModel.onLoginClicked(
                textOf(getBinding().inputUsername),
                textOf(getBinding().inputPassword));

        if (validation.isValid()) {
            setKeyboardVisible(getBinding().getRoot(), false);
        } else {
            View field = validation.usernameError() != null
                    ? getBinding().inputUsername
                    : getBinding().inputPassword;
            field.requestFocus();
            setKeyboardVisible(field, true);
        }
    }

    private void setKeyboardVisible(@NonNull View view, boolean visible) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), view);
        if (visible) {
            controller.show(WindowInsetsCompat.Type.ime());
        } else {
            controller.hide(WindowInsetsCompat.Type.ime());
        }
    }

    /**
     * Los errores aparecen animados. setError(null) mantiene reservado el hueco del error: al
     * corregir un campo el formulario no salta bajo el dedo del usuario.
     */
    private void renderForm(@NonNull LoginFormState state) {
        if (!state.isValid()) {
            beginLayoutTransition();
        }
        showError(getBinding().layoutUsername, state.usernameError());
        showError(getBinding().layoutPassword, state.passwordError());
    }

    private void renderLogin(@NonNull UiState state) {
        ActivityLoginBinding binding = getBinding();
        boolean isLoading = state instanceof UiState.Loading;

        // Durante la petición el formulario queda bloqueado para evitar envíos duplicados
        binding.inputUsername.setEnabled(!isLoading);
        binding.inputPassword.setEnabled(!isLoading);
        binding.buttonForgotPassword.setEnabled(!isLoading);
        binding.buttonLogin.setEnabled(!isLoading);

        // El indicador sustituye al texto: la carga se comunica donde el usuario actuó
        binding.buttonLogin.setText(isLoading ? null : getString(R.string.login_button));
        binding.progress.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);

        // Durante un reintento el aviso anterior se mantiene hasta conocer el nuevo resultado
        if (state instanceof UiState.Error error) {
            showLoginError(error.message());
        } else if (!isLoading) {
            showLoginError(null);
        }

        if (state instanceof UiState.Success) {
            navigateToHome();
        }
    }

    /**
     * Inicio pasa a ser la raíz de la tarea: volver atrás desde ella no regresa al login.
     */
    private void navigateToHome() {
        startActivity(new Intent(this, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    private void showError(@NonNull TextInputLayout layout, @Nullable Integer errorRes) {
        layout.setError(errorRes != null ? getString(errorRes) : null);
    }

    /**
     * El aviso permanece mientras se reintenta: ocultarlo en Loading lanzaba un fade-out que, si la
     * respuesta llegaba antes de terminar, quedaba interrumpido y dejaba la tarjeta invisible.
     */
    private void showLoginError(@Nullable UiText message) {
        // El texto se fija antes de mostrarse para que TalkBack anuncie el mensaje correcto
        if (message != null) {
            getBinding().textLoginError.setText(message.resolve(this));
        }
        int visibility = message != null ? View.VISIBLE : View.GONE;
        if (getBinding().cardLoginError.getVisibility() != visibility) {
            beginLayoutTransition();
            getBinding().cardLoginError.setVisibility(visibility);
        }
    }

    /**
     * Anima el siguiente cambio de layout del formulario. Si aún no se ha dibujado (al recrearse la
     * Activity se restaura el estado antes del primer layout) se pinta directamente, sin animar.
     */
    private void beginLayoutTransition() {
        ViewGroup form = getBinding().containerForm;
        if (!form.isLaidOut()) return;
        Transition transition = new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(new Fade())
                .addTransition(new ChangeBounds())
                .setDuration(getResources().getInteger(R.integer.motion_duration_layout))
                .setInterpolator(new DecelerateInterpolator());
        TransitionManager.beginDelayedTransition(form, transition);
    }

    @NonNull
    private static String textOf(@NonNull EditText input) {
        Editable text = input.getText();
        return text != null ? text.toString() : "";
    }

    @NonNull
    private static TextWatcher afterChanged(@NonNull Consumer<String> action) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                action.accept(s.toString());
            }
        };
    }
}
