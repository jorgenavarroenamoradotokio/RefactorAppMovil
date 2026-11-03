package com.redur.electra.data.remote.dto.response.piece.dock;

import com.google.gson.annotations.SerializedName;

public record DockResponseDT0(
        @SerializedName("plzsid") String plzsId,
        @SerializedName("muelle") String muelle,
        @SerializedName("cp") String postalCode,
        @SerializedName("pais") String country

) {
}
