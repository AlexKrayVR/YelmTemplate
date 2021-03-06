package yelm.io.extra_delicate.basket.controller;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.basket.adapter.BasketAdapter;
import yelm.io.extra_delicate.basket.model.BasketCheckPOJO;
import yelm.io.extra_delicate.basket.model.DeletedId;
import yelm.io.extra_delicate.constants.Constants;
import yelm.io.extra_delicate.database.Common;
import yelm.io.extra_delicate.database.basket_new.BasketCart;
import yelm.io.extra_delicate.database.user_addresses.UserAddress;
import yelm.io.extra_delicate.databinding.ActivityBasketOnlyDeliveryBinding;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.main.model.Modifier;
import yelm.io.extra_delicate.order.controller.OrderActivity;
import yelm.io.extra_delicate.rest.rest_api.RestAPI;
import yelm.io.extra_delicate.rest.client.RetrofitClient;
import yelm.io.extra_delicate.support_stuff.Logging;
import yelm.io.extra_delicate.user_account.model.UserAuth;

public class BasketActivity extends AppCompatActivity {

    ActivityBasketOnlyDeliveryBinding binding;
    BasketAdapter basketAdapter;
    private final CompositeDisposable compositeDisposableBasket = new CompositeDisposable();
    UserAddress currentAddress;
    private BigDecimal deliveryCostStart = new BigDecimal("0");
    private BigDecimal deliveryCostFinal = new BigDecimal("0");
    private BigDecimal finalCost = new BigDecimal("0");
    private String deliveryTime = "0";
    private static final int PAYMENT_SUCCESS = 777;

    private int countCutlery = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBasketOnlyDeliveryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding();
        setCustomColor();
        currentAddress = getDeliveryAddress();
        //check if there is user address in db and delivery available to it
        //if there is not we just show basket and disable ordering button
        if (currentAddress != null && !Constants.ShopID.equals("0")) {
            checkBasket(currentAddress.latitude, currentAddress.longitude);
        } else {
            Logging.logDebug("Method onCreate() - currentAddress is null or ShopID equals 0");
            binding.ordering.getBackground().setTint(getResources().getColor(R.color.colorButtonOrderingDisable));
            binding.layoutFinalCost.setVisibility(View.GONE);
            binding.layoutDeliveryNotAvailable.setVisibility(View.VISIBLE);
            binding.layoutDelivery.setVisibility(View.GONE);
            binding.time.setText(String.format("%s %s", "0", getText(R.string.delivery_time)));
            setCompositeDisposableBasket();
            basketAdapter = new BasketAdapter(this, Common.basketCartRepository.getBasketCartsList());
            binding.recyclerCart.setAdapter(basketAdapter);
        }
    }

    private void setCustomColor() {
        binding.back.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.back.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.ordering.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.ordering.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.progressBar.getIndeterminateDrawable()
                .setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)), PorterDuff.Mode.SRC_IN);
    }


    private void checkBasket(String lat, String lon) {
        //stop upgrade basket UI
        //compositeDisposableBasket.clear();
        binding.progressBar.setVisibility(View.VISIBLE);
        JSONArray jsonObjectItems = new JSONArray();
        List<BasketCart> basketCarts = Common.basketCartRepository.getBasketCartsList();
        for (BasketCart basketCart : basketCarts) {
            try {
                JSONObject jsonObjectItem = new JSONObject();
                jsonObjectItem
                        .put("id", basketCart.itemID)
                        .put("count", basketCart.count);
                jsonObjectItems.put(jsonObjectItem);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Logging.logDebug("Method checkBasket() - jsonObjectItems: " + jsonObjectItems.toString());

        RetrofitClient.
                getClient(RestAPI.URL_API_MAIN).
                create(RestAPI.class).
                checkBasket(
                        Constants.VERSION,
                        RestAPI.PLATFORM_NUMBER,
                        Constants.ShopID,
                        getResources().getConfiguration().locale.getLanguage(),
                        getResources().getConfiguration().locale.getCountry(),
                        lat,
                        lon,
                        jsonObjectItems.toString()
                ).
                enqueue(new Callback<BasketCheckPOJO>() {
                    @Override
                    public void onResponse(@NotNull Call<BasketCheckPOJO> call, @NotNull final Response<BasketCheckPOJO> response) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                if (response.body().getType().equals("close")) {
                                    binding.layoutFinalCost.setVisibility(View.GONE);
                                    binding.workingTime.setVisibility(View.VISIBLE);
                                    binding.workingTime.setText(String.format("%s %s",
                                            getString(R.string.basketActivityWorkingTime),
                                            response.body().getTimeWork()));
                                } else {
                                    binding.layoutDelivery.setVisibility(View.VISIBLE);
                                    Logging.logDebug("Method checkBasket() - BasketCheckPOJO: " + response.body().toString());
                                    deliveryTime = response.body().getDelivery().getTime();
                                    binding.time.setText(String.format("%s %s", deliveryTime, getText(R.string.delivery_time)));
                                    deliveryCostStart = new BigDecimal(response.body().getDelivery().getPrice());
                                    new Thread(() -> updateBasketCartsQuantity(response.body().getDeletedId())).start();
                                }

                            } else {
                                Logging.logError("Method checkBasket() - by some reason response is null!");
                                showToast(getString(R.string.errorConnectedToServer));
                            }
                        } else {
                            Logging.logError("Method checkBasket() - response is not successful." +
                                    "Code: " + response.code() + "Message: " + response.message());
                            showToast(getString(R.string.errorConnectedToServer));
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<BasketCheckPOJO> call, @NotNull Throwable t) {
                        Logging.logError("Method checkBasket() - failure: " + t.toString());
                        showToast(getString(R.string.errorConnectedToServer));
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void updateBasketCartsQuantity(List<DeletedId> deletedIDList) {
        for (DeletedId deletedId : deletedIDList) {
            Logging.logDebug("deletedId: " + deletedId.toString());
            BasketCart basketCart = Common.basketCartRepository.getBasketCartById(deletedId.getId());
            if (basketCart != null) {
                basketCart.quantity = deletedId.getAvailableCount();
                Common.basketCartRepository.updateBasketCart(basketCart);
            }
        }
        setCompositeDisposableBasket();
    }

    private void setCompositeDisposableBasket() {
        compositeDisposableBasket.
                add(Common.basketCartRepository.
                        getBasketCarts().
                        observeOn(AndroidSchedulers.mainThread()).
                        subscribeOn(Schedulers.io()).
                        subscribe(this::updateBasket));
    }

    private void updateBasket(List<BasketCart> carts) {
        binding.emptyBasket.setVisibility(carts.size() == 0 ? View.VISIBLE : View.GONE);
        binding.layoutDeliveryInfo.setVisibility(carts.size() == 0 ? View.GONE : View.VISIBLE);
        binding.layoutFinalCost.setVisibility(carts.size() == 0 ? View.GONE : View.VISIBLE);

        basketAdapter = new BasketAdapter(this, carts);
        binding.recyclerCart.setAdapter(basketAdapter);

        finalCost = new BigDecimal("0");
        boolean allowOrdering = true;
        for (BasketCart cart : carts) {
            BigDecimal costCurrentCart = new BigDecimal(cart.finalPrice);
            for (Modifier modifier : cart.modifier) {
                costCurrentCart = costCurrentCart.add(new BigDecimal(modifier.getValue()));
            }
            costCurrentCart = costCurrentCart.multiply(new BigDecimal(cart.count));
            finalCost = finalCost.add(costCurrentCart);
            //check if quantity less than its count
            if (new BigDecimal(cart.count).compareTo(new BigDecimal(cart.quantity)) > 0) {
                allowOrdering = false;
            }
        }
        Logging.logDebug("Method updateBasket() - finalCost: " + finalCost.toString());

        if (finalCost.compareTo(new BigDecimal(SharedPreferencesSetting.getDataString(SharedPreferencesSetting.MIN_ORDER_PRICE))) < 0) {
            binding.layoutMinOrderPrice.setVisibility(View.VISIBLE);
            allowOrdering = false;
            binding.orderMinPrice.setText(String.format("%s %s %s",
                    getString(R.string.basketActivityOrderMinPrice),
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.MIN_ORDER_PRICE),
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        } else {
            binding.layoutMinOrderPrice.setVisibility(View.GONE);
        }

        if (carts.size() != 0 && allowOrdering && currentAddress != null && !Constants.ShopID.equals("0")) {
            binding.ordering.setEnabled(true);
            binding.ordering.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        } else {
            binding.ordering.setEnabled(false);
            binding.ordering.getBackground().setTint(getResources().getColor(R.color.colorButtonOrderingDisable));
        }

        //show a note about possible free shipping
        //if finalCost of basket more than minimum value for free delivery
        if (finalCost.compareTo(new BigDecimal(SharedPreferencesSetting.getDataString(SharedPreferencesSetting.MIN_PRICE_FOR_FREE_DELIVERY))) < 0) {
            deliveryCostFinal = deliveryCostStart;
            BigDecimal freeDelivery = new BigDecimal(SharedPreferencesSetting.getDataString(SharedPreferencesSetting.MIN_PRICE_FOR_FREE_DELIVERY));
            freeDelivery = freeDelivery.subtract(finalCost);
            binding.freeDelivery.setText(String.format("%s %s %s %s",
                    getString(R.string.basketActivityFreeDelivery1),
                    freeDelivery,
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN),
                    getString(R.string.basketActivityFreeDelivery2)));
            binding.freeDelivery.setVisibility(View.VISIBLE);
        } else {
            deliveryCostFinal = new BigDecimal("0");
            binding.freeDelivery.setVisibility(View.GONE);
        }

        Logging.logDebug("Method updateBasket() - deliveryCostFinal: " + deliveryCostFinal.toString());

        binding.total.setText(String.format("%s %s",
                finalCost,
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        binding.deliveryCost.setText(String.format("%s %s",
                deliveryCostFinal,
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        binding.finalPrice.setText(String.format("%s %s",
                finalCost.add(deliveryCostFinal),
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
    }

    private void cutleryLogic() {
        setCutleryButtonEnable();
        binding.removeCutlery.setOnClickListener(v -> {
            countCutlery -= 1;
            //cutleryViewModel.getCashBack().postValue(new BigDecimal(String.valueOf(countCutlery)));
            binding.cutleryCount.setText(String.valueOf(countCutlery));
            setCutleryButtonEnable();
        });
        binding.addCutlery.setOnClickListener(v -> {
            countCutlery += 1;
            binding.cutleryCount.setText(String.valueOf(countCutlery));
            setCutleryButtonEnable();
        });
    }

    private void setCutleryButtonEnable() {
        binding.addCutlery.setEnabled(countCutlery != 5);
        binding.removeCutlery.setEnabled(countCutlery != 1);
    }

    private void binding() {
        cutleryLogic();
        binding.recyclerCart.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.back.setOnClickListener(v -> finish());
        binding.cleanBasket.setOnClickListener(v -> Common.basketCartRepository.emptyBasketCart());
        binding.ordering.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderActivity.class);
            intent.putExtra("finalPrice", finalCost.toString());
            intent.putExtra("deliveryCost", deliveryCostFinal.toString());
            intent.putExtra("deliveryTime", deliveryTime);
            intent.putExtra("countCutlery", String.valueOf(countCutlery));
            intent.putExtra(UserAddress.class.getSimpleName(), currentAddress);
            startActivityForResult(intent, PAYMENT_SUCCESS);
        });
    }

    public UserAddress getDeliveryAddress() {
        if (Common.userAddressesRepository.getUserAddressesList() == null) {
            return null;
        }
        for (UserAddress current : Common.userAddressesRepository.getUserAddressesList()) {
            if (current.isChecked) {
                return current;
            }
        }
        return null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        if (requestCode == PAYMENT_SUCCESS) {
            Common.basketCartRepository.emptyBasketCart();
            getUserBalance();

            Logging.logDebug("PAYMENT_SUCCESS " + data.getStringExtra("success"));
            if (Objects.equals(data.getStringExtra("success"), "card")) {
                binding.paymentResultText.setText(getText(R.string.order_is_accepted_by_card));
            } else if (Objects.equals(data.getStringExtra("success"), "placeorder")) {
                binding.paymentResultText.setText(getText(R.string.order_is_accepted));
            } else {
                binding.paymentResultText.setText(getText(R.string.order_is_accepted_by_google_pay));
            }
            binding.paymentResult.setVisibility(View.VISIBLE);
            binding.lotti.playAnimation();
        }
        new Handler(Looper.getMainLooper()) {{
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    binding.paymentResult.setVisibility(View.GONE);
                }
            }, 3000);
        }};
    }

    private void showToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void getUserBalance() {
        RetrofitClient.
                getClient(RestAPI.URL_API_MAIN).
                create(RestAPI.class).
                getUserData(
                        RestAPI.PLATFORM_NUMBER,
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_LOGIN)
                ).
                enqueue(new Callback<UserAuth>() {
                    @Override
                    public void onResponse(@NotNull Call<UserAuth> call, @NotNull final Response<UserAuth> response) {
                        if (!response.isSuccessful()) {
                            Logging.logError("Method getUserBalance() - response is not successful." +
                                    "Code: " + response.code() + "Message: " + response.message());

                        } else {
                            if (response.body() != null) {
                                SharedPreferencesSetting.setData(SharedPreferencesSetting.USER_BALANCE,
                                        response.body().getUser().getInfo().getBalance());

                                SharedPreferencesSetting.setData(SharedPreferencesSetting.USER_NAME,
                                        response.body().getUser().getInfo().getName());

                                SharedPreferencesSetting.setData(SharedPreferencesSetting.USER_NOTIFICATION,
                                        response.body().getUser().getNotification().equals(true) ? "1" : "0");
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<UserAuth> call, @NotNull Throwable t) {
                        Logging.logError("Method getUserBalance() - failure: " + t.toString());
                    }
                });
    }

    @Override
    protected void onDestroy() {
        compositeDisposableBasket.clear();
        super.onDestroy();
    }
}