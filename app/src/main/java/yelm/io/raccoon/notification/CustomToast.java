package yelm.io.raccoon.notification;

import android.content.Context;
import android.widget.Toast;

public class CustomToast {
    private static Toast toast;
    public static void showStatus(Context context, String message) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }
}
