package yelm.io.extra_delicate.chat.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.chat.adapter.ChatAdapter;
import yelm.io.extra_delicate.chat.model.ChatContent;
import yelm.io.extra_delicate.chat.model.ChatHistoryClass;
import yelm.io.extra_delicate.constants.Constants;
import yelm.io.extra_delicate.databinding.ActivityChatBinding;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.main.model.Item;
import yelm.io.extra_delicate.rest.rest_api.RestAPI;
import yelm.io.extra_delicate.rest.rest_api.RestApiChat;
import yelm.io.extra_delicate.rest.client.RetrofitClientChat;
import yelm.io.extra_delicate.support_stuff.Logging;

public class ChatActivity extends AppCompatActivity implements PickImageBottomSheet.BottomSheetShopListener, PickImageBottomSheet.CameraListener {

    ActivityChatBinding binding;
    ChatAdapter chatAdapter;

    ArrayList<ChatContent> chatContentList = new ArrayList<>();
    PickImageBottomSheet pickImageBottomSheet = new PickImageBottomSheet();

    private static final int REQUEST_TAKE_PHOTO = 11;

    private static final String[] READ_WRITE_EXTERNAL_PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    private static final String[] CAMERA_PERMISSIONS = new String[]{Manifest.permission.CAMERA};


    private static final int REQUEST_PERMISSIONS_READ_WRITE_STORAGE = 100;
    private static final int REQUEST_PERMISSIONS_CAMERA = 10;

    SimpleDateFormat currentFormatterDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    SimpleDateFormat printedFormatterDate = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public static final String CHAT_SERVER_URL = "https://chat.yelm.io/";
    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCustomColor();

        Constants.customerInChat = true;
        tuneChatRecycler();
        binding();
        tuneSocketConnection();
        getChatHistory();
    }

    private void setCustomColor() {
        binding.back.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.choosePicture.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.sendMessage.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.progress.getIndeterminateDrawable().setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)), PorterDuff.Mode.SRC_IN);
        binding.back.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.choosePicture.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.sendMessage.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
    }

    private void tuneSocketConnection() {
        try {
            IO.Options options = new IO.Options();
            options.query = "token=" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.API_TOKEN)
                    + "&room_id=" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID)
                    + "&user=Client";
            socket = IO.socket(CHAT_SERVER_URL, options);
            socket.on("room." + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID), onLogin);
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Logging.logDebug("error: " + e.getMessage());
        }
    }

    private Emitter.Listener onLogin = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            ChatActivity.this.runOnUiThread(() -> {
                JSONObject data = (JSONObject) args[0];
                Logging.logDebug("data: " + data.toString());

                if (data.has("role")) {
                    try {
                        String role = data.getString("role");
                        Logging.logDebug("role: " + role);
                        String type = data.getString("type");
                        Logging.logDebug("type: " + type);
                        if (data.getString("type").equals("connected")) {
                            binding.chatStatus.setText(getText(R.string.chatActivityOnline));
                            binding.chatStatus.setTextColor(getResources().getColor(R.color.colorAcceptOrder));
                        } else {
                            binding.chatStatus.setText(getText(R.string.chatActivityOffline));
                            binding.chatStatus.setTextColor(getResources().getColor(R.color.colorRedDiscount));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Logging.logDebug("JSONException: " + e.getMessage());
                    }
                    return;
                }

                try {
                    if (data.getString("from_whom").equals(SharedPreferencesSetting.getDataString(SharedPreferencesSetting.CLIENT_CHAT_ID))) {
                        return;
                    }
                    if (data.getString("type").equals("message")) {
                        String message = data.getString("message");
                        Calendar current = GregorianCalendar.getInstance();
                        ChatContent chatMessage = new ChatContent(
                                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.SHOP_CHAT_ID),
                                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID),
                                message,
                                printedFormatterDate.format(current.getTime()),
                                "message",
                                "",
                                null,
                                false,
                                "0");
                        chatContentList.add(chatMessage);
                        chatAdapter.notifyDataSetChanged();
                    } else if (data.getString("type").equals("images")) {
                        String arrayImages = data.getString("images");
                        Gson gson = new Gson();
                        Type typeString = new TypeToken<ArrayList<String>>() {
                        }.getType();
                        ArrayList<String> arrayImagesList = gson.fromJson(arrayImages, typeString);
                        for (String image : arrayImagesList) {
                            Calendar current = GregorianCalendar.getInstance();
                            ChatContent temp = new ChatContent(
                                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.SHOP_CHAT_ID),
                                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID),
                                    "",
                                    printedFormatterDate.format(current.getTime()),
                                    "images",
                                    image,
                                    null,
                                    false,
                                    "0");
                            chatContentList.add(temp);
                            chatAdapter.notifyDataSetChanged();
                        }
                    } else if (data.getString("type").equals("items")) {
                        String itemString = data.getString("items");
                        Gson gson = new Gson();
                        Type typeItem = new TypeToken<Item>() {
                        }.getType();
                        Item item = gson.fromJson(itemString, typeItem);
                        Calendar current = GregorianCalendar.getInstance();
                        ChatContent temp = new ChatContent(
                                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.SHOP_CHAT_ID),
                                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID),
                                "",
                                printedFormatterDate.format(current.getTime()),
                                "items",
                                null,
                                item,
                                false,
                                "0");
                        chatContentList.add(temp);
                        chatAdapter.notifyDataSetChanged();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Logging.logDebug("JSONException: " + e.getMessage());
                }
            });
        }
    };

    private String ConvertingImageToBase64(Bitmap bitmap) {
        Logging.logDebug("ConvertingImageToBase64()");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
        byte[] imageBytes = baos.toByteArray();
        bitmap.recycle();
        Logging.logDebug("imageBytes - length " + imageBytes.length);
        String imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        Logging.logDebug("Base64.encodeToString - imageString.length " + imageString.length());
        return imageString;
    }


    //byte[] byteArray = Base64.decode(imageString, Base64.DEFAULT);
    //Log.d(Logging.debug, "Arrays.toString(byteArray) " + Arrays.toString(byteArray));
    //byte[] decodedString = Base64.decode(imageString, Base64.DEFAULT);
    //Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    // binding.imageView.setImageBitmap(decodedByte);


    private void getChatHistory() {
        RetrofitClientChat.
                getClient(RestApiChat.URL_API_MAIN).
                create(RestApiChat.class).
                getChatHistory(RestAPI.PLATFORM_NUMBER,
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID)).
                enqueue(new Callback<ArrayList<ChatHistoryClass>>() {
                    @Override
                    public void onResponse(@NotNull Call<ArrayList<ChatHistoryClass>> call, @NotNull final Response<ArrayList<ChatHistoryClass>> response) {
                        binding.progress.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                Logging.logDebug("ChatSettingsClass: " + response.body().toString());
                                for (ChatHistoryClass chat : response.body()) {
                                    String value = chat.getCreatedAt();
                                    Calendar current = GregorianCalendar.getInstance();
                                    try {
                                        current.setTime(Objects.requireNonNull(currentFormatterDate.parse(value)));
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                    switch (chat.getType()) {
                                        case "message":
                                            chatContentList.add(new ChatContent(chat.getFromWhom(),
                                                    chat.getToWhom(),
                                                    chat.getMessage(),
                                                    printedFormatterDate.format(current.getTime()),
                                                    "message",
                                                    "",
                                                    null,
                                                    false,
                                                    chat.getOrderID()));
                                            break;
                                        case "images":
                                            for (String image : chat.getImages()) {
                                                chatContentList.add(new ChatContent(chat.getFromWhom(),
                                                        chat.getToWhom(),
                                                        chat.getMessage(),
                                                        printedFormatterDate.format(current.getTime()),
                                                        "images",
                                                        image,
                                                        null,
                                                        false,
                                                        chat.getOrderID()));
                                            }
                                            break;
                                        case "items":
                                            chatContentList.add(new ChatContent(chat.getFromWhom(),
                                                    chat.getToWhom(),
                                                    chat.getMessage(),
                                                    printedFormatterDate.format(current.getTime()),
                                                    "items",
                                                    "",
                                                    chat.getItems(),
                                                    false,
                                                    chat.getOrderID()));
                                            break;
                                        case "order":
                                            chatContentList.add(new ChatContent(chat.getFromWhom(),
                                                    chat.getToWhom(),
                                                    chat.getMessage(),
                                                    printedFormatterDate.format(current.getTime()),
                                                    "order",
                                                    "",
                                                    null,
                                                    false,
                                                    chat.getOrderID()));
                                            break;
                                    }
                                }
                                chatAdapter = new ChatAdapter(ChatActivity.this, chatContentList);
                                binding.chatRecycler.setAdapter(chatAdapter);
                            } else {
                                Logging.logError("Method getChatHistory(): by some reason response is null!");
                            }
                        } else {
                            Logging.logError("Method getChatHistory() response is not successful." +
                                    " Code: " + response.code() + "Message: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<ArrayList<ChatHistoryClass>> call, @NotNull Throwable t) {
                        binding.progress.setVisibility(View.GONE);
                        Logging.logError("Method getChatHistory() failure: " + t.toString());
                    }
                });
    }

    private boolean hasReadExternalStoragePermission() {
        int result = ContextCompat
                .checkSelfPermission(this.getApplicationContext(), READ_WRITE_EXTERNAL_PERMISSIONS[0]);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void tuneChatRecycler() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        binding.chatRecycler.setLayoutManager(linearLayoutManager);
        chatAdapter = new ChatAdapter(this, chatContentList);
        binding.chatRecycler.setAdapter(chatAdapter);
    }

    private void binding() {
        binding.sendMessage.setOnClickListener(v -> {
            String message = binding.messageField.getText().toString().trim();
            if (!message.isEmpty()) {
                Calendar current = GregorianCalendar.getInstance();
                ChatContent temp = new ChatContent(
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.CLIENT_CHAT_ID),
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID),
                        message,
                        printedFormatterDate.format(current.getTime()),
                        "message",
                        "",
                        null,
                        true,
                        "0");
                chatContentList.add(temp);
                chatAdapter.notifyDataSetChanged();
                binding.messageField.setText("");
                socketSendMessage(message);
            }
        });

        binding.back.setOnClickListener(v -> finish());

        binding.rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            binding.rootLayout.getWindowVisibleDisplayFrame(r);
            int heightDiff = binding.rootLayout.getRootView().getHeight() - r.height();
            if (heightDiff > 0.25 * binding.rootLayout.getRootView().getHeight()) {
                Logging.logDebug("keyboard opened");
                if (chatContentList.size() != 0) {
                    binding.chatRecycler.smoothScrollToPosition(chatContentList.size() - 1);
                }
            } else {
                Logging.logDebug("keyboard closed");
            }
        });
        binding.choosePicture.setOnClickListener(v -> requestPermissions());
    }

    private void requestPermissions() {
        if (hasReadExternalStoragePermission()) {
            if (hasCameraPermission()) {
                callPickImageBottomSheet();
            } else {
                ActivityCompat.requestPermissions(
                        this, CAMERA_PERMISSIONS, REQUEST_PERMISSIONS_CAMERA);
            }
        } else {
            ActivityCompat.requestPermissions(this, READ_WRITE_EXTERNAL_PERMISSIONS, REQUEST_PERMISSIONS_READ_WRITE_STORAGE);
        }
    }

    private void callPickImageBottomSheet() {
        //check if AddressesBottomSheet is added otherwise we get exception:
        //java.lang.IllegalStateException: Fragment already added
        if (!pickImageBottomSheet.isAdded()) {
            pickImageBottomSheet = new PickImageBottomSheet();
            pickImageBottomSheet.show(getSupportFragmentManager(), "pickImageBottomSheet");
        }
    }

    private boolean hasCameraPermission() {
        int result = ContextCompat
                .checkSelfPermission(this.getApplicationContext(), CAMERA_PERMISSIONS[0]);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    public static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS_READ_WRITE_STORAGE:
                if (hasReadExternalStoragePermission()) {
                    Logging.logDebug("Method onRequestPermissionsResult() - Request STORAGE Permissions Result: Success!");
                    requestPermissions();
                } else if (shouldShowRequestPermissionRationale(permissions[0])) {
                    showDialogExplanationAboutRequestReadWriteStoragePermission(getText(R.string.chatActivityRequestStoragePermission).toString());
                } else {
                    Logging.logDebug("Method onRequestPermissionsResult() - Request STORAGE Permissions Result: Failed!");
                }
                break;
            case REQUEST_PERMISSIONS_CAMERA:
                if (hasCameraPermission()) {
                    Logging.logDebug("Method onRequestPermissionsResult() - Request Camera Permissions Result: Success!");
                    callPickImageBottomSheet();
                } else if (shouldShowRequestPermissionRationale(permissions[0])) {
                    showDialogExplanationAboutRequestCameraPermission(getText(R.string.chatActivityRequestCameraPermission).toString());
                } else {
                    Logging.logDebug("Method onRequestPermissionsResult() - Request Camera Permissions Result: Failed!");
                }
            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);

        }
    }

    private void showDialogExplanationAboutRequestReadWriteStoragePermission(String message) {
        new AlertDialog.Builder(ChatActivity.this)
                .setMessage(message)
                .setTitle(getText(R.string.mainActivityAttention))
                .setOnCancelListener(dialogInterface -> ActivityCompat.requestPermissions(ChatActivity.this, READ_WRITE_EXTERNAL_PERMISSIONS, REQUEST_PERMISSIONS_READ_WRITE_STORAGE))
                .setPositiveButton(getText(R.string.mainActivityOk), (dialogInterface, i) -> ActivityCompat.requestPermissions(ChatActivity.this, READ_WRITE_EXTERNAL_PERMISSIONS, REQUEST_PERMISSIONS_READ_WRITE_STORAGE))
                .create()
                .show();
    }

    private void showDialogExplanationAboutRequestCameraPermission(String message) {
        new AlertDialog.Builder(ChatActivity.this)
                .setMessage(message)
                .setTitle(getText(R.string.mainActivityAttention))
                .setOnCancelListener(dialogInterface -> ActivityCompat.requestPermissions(ChatActivity.this, CAMERA_PERMISSIONS, REQUEST_PERMISSIONS_CAMERA))
                .setPositiveButton(getText(R.string.mainActivityOk), (dialogInterface, i) -> ActivityCompat.requestPermissions(ChatActivity.this, CAMERA_PERMISSIONS, REQUEST_PERMISSIONS_CAMERA))
                .create()
                .show();
    }

    @Override
    public boolean shouldShowRequestPermissionRationale(@NonNull String permission) {
        return super.shouldShowRequestPermissionRationale(permission);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            if (data != null) {
                if (data.getExtras() == null) {
                    return;
                }
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                Logging.logDebug("Bitmap: " + bitmap.getByteCount());
                new Thread(() -> socketSendPhoto(bitmap)).start();
                File dir = new File(Environment.getExternalStorageDirectory() + "/" + getText(R.string.app_name));
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String fileName = String.format(Locale.getDefault(), "IMG_%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(outFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                    try {
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Logging.logDebug(" " + e.toString());
                }
                Logging.logDebug("outFile.getAbsolutePath()" + outFile.getAbsolutePath());

                Calendar current = GregorianCalendar.getInstance();
                ChatContent temp = new ChatContent(
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.CLIENT_CHAT_ID),
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID),
                        "",
                        printedFormatterDate.format(current.getTime()),
                        "images",
                        outFile.getAbsolutePath(),
                        null,
                        true,
                        "0");
                chatContentList.add(temp);
                chatAdapter.notifyDataSetChanged();
                binding.chatRecycler.smoothScrollToPosition(chatContentList.size() - 1);

                MediaScannerConnection.scanFile(this,
                        new String[]{outFile.toString()}, null,
                        (path, uri) -> {
                            Logging.logDebug("Scanned " + path + ":");
                            Logging.logDebug("-> uri=" + uri);
                        });
            }
            pickImageBottomSheet.dismiss();
        }
    }

    @Override
    public void onSendPictures(HashMap<Integer, String> picturesMap) {
        Logging.logDebug("onSendPictures()");
        for (Map.Entry<Integer, String> picture : picturesMap.entrySet()) {
            Logging.logDebug("picture.getKey(): " + picture.getKey());
            Logging.logDebug("picture.getValue(): " + picture.getValue());
            Calendar current = GregorianCalendar.getInstance();
            chatContentList.add(new ChatContent(
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.CLIENT_CHAT_ID),
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID),
                    "",
                    printedFormatterDate.format(current.getTime()),
                    "images",
                    picture.getValue(),
                    null,
                    true,
                    "0"));
            chatAdapter.notifyDataSetChanged();
            binding.chatRecycler.smoothScrollToPosition(chatContentList.size() - 1);
            new Thread(() -> socketSendPictures(picture.getValue())).start();
        }
    }


    private void socketSendPictures(String image) {
        JSONObject jsonObjectItem = new JSONObject();
        JSONArray picturesArray = new JSONArray();
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.fromFile(new File(image)));
            Logging.logDebug("socketSendPictures: bitmap.getByteCount() " + bitmap.getByteCount());
            String base64 = ConvertingImageToBase64(bitmap);
            JSONObject pictureObject = new JSONObject();
            pictureObject.put("image", base64);
            picturesArray.put(pictureObject);
            jsonObjectItem.put("room_id", SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID));
            jsonObjectItem.put("from_whom", SharedPreferencesSetting.getDataString(SharedPreferencesSetting.CLIENT_CHAT_ID));
            jsonObjectItem.put("to_whom", SharedPreferencesSetting.getDataString(SharedPreferencesSetting.SHOP_CHAT_ID));
            jsonObjectItem.put("message", "");
            jsonObjectItem.put("items", "{}");
            jsonObjectItem.put("type", "images");
            jsonObjectItem.put("platform", RestAPI.PLATFORM_NUMBER);
            jsonObjectItem.put("images", picturesArray.toString());
            socket.emit("room." + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID), jsonObjectItem);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void socketSendPhoto(Bitmap bitmap) {
        JSONObject jsonObjectItem = new JSONObject();
        JSONArray picturesArray = new JSONArray();
        try {
            String base64 = ConvertingImageToBase64(bitmap);
            JSONObject pictureObject = new JSONObject();
            pictureObject.put("image", base64);
            picturesArray.put(pictureObject);
            jsonObjectItem.put("room_id", SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID));
            jsonObjectItem.put("from_whom", SharedPreferencesSetting.getDataString(SharedPreferencesSetting.CLIENT_CHAT_ID));
            jsonObjectItem.put("to_whom", SharedPreferencesSetting.getDataString(SharedPreferencesSetting.SHOP_CHAT_ID));
            jsonObjectItem.put("message", "");
            jsonObjectItem.put("items", "{}");
            jsonObjectItem.put("type", "images");//"type"-"images/message"
            jsonObjectItem.put("platform", RestAPI.PLATFORM_NUMBER);
            jsonObjectItem.put("images", picturesArray.toString());
            socket.emit("room." + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID), jsonObjectItem);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void socketSendMessage(String message) {
        JSONObject jsonObjectItem = new JSONObject();
        try {
            jsonObjectItem.put("room_id", SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID));
            jsonObjectItem.put("from_whom", SharedPreferencesSetting.getDataString(SharedPreferencesSetting.CLIENT_CHAT_ID));
            jsonObjectItem.put("to_whom", SharedPreferencesSetting.getDataString(SharedPreferencesSetting.SHOP_CHAT_ID));
            jsonObjectItem.put("message", message);
            jsonObjectItem.put("type", "message");//"type"-"images/message"
            jsonObjectItem.put("platform", RestAPI.PLATFORM_NUMBER);
            jsonObjectItem.put("images", "[]");
            jsonObjectItem.put("items", "{}");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("room." + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID), jsonObjectItem);
    }

    @Override
    public void onCameraClick() {

//        ContentValues values = new ContentValues();
//        values.put(MediaStore.Images.Media.TITLE, "Picture");
//        values.put(MediaStore.Images.Media.DESCRIPTION, "Camera");
//        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        File file = new File(Environment.getExternalStorageDirectory(), "/" + getText(R.string.app_name) + "/photo_" + timeStamp + ".png");
//        imageUri = Uri.fromFile(file);


        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(takePhotoIntent, REQUEST_TAKE_PHOTO);
    }

    @Override
    protected void onDestroy() {
        Constants.customerInChat = false;
        socket.off("room." + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.ROOM_CHAT_ID));
        socket.disconnect();
        super.onDestroy();
    }
}