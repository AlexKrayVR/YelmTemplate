package yelm.io.raccoon.loader.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Payment {
    @SerializedName("card")
    @Expose
    private Boolean card;
    @SerializedName("applepay")
    @Expose
    private Boolean applepay;
    @SerializedName("placeorder")
    @Expose
    private Boolean placeorder;

    public Boolean getCard() {
        return card;
    }

    public void setCard(Boolean card) {
        this.card = card;
    }

    public Boolean getApplepay() {
        return applepay;
    }

    public void setApplepay(Boolean applepay) {
        this.applepay = applepay;
    }

    public Boolean getPlaceorder() {
        return placeorder;
    }

    public void setPlaceorder(Boolean placeorder) {
        this.placeorder = placeorder;
    }

}
