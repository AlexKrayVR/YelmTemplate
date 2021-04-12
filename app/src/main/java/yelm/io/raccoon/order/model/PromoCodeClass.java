package yelm.io.raccoon.order.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import yelm.io.raccoon.order.model.PromoCode;

public class PromoCodeClass {

    @SerializedName("promocode")
    @Expose
    private PromoCode promocode;
    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("status")
    @Expose
    private String status;

    public PromoCode getPromocode() {
        return promocode;
    }

    public void setPromocode(PromoCode promocode) {
        this.promocode = promocode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
