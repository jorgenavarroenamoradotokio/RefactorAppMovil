package com.redur.electra.data.remote.api;

import com.redur.electra.data.remote.dto.request.piece.dock.DockRequestDT0;
import com.redur.electra.data.remote.dto.response.ApiResponseDTO;
import com.redur.electra.data.remote.dto.response.piece.dock.DockResponseDT0;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PieceApiService {

    String DOCK_SEARCH_BY_CB_PATH = "ElectraWS/mobile/bulto/internacional/muelle";
    @POST(DOCK_SEARCH_BY_CB_PATH)
    Call<ApiResponseDTO<DockResponseDT0>> getPieceDock(@Body DockRequestDT0 request);
}
