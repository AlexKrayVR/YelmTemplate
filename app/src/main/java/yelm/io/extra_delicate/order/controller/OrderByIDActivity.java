package yelm.io.extra_delicate.order.controller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;

import com.google.android.material.appbar.AppBarLayout;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.constants.Constants;
import yelm.io.extra_delicate.databinding.ActivityOrderByIDBinding;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.order.model.OrderItemPOJO;
import yelm.io.extra_delicate.order.model.UserOrderPOJO;
import yelm.io.extra_delicate.order.adapter.OrderProductAdapter;
import yelm.io.extra_delicate.rest.rest_api.RestAPI;
import yelm.io.extra_delicate.rest.client.RetrofitClient;
import yelm.io.extra_delicate.support_stuff.Logging;

public class OrderByIDActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener {
    ActivityOrderByIDBinding binding;
    OrderProductAdapter orderProductAdapter;
    private static final float DEFAULT_ZOOM = 16f;

    private int maxScrollSize;
    private static final int PERCENTAGE_TO_SHOW_IMAGE = 80;
    private boolean isImageHidden;

    SimpleDateFormat currentFormatterDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat printedFormatterDate = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MapKitFactory.setApiKey(getString(R.string.yandex_maps_API_key));
        MapKitFactory.initialize(this);
        super.onCreate(savedInstanceState);
        binding = ActivityOrderByIDBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.back.setOnClickListener(v -> finish());
        binding.mapView.getMap().setScrollGesturesEnabled(false);
        binding.mapView.getMap().setZoomGesturesEnabled(false);
        binding.mapView.getMap().setFastTapEnabled(false);

        Bundle args = getIntent().getExtras();
        if (args != null) {
            Logging.logDebug( "Method onCreate() - id: " + args.getString("id"));
            getOrder(args.getString("id"));
        }
    }


    private void getOrder(String id) {
        RetrofitClient.
                getClient(RestAPI.URL_API_MAIN).
                create(RestAPI.class).
                getOrderByID(RestAPI.PLATFORM_NUMBER,
                        Constants.VERSION,
                        getResources().getConfiguration().locale.getLanguage(),
                        getResources().getConfiguration().locale.getCountry(),
                        id).
                enqueue(new Callback<UserOrderPOJO>() {
                    @Override
                    public void onResponse(@NotNull Call<UserOrderPOJO> call, @NotNull final Response<UserOrderPOJO> response) {
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                binding.mapView.getMap().move(
                                        new CameraPosition(new Point(Double.parseDouble(response.body().getLatitude()),
                                                Double.parseDouble(response.body().getLongitude())),
                                                DEFAULT_ZOOM,
                                                0.0f,
                                                0.0f),
                                        new Animation(Animation.Type.LINEAR, 0),
                                        null);
                                Calendar date = GregorianCalendar.getInstance();
                                try {
                                    date.setTime(Objects.requireNonNull(currentFormatterDate.parse(response.body().getCreatedAt())));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                                binding.description.setText(String.format("№ %s\n%s %s %s\n%s %s %s\n%s %s\n%s %s",
                                        response.body().getId(),
                                        getText(R.string.orderByIDActivityOrderPrice),
                                        response.body().getEndTotal(),
                                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN),
                                        getText(R.string.orderByIDActivityDelivery),
                                        response.body().getDeliveryPrice(),
                                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN),
                                        getText(R.string.orderByIDActivityOrderData),
                                        printedFormatterDate.format(date.getTime()),
                                        getText(R.string.orderByIDActivityOrderAddress),
                                        response.body().getAddress()));

                                binding.collapsingToolbar.setTitle(response.body().getStatus());

//                                if (response.body().getTransactionStatus().equals("Подтвержденный")) {
//                                    binding.collapsingToolbar.setCollapsedTitleTextColor(getResources().getColor(R.color.greenColor));
//                                    binding.collapsingToolbar.setExpandedTitleColor(getResources().getColor(R.color.greenColor));
//                                } else if (response.body().getTransactionStatus().equals("Неподтвержденный")) {
//                                    binding.collapsingToolbar.setCollapsedTitleTextColor(getResources().getColor(R.color.colorOrangeText));
//                                    binding.collapsingToolbar.setExpandedTitleColor(getResources().getColor(R.color.colorOrangeText));
//                                } else if (response.body().getTransactionStatus().equals("Отмененный")) {
//                                    binding.collapsingToolbar.setCollapsedTitleTextColor(getResources().getColor(R.color.redColor));
//                                    binding.collapsingToolbar.setExpandedTitleColor(getResources().getColor(R.color.redColor));
//                                }

                                //need for correct getting counts of items cuz server BD gives weird result >_<
                                HashMap<String, String> mapItems = new HashMap<>();
                                for (OrderItemPOJO itemPOJO : response.body().getItems()) {
                                    mapItems.put(itemPOJO.getId(), itemPOJO.getCount());
                                }
                                orderProductAdapter = new OrderProductAdapter(OrderByIDActivity.this,
                                        response.body().getItemsInfo(),
                                        mapItems
                                );
                                binding.recyclerOrderItems.setLayoutManager(new LinearLayoutManager(OrderByIDActivity.this, LinearLayoutManager.VERTICAL, false));
                                binding.recyclerOrderItems.setAdapter(orderProductAdapter);
                            } else {
                                Logging.logError("Method getOrder(): by some reason response is null!");
                            }
                        } else {
                            Logging.logError( "Method getOrder() response is not successful." +
                                    " Code: " + response.code() + "Message: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<UserOrderPOJO> call, @NotNull Throwable t) {
                        Logging.logError("Method getOrder() failure: " + t.toString());
                    }
                });
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (maxScrollSize == 0)
            maxScrollSize = appBarLayout.getTotalScrollRange();

        int currentScrollPercentage = (Math.abs(i)) * 100
                / maxScrollSize;

        if (currentScrollPercentage >= PERCENTAGE_TO_SHOW_IMAGE) {
            if (!isImageHidden) {
                isImageHidden = true;

                //ViewCompat.animate(mFab).scaleY((float) 0.9).scaleX((float) 0.9).start();
                //ViewCompat.animate(imageButton).scaleY((float) 0.2).scaleX((float) 0.2).setDuration(200).start();
            }
        }

        if (currentScrollPercentage < PERCENTAGE_TO_SHOW_IMAGE) {
            if (isImageHidden) {
                isImageHidden = false;

                //ViewCompat.animate(mFab).scaleY(1).scaleX(1).start();
                //ViewCompat.animate(imageButton).scaleY(1).scaleX(1).start();
            }
        }
    }

    @Override
    protected void onStop() {
        binding.mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        binding.mapView.onStart();
    }
}