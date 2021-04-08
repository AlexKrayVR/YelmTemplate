package yelm.io.raccoon.support_stuff;

import android.util.Log;

import yelm.io.raccoon.BuildConfig;

public class Logging {
    private static String error = "AlexError";
    private static String debug = "AlexDebug";

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