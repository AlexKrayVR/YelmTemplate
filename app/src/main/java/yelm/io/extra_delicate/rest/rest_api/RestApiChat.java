package yelm.io.extra_delicate.rest.rest_api;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import yelm.io.extra_delicate.chat.model.ChatHistoryClass;

public interface RestApiChat {

    String URL_API_MAIN = "https://chat.yelm.io/api/message/";

    @GET("all?")
    Call<ArrayList<ChatHistoryClass>> getChatHistory(
            @Query("platform") String Platform,
            @Query("room_id") String RoomID
    );
}