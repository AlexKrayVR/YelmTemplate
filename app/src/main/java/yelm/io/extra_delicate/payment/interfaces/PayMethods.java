package yelm.io.extra_delicate.payment.interfaces;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import yelm.io.extra_delicate.payment.models.PayRequestArgs;
import yelm.io.extra_delicate.payment.models.Post3dsRequestArgs;
import yelm.io.extra_delicate.payment.models.Transaction;
import yelm.io.extra_delicate.payment.response.PayApiResponse;
import yelm.io.extra_delicate.rest.rest_api.RestAPI;

public interface PayMethods {
    @POST("charge?platform=" + RestAPI.PLATFORM_NUMBER)
    Observable<PayApiResponse<Transaction>> charge(@Header("Content-Type") String contentType, @Body PayRequestArgs args);

    @POST("cryptogram?platform=" + RestAPI.PLATFORM_NUMBER)
    Observable<PayApiResponse<Transaction>> auth(@Header("Content-Type") String contentType, @Body PayRequestArgs args);

    @POST("processing?platform=" + RestAPI.PLATFORM_NUMBER)
    Observable<PayApiResponse<Transaction>> post3ds(@Header("Content-Type") String contentType, @Body Post3dsRequestArgs args);
}