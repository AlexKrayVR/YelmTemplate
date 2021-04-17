package yelm.io.raccoon.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import yelm.io.raccoon.R;
import yelm.io.raccoon.chat.controller.ChatActivity;
import yelm.io.raccoon.constants.Constants;
import yelm.io.raccoon.loader.controller.LoaderActivity;
import yelm.io.raccoon.rest.rest_api.RestAPI;
import yelm.io.raccoon.rest.client.RetrofitClient;
import yelm.io.raccoon.support_stuff.Logging;

public class FcmMessageService extends FirebaseMessagingService {

    private static final int NOTIFY_ID = 101;

    @Override
    public void onNewToken(@NotNull String s) {
        super.onNewToken(s);
        NotificationChannelCreator.createNotificationChannel(this);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Logging.logDebug( "From: " + remoteMessage.getFrom());
        Logging.logDebug("remoteMessage.getData(): " + remoteMessage.getData().toString());
        Logging.logDebug( "remoteMessage.toString(): " + remoteMessage.toString());

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Logging.logDebug( "Message Notification Body: " + remoteMessage.getNotification().getBody());
            Logging.logDebug( "Message Notification Title: " + remoteMessage.getNotification().getTitle());
        }
        showNotification(remoteMessage);
    }

    private void showNotification(RemoteMessage remoteMessage) {
        Intent i = new Intent(this, LoaderActivity.class);
        String data = remoteMessage.getData().toString();
        try {
            JSONObject jsonObj = new JSONObject(data);
            String chat = jsonObj.getString("name");
            Logging.logDebug("name: " + jsonObj.getString("name"));
            if (chat.equals("chat")) {
                if (Constants.customerInChat) {
                    return;
                }else {
                    i = new Intent(this, ChatActivity.class);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        i.putExtra("data", data);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 333, i, PendingIntent.FLAG_UPDATE_CURRENT);
//FLAG_ONE_SHOT       FLAG_UPDATE_CURRENT

        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.raccoon_icon);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                getString(R.string.notificationChannelId))
                .setAutoCancel(true)
                .setContentTitle(Objects.requireNonNull(remoteMessage.getNotification()).getTitle())
                .setContentText(remoteMessage.getNotification().getBody())
                //.setLargeIcon(bitmap)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.drawable.ic_notify)
                .setColor(getResources().getColor(R.color.mainThemeColor))
                .setContentIntent(pendingIntent)
                .setSound(defaultSoundUri);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        manager.notify(NOTIFY_ID, builder.build());
    }
}