package yelm.io.extra_delicate.loader.app_settings;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceInfo {
    public static JSONObject getDeviceInfo(Context context) {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        int percentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put("percentage", Integer.toString(percentage));
            jsonData.put("device", Build.DEVICE);
            jsonData.put("model", Build.MODEL);
            jsonData.put("product", Build.PRODUCT);
            jsonData.put("display", Build.DISPLAY);
            jsonData.put("brand", Build.BRAND);
            jsonData.put("user", Build.USER);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonData;
    }
}