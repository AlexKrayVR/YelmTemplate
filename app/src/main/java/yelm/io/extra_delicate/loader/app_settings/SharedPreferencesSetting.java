package yelm.io.extra_delicate.loader.app_settings;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesSetting {

    public static final String USER_NAME = "USER_NAME";

    //app settings
    public static final String MIN_PRICE_FOR_FREE_DELIVERY = "MIN_PRICE_FOR_FREE_DELIVERY";
    public static final String APP_COLOR = "APP_COLOR";
    public static final String APP_TEXT_COLOR = "APP_TEXT_COLOR";
    public static final String CATEGORY_TEXT_COLOR = "CATEGORY_TEXT_COLOR";
    public static final String MIN_ORDER_PRICE = "MIN_ORDER_PRICE";
    public static final String PRICE_IN = "PRICE_IN";
    public static final String CURRENCY = "CNT";
    public static final String COUNTRY_CODE = "COUNTRY_CODE";
    public static final String PAYMENT_CARD = "PAYMENT_CARD";
    public static final String PAYMENT_MOBILE = "PAYMENT_MOBILE";
    public static final String PAYMENT_CASH = "PAYMENT_CASH";

    //chat settings
    public static final String API_TOKEN = "API_TOKEN";
    public static final String ROOM_CHAT_ID = "ROOM_ID";
    public static final String SHOP_CHAT_ID = "SHOP_ID";
    public static final String CLIENT_CHAT_ID = "CLIENT_ID";

    //promo-code settings
    public static final String DISCOUNT_TYPE = "DISCOUNT_TYPE";
    public static final String DISCOUNT_AMOUNT = "DISCOUNT_AMOUNT";
    public static final String DISCOUNT_NAME = "DISCOUNT_NAME";

    private static SharedPreferences settings = null;
    private static final String APP_PREFERENCES = "settings";

    public static void initSharedPreferencesSettings(Context context) {
        settings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    public static SharedPreferences getSettings(){
        return settings;
    }

    public static void setData(String key, String data) {
        settings.edit().putString(key, data).apply();
    }

    public static void setData(String key, Boolean data) {
        settings.edit().putBoolean(key, data).apply();
    }

    public static String getDataString(String key) {
        return settings.getString(key, "");
    }

    public static Boolean getDataBoolean(String key) {
        return settings.getBoolean(key, false);
    }


}