package yelm.io.extra_delicate.payment.response;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import io.reactivex.Observable;
import yelm.io.extra_delicate.support_stuff.Logging;

public class PayApiResponse<T> {

    @SerializedName("Success")
    private boolean success;

    @SerializedName("Message")
    private String message;

    @Nullable
    @SerializedName("Model")
    private T data;

    @Nullable
    public T getData() {
        return data;
    }

    public boolean isSuccess() {
        if (success == false && data == null)
            return false;
        else if (success == false && data != null)
            return true;
        else
            return success;
    }

    public Observable<PayApiResponse<T>> handleError() {
        if (isSuccess()) {
            return Observable.just(this);
        } else {
            Logging.logDebug("handleError");
            return Observable.error(new PayApiError(message));
        }
    }
}
