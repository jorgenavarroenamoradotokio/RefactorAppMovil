package com.redur.electra.core.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import com.redur.electra.R;
import com.redur.electra.core.error.AppError;
import com.redur.electra.core.error.Device;
import com.redur.electra.core.error.Network;
import com.redur.electra.rule.TimberTestRule;

import org.junit.Rule;
import org.junit.Test;

public class ErrorUiMapperTest {

    @Rule
    public final TimberTestRule timber = new TimberTestRule();

    // ---------- Errores de API ----------

    @Test
    public void codigoMapeado_devuelveSuRecursoIgnorandoElMensajeDelServidor() {
        UiText text = ErrorUiMapper.toUiText(new AppError.ApiError("ERROR_A01", "Credenciales inválidas"));

        assertEquals(new UiText.Res(R.string.error_login_failed), text);
        assertTrue(timber.entries().isEmpty());
    }

    @Test
    public void http403SinCodigoDeApi_informaDeFaltaDePermisos() {
        assertEquals(new UiText.Res(R.string.error_network_403),
                ErrorUiMapper.toUiText(new AppError.ApiError("HTTP_403", null)));
    }

    @Test
    public void codigosDistintosPuedenCompartirRecurso() {
        assertEquals(
                ErrorUiMapper.toUiText(new AppError.ApiError("ERROR_C06", null)),
                ErrorUiMapper.toUiText(new AppError.ApiError("ERROR_CCB_04_BARCODE_INVALID", null)));
    }

    @Test
    public void codigoNoMapeadoConMensaje_muestraElMensajeDelServidorYRegistraSoloElCodigo() {
        UiText text = ErrorUiMapper.toUiText(new AppError.ApiError("ERROR_X99", "Pedido bloqueado para Ana"));

        assertEquals(new UiText.Raw("Pedido bloqueado para Ana"), text);
        assertTrue(timber.contains(Log.WARN, "ERROR_X99"));
        assertFalse(timber.contains(Log.WARN, "Ana"));
    }

    @Test
    public void codigoNoMapeadoSinMensaje_devuelveErrorGenerico() {
        assertEquals(new UiText.Res(R.string.error_unknown),
                ErrorUiMapper.toUiText(new AppError.ApiError("ERROR_X99", null)));
        assertEquals(new UiText.Res(R.string.error_unknown),
                ErrorUiMapper.toUiText(new AppError.ApiError("ERROR_X99", "   ")));
    }

    @Test
    public void codigoNulo_noFallaYUsaElMensajeDelServidor() {
        assertEquals(new UiText.Raw("Servicio en mantenimiento"),
                ErrorUiMapper.toUiText(new AppError.ApiError(null, "Servicio en mantenimiento")));
    }

    // ---------- Errores de red ----------

    @Test
    public void errorDeRed_sinConexion() {
        assertEquals(new UiText.Res(R.string.error_network_no_connection),
                ErrorUiMapper.toUiText(new AppError.NetworkError(Network.NO_CONNECTION)));
    }

    @Test
    public void errorDeRed_timeout() {
        assertEquals(new UiText.Res(R.string.error_network_timeout),
                ErrorUiMapper.toUiText(new AppError.NetworkError(Network.TIMEOUT)));
    }

    @Test
    public void todoTipoDeRedTieneUnMensajePropio() {
        for (Network type : Network.values()) {
            UiText text = ErrorUiMapper.toUiText(new AppError.NetworkError(type));
            assertNotEquals(type.name(), new UiText.Res(R.string.error_unknown), text);
        }
    }

    @Test
    public void falloDeImpresora_explicaQueHacerSegunElMotivo() {
        assertEquals(new UiText.Res(R.string.error_device_not_connected),
                ErrorUiMapper.toUiText(new AppError.DeviceError(Device.NOT_CONNECTED)));
        assertEquals(new UiText.Res(R.string.error_device_opertation_timeout),
                ErrorUiMapper.toUiText(new AppError.DeviceError(Device.OPERATION_TIMEOUT)));
    }
}
