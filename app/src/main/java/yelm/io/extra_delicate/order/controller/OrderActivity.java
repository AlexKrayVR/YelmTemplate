package yelm.io.extra_delicate.order.controller;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodToken;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.TransactionInfo;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.cloudpayments.sdk.three_ds.ThreeDSDialogListener;
import ru.cloudpayments.sdk.three_ds.ThreeDsDialogFragment;
import yelm.io.extra_delicate.constants.Constants;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.notification.CustomToast;
import yelm.io.extra_delicate.order.model.PriceConverterResponse;
import yelm.io.extra_delicate.order.model.PromoCodeClass;
import yelm.io.extra_delicate.order.text_watcher.CustomTextWatcher;
import yelm.io.extra_delicate.payment.PayApi;
import yelm.io.extra_delicate.payment.PaymentActivity;
import yelm.io.extra_delicate.payment.models.Transaction;
import yelm.io.extra_delicate.payment.response.PayApiError;
import yelm.io.extra_delicate.rest.rest_api.RestAPI;
import yelm.io.extra_delicate.rest.client.RetrofitClient;
import yelm.io.extra_delicate.support_stuff.Logging;
import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.database.basket_new.BasketCart;
import yelm.io.extra_delicate.database.Common;
import yelm.io.extra_delicate.database.user_addresses.UserAddress;
import yelm.io.extra_delicate.databinding.ActivityOrderNewBinding;
import yelm.io.extra_delicate.payment.googleplay.PaymentsUtil;
import yelm.io.extra_delicate.support_stuff.PhoneTextFormatter;
import yelm.io.extra_delicate.user_account.model.UserAuth;

public class OrderActivity extends AppCompatActivity implements ThreeDSDialogListener {
    ActivityOrderNewBinding binding;

    private static final int PAYMENT_SUCCESS = 77;

    private PaymentsClient paymentsClient;

    private BigDecimal startCost = new BigDecimal("0");
    private BigDecimal finalCost = new BigDecimal("0");//without delivery cost
    private BigDecimal paymentCost = new BigDecimal("0");
    private BigDecimal paymentCostAfterBonus = new BigDecimal("0");

    private BigDecimal userBonus = new BigDecimal("0");

    private BigDecimal deliveryCostStart = new BigDecimal("0");
    private BigDecimal deliveryCostFinal = new BigDecimal("0");
    private BigDecimal discountPromo = new BigDecimal("0");

    private String deliveryTime = "";
    private String discountType = "0";
    UserAddress currentAddress;

    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 991;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String transactionID = "-1";
    private String order = "";
    private final String userID = SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_LOGIN);
    private final String currency = SharedPreferencesSetting.getDataString(SharedPreferencesSetting.CURRENCY);
    private String countCutlery = "1";
    private static final String ENTRANCE = "ENTRANCE";
    private static final String FLOOR = "FLOOR";
    private static final String FLAT = "FLAT";
    private static final String PHONE = "PHONE";
    private static SharedPreferences userSettings;
    private static final String USER_PREFERENCES = "user_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderNewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        userSettings = getSharedPreferences(USER_PREFERENCES, Context.MODE_PRIVATE);
        setCustomColor();
        Bundle args = getIntent().getExtras();
        if (args != null) {
            finalCost = new BigDecimal(args.getString("finalPrice"));
            startCost = finalCost;
            deliveryCostStart = new BigDecimal(args.getString("deliveryCost"));
            deliveryCostFinal = deliveryCostStart;
            paymentCost = finalCost.add(deliveryCostStart);
            paymentCostAfterBonus = paymentCost;
            deliveryTime = args.getString("deliveryTime");
            currentAddress = (UserAddress) args.getSerializable(UserAddress.class.getSimpleName());
            countCutlery = args.getString("countCutlery");

            Logging.logDebug("countCutlery: " + countCutlery);
            Logging.logDebug("startCost: " + startCost);
            Logging.logDebug("finalCost: " + finalCost);
            Logging.logDebug("paymentCost: " + paymentCost);
            Logging.logDebug("deliveryCost: " + discountPromo);
            Logging.logDebug("deliveryPrice: " + deliveryCostStart);
            Logging.logDebug("deliveryTime: " + deliveryTime);
            Logging.logDebug("currentAddress: " + currentAddress.toString());
        }
        binding();
        checkEditText();
        paymentsClient = PaymentsUtil.createPaymentsClient(this);
        checkIsReadyToPay();
        bindingChosePaymentType();
        binding.applyPromocode.setOnClickListener(v -> getPromoCode());
        getPromoIfExist();
        initSeekBar();
    }

    private void setCustomColor() {
        binding.back.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.applyPromocode.getBackground()
                .setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.paymentCard.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.paymentCash.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.progress.getIndeterminateDrawable()
                .setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)), PorterDuff.Mode.SRC_IN);
        binding.back.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.applyPromocode.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.paymentCash.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.paymentCard.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
    }

    private void checkEditText() {
        binding.entrance.addTextChangedListener(new CustomTextWatcher(binding.entrance, this));
        binding.floor.addTextChangedListener(new CustomTextWatcher(binding.floor, this));
        binding.flat.addTextChangedListener(new CustomTextWatcher(binding.flat, this));
        binding.phone.addTextChangedListener(new CustomTextWatcher(binding.phone, this));
    }

    private void getPromoIfExist() {
        String type = SharedPreferencesSetting.getDataString(SharedPreferencesSetting.DISCOUNT_TYPE);
        String amount = SharedPreferencesSetting.getDataString(SharedPreferencesSetting.DISCOUNT_AMOUNT);
        String name = SharedPreferencesSetting.getDataString(SharedPreferencesSetting.DISCOUNT_NAME);
        Logging.logDebug("type: " + type);
        Logging.logDebug("amount: " + amount);
        Logging.logDebug("name: " + name);
        if (type != null) {
            if (!type.isEmpty()) {
                setPromoCode(type, amount, name);
                binding.promoCode.setText(name);
            }
        }
    }

    private void getPromoCode() {
        if (binding.promoCode.getText().toString().trim().isEmpty()) {
            showToast(getString(R.string.orderActivityEnterPromoCode));
        } else {
            RetrofitClient.
                    getClient(RestAPI.URL_API_MAIN)
                    .create(RestAPI.class)
                    .getPromoCode(binding.promoCode.getText().toString().trim(),
                            RestAPI.PLATFORM_NUMBER,
                            SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_LOGIN)
                    ).enqueue(new Callback<PromoCodeClass>() {
                @Override
                public void onResponse(@NotNull Call<PromoCodeClass> call, @NotNull Response<PromoCodeClass> response) {
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            if (response.body().getStatus().equals("200")) {
                                setPromoCode(response.body().getPromocode().getType(),
                                        response.body().getPromocode().getAmount(),
                                        response.body().getPromocode().getName());
                                initSeekBar();
                            }
                            showToast(response.body().getMessage());
                        } else {
                            Logging.logError("Method getPromoCode() - by some reason response is null!");
                        }

                    } else {
                        Logging.logError("Method getPromoCode() - response is not successful. " +
                                "Code: " + response.code() + "Message: " + response.message());
                    }
                }

                @Override
                public void onFailure(@NotNull Call<PromoCodeClass> call, @NotNull Throwable t) {
                    Logging.logError("Method getPromoCode() - failure: " + t.toString());
                }
            });
        }
    }

    private void setPromoCode(String type, String amount, String name) {
        binding.layoutDiscount.setVisibility(View.VISIBLE);
        finalCost = startCost;
        deliveryCostFinal = deliveryCostStart;
        discountPromo = new BigDecimal(amount);
        discountType = type;
        SharedPreferencesSetting.setData(SharedPreferencesSetting.DISCOUNT_TYPE, type);
        SharedPreferencesSetting.setData(SharedPreferencesSetting.DISCOUNT_AMOUNT, amount);
        SharedPreferencesSetting.setData(SharedPreferencesSetting.DISCOUNT_NAME, name);
        switch (type) {
            case "full":
                binding.discountPercent.setText(String.format("%s",
                        getText(R.string.orderDiscount)));
                binding.discountPrice.setText(String.format("%s %s", discountPromo,
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
                if (discountPromo.compareTo(finalCost) >= 0) {
                    finalCost = new BigDecimal("1");
                } else {
                    finalCost = finalCost.subtract(discountPromo);
                }
                break;
            case "delivery":
                if (deliveryCostFinal.compareTo(new BigDecimal("0")) == 0) {
                    binding.discountPercent.setText(String.format("%s - %s", getText(R.string.orderDiscountDelivery), getText(R.string.orderDiscountDeliveryAlreadyFree)));
                    break;
                }
                binding.discountPercent.setText(String.format("%s %s%%", getText(R.string.orderDiscountDelivery), discountPromo));
                BigDecimal discountDelivery = discountPromo.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
                discountDelivery = discountDelivery.multiply(deliveryCostFinal).setScale(2, BigDecimal.ROUND_HALF_UP);
                binding.discountPrice.setText(String.format("%s %s", discountDelivery,
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
                deliveryCostFinal = deliveryCostFinal.subtract(discountDelivery);
                break;
            case "percent":
                binding.discountPercent.setText(String.format("%s %s%%", getText(R.string.orderDiscount), discountPromo));
                BigDecimal discount = discountPromo.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
                discount = discount.multiply(finalCost).setScale(2, BigDecimal.ROUND_HALF_UP);
                binding.discountPrice.setText(String.format("%s %s", discount,
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
                finalCost = finalCost.subtract(discount);
                if (finalCost.compareTo(new BigDecimal("0")) == 0) {
                    finalCost = new BigDecimal("1");
                }
                break;
        }
        paymentCost = finalCost.add(deliveryCostFinal);
        paymentCostAfterBonus = paymentCost;
        binding.finalPrice.setText(String.format("%s %s", paymentCostAfterBonus,
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        Logging.logDebug("finalCost: " + finalCost);
        Logging.logDebug("paymentCost: " + paymentCost);
    }

    private void sendOrder(String payment) {
        List<BasketCart> basketCarts = Common.basketCartRepository.getBasketCartsList();
        JSONArray jsonObjectItems = new JSONArray();
        try {
            for (int i = 0; i < basketCarts.size(); i++) {
                JSONObject jsonObjectItem = new JSONObject();
                jsonObjectItem
                        .put("id", basketCarts.get(i).itemID)
                        .put("count", basketCarts.get(i).count);
                jsonObjectItems.put(jsonObjectItem);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Logging.logDebug("jsonObjectItems: " + jsonObjectItems.toString());
        RetrofitClient.
                getClient(RestAPI.URL_API_MAIN)
                .create(RestAPI.class)
                .sendOrder(Constants.VERSION,
                        getResources().getConfiguration().locale.getCountry(),
                        getResources().getConfiguration().locale.getLanguage(),
                        RestAPI.PLATFORM_NUMBER,
                        currentAddress.latitude,
                        currentAddress.longitude,
                        "test",
                        startCost.toString(),
                        discountPromo.toString(),
                        transactionID,
                        userID,
                        currentAddress.address,
                        payment,
                        binding.floor.getText().toString(),
                        binding.entrance.getText().toString(),
                        paymentCostAfterBonus.toString(),
                        binding.phone.getText().toString(),
                        binding.flat.getText().toString(),
                        "delivery",
                        jsonObjectItems.toString(),
                        deliveryCostFinal.toString(),
                        currency,
                        Constants.ShopID,
                        discountType,
                        countCutlery,
                        userBonus.toString()
                ).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Logging.logDebug("Method sendOrder() - response.code(): " + response.code());
                    Common.basketCartRepository.emptyBasketCart();
                    Intent intentGP = new Intent();
                    intentGP.putExtra("success", payment);
                    setResult(RESULT_OK, intentGP);
                    finish();
                } else {
                    Logging.logError("Method sendOrder() - response is not successful. " +
                            "Code: " + response.code() + "Message: " + response.message());
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                Logging.logError("Method sendOrder() - failure: " + t.toString());
            }
        });
    }

    private boolean preparePayment() {
        String phone = binding.phone.getText().toString();
        Logging.logDebug("phone: " + phone);
        phone = phone.replaceAll("\\D", "");
        Logging.logDebug("phone after replacement: " + phone);
        if (phone.trim().equals("") || phone.length() != 11) {
            CustomToast.showStatus(this, getString(R.string.orderActivityEnterCorrectPhone));
            return false;
        }

        String floor = binding.floor.getText().toString();
        if (floor.trim().equals("")) {
            CustomToast.showStatus(this, getString(R.string.orderActivityEnterFloor));
            return false;
        }

        String entrance = binding.entrance.getText().toString();
        if (entrance.trim().equals("")) {
            CustomToast.showStatus(this, getString(R.string.orderActivityEnterEntrance));
            return false;
        }

        String flat = binding.flat.getText().toString();
        if (flat.trim().equals("")) {
            CustomToast.showStatus(this, getString(R.string.orderActivityEnterFlat));
            return false;
        }
        return true;
    }

    private void bindingChosePaymentType() {
        if (!SharedPreferencesSetting.getDataBoolean(SharedPreferencesSetting.PAYMENT_CARD) &&
                !SharedPreferencesSetting.getDataBoolean(SharedPreferencesSetting.PAYMENT_CASH)) {
            binding.paymentUnavailable.setVisibility(View.VISIBLE);
            binding.layoutPaymentType.setVisibility(View.GONE);
            return;
        }
        if (SharedPreferencesSetting.getDataBoolean(SharedPreferencesSetting.PAYMENT_CARD)) {
            binding.cardPay.setCardBackgroundColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
            binding.cardPayText.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
            binding.paymentCard.setVisibility(View.VISIBLE);
            binding.cardPay.setOnClickListener(view -> {
                binding.cardPay.setCardBackgroundColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
                binding.cardPayText.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
                binding.googlepayPay.setCardBackgroundColor(Color.TRANSPARENT);
                binding.googlePayText.setTextColor(getResources().getColor(R.color.colorText));

                binding.cashPay.setCardBackgroundColor(Color.TRANSPARENT);
                binding.cashPayText.setTextColor(getResources().getColor(R.color.colorText));

                binding.googlePay.setVisibility(View.GONE);
                binding.paymentCash.setVisibility(View.GONE);
                binding.paymentCard.setVisibility(View.VISIBLE);
            });
        } else {
            binding.cashPay.setCardBackgroundColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
            binding.cardPayText.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
            binding.cardPay.setVisibility(View.GONE);
            binding.googlepayPay.setVisibility(View.GONE);

            binding.paymentCash.setVisibility(View.VISIBLE);
            binding.paymentCard.setVisibility(View.GONE);
        }

        binding.googlepayPay.setOnClickListener(view -> {
            binding.googlepayPay.setCardBackgroundColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
            binding.googlePayText.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
            binding.cardPay.setCardBackgroundColor(Color.TRANSPARENT);
            binding.cardPayText.setTextColor(getResources().getColor(R.color.colorText));

            binding.cashPay.setCardBackgroundColor(Color.TRANSPARENT);
            binding.cashPayText.setTextColor(getResources().getColor(R.color.colorText));

            binding.paymentCard.setVisibility(View.GONE);
            binding.paymentCash.setVisibility(View.GONE);
            binding.googlePay.setVisibility(View.VISIBLE);
        });

        if (SharedPreferencesSetting.getDataBoolean(SharedPreferencesSetting.PAYMENT_CASH)) {
            binding.cashPay.setOnClickListener(view -> {
                binding.cashPay.setCardBackgroundColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
                binding.cashPayText.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
                binding.cardPay.setCardBackgroundColor(Color.TRANSPARENT);
                binding.cardPayText.setTextColor(getResources().getColor(R.color.colorText));

                binding.googlepayPay.setCardBackgroundColor(Color.TRANSPARENT);
                binding.googlePayText.setTextColor(getResources().getColor(R.color.colorText));

                binding.paymentCard.setVisibility(View.GONE);
                binding.googlePay.setVisibility(View.GONE);
                binding.paymentCash.setVisibility(View.VISIBLE);
            });
        } else {
            binding.cashPay.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        switch (requestCode) {
            case PAYMENT_SUCCESS:
                Intent intent = new Intent();
                intent.putExtra("success", "card");
                setResult(RESULT_OK, intent);
                finish();
                break;
            case LOAD_PAYMENT_DATA_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Logging.logDebug("Method payment with GooglePay(): RESULT_OK");
                        PaymentData paymentData = PaymentData.getFromIntent(data);
                        handlePaymentSuccess(paymentData);
                        break;
                    case Activity.RESULT_CANCELED:
                        Logging.logDebug("Method payment with GooglePay(): RESULT_CANCELED");
                        // Nothing to here normally - the user simply cancelled without selecting a payment method.
                        break;
                    case AutoResolveHelper.RESULT_ERROR:
                        Logging.logDebug("Method payment with GooglePay(): RESULT_ERROR");
                        Status status = AutoResolveHelper.getStatusFromIntent(data);
                        if (status != null) {
                            handlePaymentError(status.getStatusCode());
                            Logging.logDebug("Method - status.getStatusMessage(): " + status.getStatusMessage());
                        } else {
                            Logging.logDebug("Method - status.getStatusMessage(): status is null");
                        }
                        break;
                }
                break;
        }
    }

    /**
     * set settings of seek bar
     * start/end value, change listener
     */
    private void initSeekBar() {
        userBonus = new BigDecimal(0);

        binding.startBonus.setText(String.format("%s %s",
                0,
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));

        String bonus = SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_BALANCE);
        if (bonus.isEmpty()) {
            bonus = "0";
        }

        binding.yourBonus.setText(String.format("%s %s %s",
                getString(R.string.yourBalance),
                bonus,
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));

        BigDecimal maxBonus;
        if (paymentCost.compareTo(new BigDecimal(bonus).add(new BigDecimal("1"))) > 0) {
            maxBonus = new BigDecimal(bonus);
        } else {
            maxBonus = paymentCost.subtract(new BigDecimal("1"));
        }

        binding.endBonus.setText(String.format("%s %s",
                0,
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        binding.seekBar.setMax(Integer.parseInt(maxBonus.toString()));
        binding.seekBar.setProgress(0);
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Logging.logDebug("progress: " + progress);
                binding.endBonus.setText(String.format("%s %s",
                        progress,
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
                userBonus = new BigDecimal(progress);
                paymentCostAfterBonus = paymentCost;
                paymentCostAfterBonus = paymentCostAfterBonus.subtract(userBonus);
                binding.finalPrice.setText(String.format("%s %s", paymentCostAfterBonus,
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void binding() {
        binding.startPrice.setText(String.format("%s %s", startCost,
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        binding.finalPrice.setText(String.format("%s %s", finalCost.add(deliveryCostStart),
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        binding.back.setOnClickListener(v -> finish());
        binding.deliveryPrice.setText(String.format("%s %s", deliveryCostStart,
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        binding.userAddress.setText(currentAddress.address);

        //set amount of products
        BigInteger productsAmount = new BigInteger("0");
        for (BasketCart basketCart : Common.basketCartRepository.getBasketCartsList()) {
            productsAmount = productsAmount.add(new BigInteger(basketCart.count));
        }
        binding.amountOfProducts.setText(String.format("%s %s %s", getText(R.string.inYourOrderProductsCount), productsAmount, getText(R.string.inYourOrderProductsPC)));

        binding.phone.addTextChangedListener(new PhoneTextFormatter(binding.phone, "+# (###) ###-##-##"));

        binding.paymentCard.setOnClickListener(v -> {
            if (preparePayment()) {
                Intent intent = new Intent(OrderActivity.this, PaymentActivity.class);
                intent.putExtra("startCost", startCost.toString());
                intent.putExtra("finalPrice", finalCost.toString());
                intent.putExtra("discountPromo", discountPromo.toString());
                intent.putExtra("deliveryCost", deliveryCostFinal.toString());
                intent.putExtra("deliveryTime", deliveryTime);
                intent.putExtra("order", "");
                intent.putExtra("floor", binding.floor.getText().toString());
                intent.putExtra("entrance", binding.entrance.getText().toString());
                intent.putExtra("phone", binding.phone.getText().toString());
                intent.putExtra("flat", binding.flat.getText().toString());
                intent.putExtra("discountType", discountType);
                intent.putExtra("countCutlery", countCutlery);
                intent.putExtra("userBonus", userBonus.toString());
                intent.putExtra(UserAddress.class.getSimpleName(), currentAddress);
                startActivityForResult(intent, PAYMENT_SUCCESS);
            }
        });

        binding.paymentCash.setOnClickListener(v -> {
            if (preparePayment()) {
                showDialogNewOrder();
            }
        });
    }

    private void showDialogNewOrder() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(OrderActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(OrderActivity.this)
                .inflate(R.layout.layout_dialog_confirm_order,
                        findViewById(R.id.layoutDialogContainer));
        builder.setView(view);

        TextView message = view.findViewById(R.id.message);
        message.setText(String.format("%s", getText(R.string.orderActivityConfirmDecription)));

        TextView textTitle = view.findViewById(R.id.textTitle);
        textTitle.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        textTitle.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));

        TextView buttonOk = view.findViewById(R.id.buttonOk);
        buttonOk.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        buttonOk.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));

        ImageView icon = view.findViewById(R.id.imageIcon);
        icon.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));

        android.app.AlertDialog alertDialog = builder.create();
        buttonOk.setOnClickListener(v -> {
            sendOrder("placeorder");
            alertDialog.dismiss();
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();
    }

    private void checkIsReadyToPay() {
        // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
        // OnCompleteListener to be triggered when the result of the call is known.
        PaymentsUtil.isReadyToPay(paymentsClient).addOnCompleteListener(
                task -> {
                    try {
                        Logging.logDebug("isReadyToPay");
                        boolean result = task.getResult(ApiException.class);
                        setPwgAvailable(result);
                    } catch (ApiException exception) {
                        Logging.logDebug(exception.toString());
                    }
                });
    }

    private void setPwgAvailable(boolean available) {
        // If isReadyToPay returned true, show the button and hide the "checking" text. Otherwise,
        // notify the user that Pay with Google is not available.
        // Please adjust to fit in with your current user flow. You are not required to explicitly
        // let the user know if isReadyToPay returns false.
        if (available) {
            bindingGooglePayButton();
        } else {
            binding.pwgStatus.setText(R.string.pwg_status_unavailable);
        }
    }

    private void bindingGooglePayButton() {
        binding.pwgStatus.setVisibility(View.GONE);
        binding.pwgButton.getRoot().setVisibility(View.VISIBLE);
        binding.pwgButton.getRoot().setOnClickListener(v -> {
            if (preparePayment()) {
                if (Objects.equals(SharedPreferencesSetting.getDataString(SharedPreferencesSetting.CURRENCY), "RUB")) {
                    requestPayment(paymentsClient);
                } else {
                    convertPrice();
                }
            }
        });
    }

    // This method is called when the Pay with Google button is clicked.
    public void requestPayment(PaymentsClient paymentsClient) {
        // Disables the button to prevent multiple clicks.
        //pwg_button.setClickable(false);
        Logging.logDebug("requestPayment");

        // The price provided to the API should include taxes and shipping.
        // This price is not displayed to the user.
        String price = paymentCost.toString();

        TransactionInfo transaction = PaymentsUtil.createTransaction(price);
        PaymentDataRequest request = PaymentsUtil.createPaymentDataRequest(transaction);
        Task<PaymentData> futurePaymentData = paymentsClient.loadPaymentData(request);
        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        AutoResolveHelper.resolveTask(futurePaymentData, Objects.requireNonNull(this), LOAD_PAYMENT_DATA_REQUEST_CODE);
    }

    public void showLoading() {
        if (binding.progress.getVisibility() == View.VISIBLE) {
            return;
        }
        binding.progress.setVisibility(View.VISIBLE);
    }

    public void hideLoading() {
        if (binding.progress.getVisibility() == View.GONE) {
            return;
        }
        binding.progress.setVisibility(View.GONE);
    }

    private void convertPrice() {
        Logging.logDebug("Method convertPrice()");
        RetrofitClient.
                getClient(RestAPI.URL_API_MAIN)
                .create(RestAPI.class)
                .convertPrice(
                        paymentCost.toString(),
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.CURRENCY)
                ).enqueue(new Callback<PriceConverterResponse>() {
            @Override
            public void onResponse(@NotNull Call<PriceConverterResponse> call, @NotNull Response<PriceConverterResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        Logging.logDebug("Method convertPrice() - paymentCost: " + response.body().getPrice());
                        paymentCost = new BigDecimal(response.body().getPrice());
                        requestPayment(paymentsClient);
                    } else {
                        Logging.logError("Method convertPrice() - by some reason response is null!");
                    }
                } else {
                    Logging.logError("Method convertPrice() - response is not successful. " +
                            "Code: " + response.code() + "Message: " + response.message());
                }
            }

            @Override
            public void onFailure(@NotNull Call<PriceConverterResponse> call, @NotNull Throwable t) {
                Logging.logError("Method convertPrice() - failure: " + t.toString());
            }
        });
    }

    private void handlePaymentSuccess(PaymentData paymentData) {
        // PaymentMethodToken contains the payment information, as well as any additional
        // requested information, such as billing and shipping address.
        // Refer to your processor's documentation on how to proceed from here.
        PaymentMethodToken token = paymentData.getPaymentMethodToken();
        Logging.logDebug("token.toString()" + token.toString());

        // getPaymentMethodToken will only return null if Payment Method Tokenization Parameters was
        // not set in the PaymentRequest.
        if (token != null) {
            String billingName = paymentData.getCardInfo().getBillingAddress().getName();
            Toast.makeText(this, getString(R.string.payments_show_name, billingName), Toast.LENGTH_LONG).show();
            // Use token.getToken() to get the token string.
            Logging.logDebug("token.getToken()" + token.getToken());
            Logging.logDebug("Method handlePaymentSuccess() - paymentCost: " + paymentCost);
            charge(token.getToken(), "Google Pay", paymentCost, order);
        }
    }

    private void handlePaymentError(int statusCode) {
        // At this stage, the user has already seen a popup informing them an error occurred.
        // Normally, only logging is required.
        // statusCode will hold the value of any constant from CommonStatusCode or one of the
        // WalletConstants.ERROR_CODE_* constants.
        Logging.logDebug(String.format("Error code: %d", statusCode));
    }

    // ???????????? ???? ???????????????????? ???????????????????????????? ??????????????
    private void charge(String cardCryptogramPacket, String cardHolderName, BigDecimal
            paymentCost, String order) {
        compositeDisposable.add(PayApi
                .charge(cardCryptogramPacket, cardHolderName, paymentCost, order, "googlepay")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> showLoading())
                .doOnEach(notification -> hideLoading())
                .subscribe(this::checkResponse, this::handleError));
    }

    // ?????????????????? ???????????????????? ???? ?????????????????????????? ?? ???????????????????????????? 3DS
    private void checkResponse(Transaction transaction) {
        Logging.logDebug("Method checkResponse()");
        if (transaction.getPaReq() != null && transaction.getAcsUrl() != null) {
            // ???????????????????? 3DS ??????????
            Logging.logDebug("show3DS");
            show3DS(transaction);
        } else {
            // ???????????????????? ??????????????????
            Logging.logDebug("transaction.getCardHolderMessage(): " + transaction.getCardHolderMessage());
            Logging.logDebug("transaction.getReasonCode(): " + transaction.getReasonCode());
            Logging.logDebug("transaction.getId(): " + transaction.getId());
            showToast(transaction.getCardHolderMessage());
            if (transaction.getReasonCode() == 0) {
                transactionID = transaction.getId();
                SharedPreferencesSetting.setData(SharedPreferencesSetting.DISCOUNT_TYPE, "");
                SharedPreferencesSetting.setData(SharedPreferencesSetting.DISCOUNT_AMOUNT, "0");
                SharedPreferencesSetting.setData(SharedPreferencesSetting.DISCOUNT_NAME, "");
                sendOrder("googlepay");
            }
        }
    }

    @Override
    public void onAuthorizationCompleted(String md, String paRes) {
        post3ds(md, paRes);
    }

    @Override
    public void onAuthorizationFailed(String html) {
        Toast.makeText(this, "AuthorizationFailed: " + html, Toast.LENGTH_SHORT).show();
        Logging.logDebug("onAuthorizationFailed: " + html);
    }

    private void show3DS(Transaction transaction) {
        // ?????????????????? 3ds ??????????
        ThreeDsDialogFragment.newInstance(transaction.getAcsUrl(),
                transaction.getId(),
                transaction.getPaReq())
                .show(this.getSupportFragmentManager(), "3DS");
    }

    // ?????????????????? ???????????????????? ?????????? ?????????????????????? 3DS ??????????
    private void post3ds(String md, String paRes) {
        compositeDisposable.add(PayApi
                .post3ds(md, paRes, "googlepay")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> showLoading())
                .doOnEach(notification -> hideLoading())
                .subscribe(this::checkResponse, this::handleError));
    }

    public void handleError(Throwable throwable, Class... ignoreClasses) {
        if (ignoreClasses.length > 0) {
            List<Class> classList = Arrays.asList(ignoreClasses);
            if (classList.contains(throwable.getClass())) {
                return;
            }
        }
        if (throwable instanceof PayApiError) {
            PayApiError apiError = (PayApiError) throwable;
            String message = apiError.getMessage();
            Logging.logError("handleError() - message: " + message);
            showToast(message);
        } else if (throwable instanceof UnknownHostException) {
            Logging.logError("UnknownHostException: " + getString(R.string.common_no_internet_connection));
            showToast(getString(R.string.common_no_internet_connection));
        } else {
            Logging.logError("handleError: " + throwable.getMessage());
            showToast(throwable.getMessage());
        }
    }

    public void showToast(String message) {
        Logging.logDebug("message: " + message);
        Toast.makeText(OrderActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        binding.entrance.setText(userSettings.getString(ENTRANCE, ""));
        binding.floor.setText(userSettings.getString(FLOOR, ""));
        binding.flat.setText(userSettings.getString(FLAT, ""));
        binding.phone.setText(userSettings.getString(PHONE, ""));
    }

    @Override
    protected void onStop() {
        SharedPreferences.Editor editor = userSettings.edit();
        editor.putString(ENTRANCE, binding.entrance.getText().toString());
        editor.putString(FLOOR, binding.floor.getText().toString());
        editor.putString(FLAT, binding.flat.getText().toString());
        editor.putString(PHONE, binding.phone.getText().toString());
        editor.apply();
        super.onStop();
    }
}