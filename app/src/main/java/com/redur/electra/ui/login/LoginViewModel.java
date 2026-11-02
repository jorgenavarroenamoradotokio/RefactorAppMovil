package com.redur.electra.ui.login;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.redur.electra.R;
import com.redur.electra.core.ui.UiState;

import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final MutableLiveData<LoginFormState> formState = new MutableLiveData<>(LoginFormState.EMPTY);
    private final MutableLiveData<UiState> loginState = new MutableLiveData<>(new UiState.Idle());

    @Inject
    public LoginViewModel() {
    }

    /**
     * Estado del formulario
     */
    public LiveData<LoginFormState> getFormState() {
        return formState;
    }

    /**
     * Estado de la petición API (Idle → Loading → Success/Error).
     */
    public LiveData<UiState> getLoginState() {
        return loginState;
    }

    /**
     * Valida el formulario y, si es correcto, inicia sesión. Devuelve el resultado de la validación
     * para que la UI decida dónde dejar el foco. Mientras hay una petición en curso no hace nada.
     */
    @NonNull
    public LoginFormState onLoginClicked(@NonNull String username, @NonNull String password) {
        if (loginState.getValue() instanceof UiState.Loading) {
            return currentState();
        }

        LoginFormState validation = new LoginFormState(usernameError(username), passwordError(password));
        formState.setValue(validation);

        if (validation.isValid()) {
            // Pendiente: aún no existe capa de autenticación contra la API; se acepta el acceso
            loginState.setValue(new UiState.Success());
        }
        return validation;
    }

    /**
     * Mientras se escribe los errores solo desaparecen, nunca aparecen: se quitan cuando el valor
     * ya es válido. Así, al recrearse la Activity, el texto restaurado no borra el error vigente.
     */
    public void onUsernameChanged(@NonNull String username) {
        LoginFormState current = currentState();
        if (current.usernameError() != null && usernameError(username) == null) {
            formState.setValue(new LoginFormState(null, current.passwordError()));
        }
    }

    public void onPasswordChanged(@NonNull String password) {
        LoginFormState current = currentState();
        if (current.passwordError() != null && passwordError(password) == null) {
            formState.setValue(new LoginFormState(current.usernameError(), null));
        }
    }

    @NonNull
    private LoginFormState currentState() {
        return Objects.requireNonNullElse(formState.getValue(), LoginFormState.EMPTY);
    }

    @Nullable
    @StringRes
    private static Integer usernameError(@NonNull String username) {
        return username.isBlank() ? R.string.login_username_required : null;
    }

    // Los espacios pueden formar parte de una contraseña: solo se exige que no esté vacía
    @Nullable
    @StringRes
    private static Integer passwordError(@NonNull String password) {
        return password.isEmpty() ? R.string.login_password_required : null;
    }
}
