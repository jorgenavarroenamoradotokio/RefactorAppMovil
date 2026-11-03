package com.redur.electra.data.remote.dto.request.piece.dock;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public record DockRequestDT0(@SerializedName("userName") String username,
                             String password,
                             String language,
                             String barcode) {

    @NonNull
    @Override
    public String toString() {
        return "DockRequestDT0[username=" + username + ", password=****, language=" + language + ", barcode=" + barcode + "]";
    }
}

