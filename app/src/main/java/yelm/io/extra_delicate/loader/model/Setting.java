package yelm.io.extra_delicate.loader.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

public class Setting {

    @SerializedName("theme")
    @Expose
    private String theme;
    @SerializedName("foreground")
    @Expose
    private String foreground;
    @SerializedName("theme_category")
    @Expose
    private String themeCategory;
    @SerializedName("min_order_price")
    @Expose
    private String minOrderPrice;
    @SerializedName("min_delivery_price")
    @Expose
    private String minDeliveryPrice;
    @SerializedName("region_code")
    @Expose
    private String regionCode;
    @SerializedName("public_id")
    @Expose
    private String publicId;
    @SerializedName("app_version")
    @Expose
    private String appVersion;
    @SerializedName("payment")
    @Expose
    private Payment payment;

    public String getThemeCategory() {
        return themeCategory;
    }

    public void setThemeCategory(String themeCategory) {
        this.themeCategory = themeCategory;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getForeground() {
        return foreground;
    }

    public void setForeground(String foreground) {
        this.foreground = foreground;
    }

    public String getMinOrderPrice() {
        return minOrderPrice;
    }

    public void setMinOrderPrice(String minOrderPrice) {
        this.minOrderPrice = minOrderPrice;
    }

    public String getMinDeliveryPrice() {
        return minDeliveryPrice;
    }

    public void setMinDeliveryPrice(String minDeliveryPrice) {
        this.minDeliveryPrice = minDeliveryPrice;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }


    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    @NotNull
    @Override
    public String toString() {
        return "Setting{" +
                "theme='" + theme + '\'' +
                ", foreground='" + foreground + '\'' +
                ", themeCategory='" + themeCategory + '\'' +
                ", minOrderPrice='" + minOrderPrice + '\'' +
                ", minDeliveryPrice='" + minDeliveryPrice + '\'' +
                ", regionCode='" + regionCode + '\'' +
                ", publicId='" + publicId + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", payment=" + payment +
                '}';
    }
}
