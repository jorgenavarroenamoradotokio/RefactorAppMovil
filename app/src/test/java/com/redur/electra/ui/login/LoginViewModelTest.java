package com.redur.electra.ui.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.redur.electra.R;
import com.redur.electra.core.ui.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LoginViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutor = new InstantTaskExecutorRule();

    private LoginViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new LoginViewModel();
    }

    @Test
    public void enviarVacio_marcaAmbosCamposYNoInicia() {
        LoginFormState result = viewModel.onLoginClicked("  ", "");

        assertEquals(new LoginFormState(R.string.login_username_required, R.string.login_password_required), result);
        assertEquals(result, viewModel.getFormState().getValue());
        assertTrue(viewModel.getLoginState().getValue() instanceof UiState.Idle);
    }

    @Test
    public void passwordConEspacios_esValida() {
        LoginFormState result = viewModel.onLoginClicked("usuario", "   ");

        assertTrue(result.isValid());
    }

    @Test
    public void enviarValido_iniciaSesion() {
        LoginFormState result = viewModel.onLoginClicked("usuario", "secreto");

        assertTrue(result.isValid());
        assertTrue(viewModel.getLoginState().getValue() instanceof UiState.Success);
    }

    @Test
    public void corregirUsuario_quitaSoloSuError() {
        viewModel.onLoginClicked("", "");

        viewModel.onUsernameChanged("u");

        assertEquals(new LoginFormState(null, R.string.login_password_required), viewModel.getFormState().getValue());
    }

    @Test
    public void corregirPassword_quitaSoloSuError() {
        viewModel.onLoginClicked("", "");

        viewModel.onPasswordChanged("p");

        assertEquals(new LoginFormState(R.string.login_username_required, null), viewModel.getFormState().getValue());
    }

    @Test
    public void textoRestauradoInvalido_mantieneElError() {
        viewModel.onLoginClicked(" ", "");

        // Al recrearse la Activity el campo restaura el mismo texto y notifica el cambio
        viewModel.onUsernameChanged(" ");
        viewModel.onPasswordChanged("");

        assertEquals(new LoginFormState(R.string.login_username_required, R.string.login_password_required),
                viewModel.getFormState().getValue());
    }
}
