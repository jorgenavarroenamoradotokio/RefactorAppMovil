package com.redur.electra.core.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.redur.electra.R;
import com.redur.electra.core.error.AppError;
import com.redur.electra.core.error.Device;
import com.redur.electra.core.error.Network;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class ErrorUiMapper {

    private static final Map<String, Integer> API_CODES = createApiCodes();

    private ErrorUiMapper() {
    }

    private static Map<String, Integer> createApiCodes() {
        Map<String, Integer> codes = new HashMap<>();

        codes.put("ERROR_S01_ERROR_INTERNO_DEL_SERVIDOR", R.string.error_unknown);
        codes.put("ERROR_S02_ERROR_DATOS_NO_LOCALIZADOS", R.string.error_response_not_data_found);
        codes.put("ERROR_S03_ERROR_REQUEST_VACIA", R.string.error_response_body_request_empty);

        codes.put("HTTP_403", R.string.error_network_403);
        codes.put("HTTP_500", R.string.error_unknown);
        codes.put("HTTP_503", R.string.error_network_503);

        codes.put("ERROR_A01", R.string.error_login_failed);
        codes.put("ERROR_A05", R.string.error_user_not_active);
        codes.put("ERROR_A06", R.string.error_user_not_active);

        codes.put("ERROR_M01", R.string.error_user_menu_not_configuration);
        codes.put("ERROR_F01", R.string.error_file_not_upload);
        codes.put("ERROR_MU01", R.string.error_dock_barcode_empty);
        codes.put("ERROR_I01", R.string.error_incidence_aplication_code_empty);

        return Collections.unmodifiableMap(codes);
    }

    @NonNull
    public static UiText toUiText(@NonNull AppError error) {
        if (error instanceof AppError.NetworkError network) {
            return new UiText.Res(mapNetwork(network.type()));
        }

        if (error instanceof AppError.ApiError api) {
            return mapApi(api.code(), api.serverMessage());
        }

        if (error instanceof AppError.DeviceError device) {
            return new UiText.Res(mapDevice(device.failure()));
        }

        return new UiText.Res(R.string.error_unknown);
    }

    @NonNull
    private static UiText mapApi(@Nullable String code, @Nullable String serverMessage) {
        Integer resource = code == null ? null : API_CODES.get(code);
        if (resource != null) {
            return new UiText.Res(resource);
        }

        // Solo se registra el código: el mensaje del servidor puede contener datos personales.
        Timber.w("Código de error de API no mapeado: %s", code);

        if (serverMessage == null || serverMessage.isBlank()) {
            return new UiText.Res(R.string.error_unknown);
        }
        return new UiText.Raw(serverMessage);
    }


    @StringRes
    private static int mapNetwork(@NonNull Network type) {
        return switch (type) {
            case NO_CONNECTION -> R.string.error_network_no_connection;
            case TIMEOUT -> R.string.error_network_timeout;
        };
    }

    @StringRes
    private static int mapDevice(@NonNull Device failure) {
        return switch (failure) {
            case NOT_CONNECTED -> R.string.error_device_not_connected;
            case CONNECTION_LOST -> R.string.error_device_connection_lost;
            case PERMISSION_DENIED -> R.string.error_device_permission_denied;
            case DEVICE_BUSY -> R.string.error_device_busy;
            case OPERATION_TIMEOUT -> R.string.error_device_opertation_timeout;
            case UNSUPPORTED_DEVICE -> R.string.error_device_unsupported_device;
            case UNKNOWN -> R.string.error_unknown;
        };
    }

}
