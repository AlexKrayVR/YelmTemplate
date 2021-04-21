package yelm.io.extra_delicate.support_stuff;

import android.util.Log;

import yelm.io.extra_delicate.BuildConfig;

public class Logging {
    private static String error = "AppError";
    private static String debug = "AppDebug";

    public static void logDebug(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(Logging.debug, message);
        }
    }

    public static void logError(String message) {
        if (BuildConfig.DEBUG) {
            Log.e(Logging.error, message);
        }
    }
}