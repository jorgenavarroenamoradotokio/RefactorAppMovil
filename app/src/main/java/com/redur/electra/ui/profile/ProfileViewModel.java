package com.redur.electra.ui.profile;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import com.redur.electra.core.device.TerminalIdProvider;

import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final TerminalIdProvider terminalIdProvider;

    @Inject
    public ProfileViewModel( TerminalIdProvider terminalIdProvider) {
        this.terminalIdProvider = terminalIdProvider;
    }

    /** Identificador con el que se registra el terminal. Null si el sistema no lo facilita. */
    @Nullable
    public String getTerminalId() {
        return terminalIdProvider.getTerminalId();
    }
}
