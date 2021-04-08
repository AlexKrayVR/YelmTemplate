package yelm.io.raccoon.order;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
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
import yelm.io.raccoon.constants.Constants;
import yelm.io.raccoon.order.promocode.PromoCodeClass;
import yelm.io.raccoon.payment.PayApi;
import yelm.io.raccoon.payment.PaymentActivity;
import yelm.io.raccoon.payment.models.Transaction;
import yelm.io.raccoon.payment.response.PayApiError;
import yelm.io.raccoon.rest.rest_api.RestAPI;
import yelm.io.raccoon.rest.client.RetrofitClient;
import yelm.io.raccoon.support_stuff.Logging;
import yelm.io.raccoon.R;
import yelm.io.raccoon.database_new.basket_new.BasketCart;
import yelm.io.raccoon.database_new.Common;
import yelm.io.raccoon.database_new.user_addresses.UserAddress;
import yelm.io.raccoon.databinding.ActivityOrderNewBinding;
import yelm.io.raccoon.loader.controller.LoaderActivity;
import yelm.io.raccoon.payment.googleplay.PaymentsUtil;
import yelm.io.raccoon.support_stuff.PhoneTextFormatter;

public class OrderActivity extends AppCompatActivity implements ThreeDSDialogListener {
    ActivityOrderNewBinding binding;

    private static final int PAYMENT_SUCCESS = 77;

    private PaymentsClient paymentsClient;

    private BigDecimal startCost = new BigDecimal("0");
    private BigDecimal finalCost = new BigDecimal("0");//without delivery cost
    private BigDecimal paymentCost = new BigDecimal("0");

    private BigDecimal deliveryCostStart = new BigDecimal("0");
    private BigDecimal deliveryCostFinal = new BigDecimal("0");
    private BigDecimal discountPromo = new BigDecimal("0");

    private String deliveryTime = "";
    private String discountType = "0";
    UserAddress currentAddress;

    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 991;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String transactionID = "0";
    private String order = "";
    private String userID = LoaderActivity.settings.getString(LoaderActivity.USER_NAME, "");
    private String currency = LoaderActivity.settings.getString(LoaderActivity.CURRENCY, "");
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

        Bundle args = getIntent().getExtras();
        if (args != null) {
            finalCost = new BigDecimal(args.getString("finalPrice"));
            startCost = finalCost;
            deliveryCostStart = new BigDecimal(args.getString("deliveryCost"));
            deliveryCostFinal = deliveryCostStart;
            paymentCost = finalCost.add(deliveryCostStart);
            deliveryTime = args.getString("deliveryTime");
            currentAddress = (UserAddress) args.getSerializable(UserAddress.class.getSimpleName());
            countCutlery = args.getString("countCutlery");

            Logging.logDebug( "countCutlery: " + countCutlery);
            Logging.logDebug("startCost: " + startCost);
            Logging.logDebug( "finalCost: " + finalCost);
            Logging.logDebug("paymentCost: " + paymentCost);
            Logging.logDebug("deliveryCost: " + discountPromo);
            Logging.logDebug( "deliveryPrice: " + deliveryCostStart);
            Logging.logDebug( "deliveryTime: " + deliveryTime);
            Logging.logDebug("currentAddress: " + currentAddress.toString());
        }
        binding();
        checkEditText();
        paymentsClient = PaymentsUtil.createPaymentsClient(this);
        checkIsReadyToPay();
        bindingChosePaymentType();
        binding.applyPromocode.setOnClickListener(v -> getPromoCode());
        getPromoIfExist();
    }

    private void checkEditText() {
        binding.entrance.addTextChangedListener(new CustomTextWatcher(binding.entrance, this));
        binding.floor.addTextChangedListener(new CustomTextWatcher(binding.floor, this));
        binding.flat.addTextChangedListener(new CustomTextWatcher(binding.flat, this));
        binding.phone.addTextChangedListener(new CustomTextWatcher(binding.phone, this));
    }

    private static class CustomTextWatcher implements TextWatcher {
        private EditText editText;
        private Context context;

        public CustomTextWatcher(EditText e, Context context) {
            this.editText = e;
            this.context = context;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.toString().trim().length() == 0) {
                editText.setBackground(ContextCompat.getDrawable(context, R.drawable.back_edittext_red));
            } else {
                editText.setBackground(ContextCompat.getDrawable(context, R.drawable.back_edittext_green));
            }
        }

        public void afterTextChanged(Editable s) {
        }
    }


    private void getPromoIfExist() {
        String type = LoaderActivity.settings.getString(LoaderActivity.DISCOUNT_TYPE, "");
        String amount = LoaderActivity.settings.getString(LoaderActivity.DISCOUNT_AMOUNT, "0");
        String name = LoaderActivity.settings.getString(LoaderActivity.DISCOUNT_NAME, "");
        Logging.logDebug( "type: " + type);
        Logging.logDebug("amount: " + amount);
        Logging.logDebug( "name: " + name);


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
                            LoaderActivity.settings.getString(LoaderActivity.USER_NAME, "")
                    ).enqueue(new Callback<PromoCodeClass>() {
                @Override
                public void onResponse(@NotNull Call<PromoCodeClass> call, @NotNull Response<PromoCodeClass> response) {
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            Logging.logDebug( " " + Constants.ShopID);
                            if (response.body().getStatus().equals("200")) {
                                setPromoCode(response.body().getPromocode().getType(),
                                        response.body().getPromocode().getAmount(),
                                        response.body().getPromocode().getName());
                            }
                            showToast(response.body().getMessage());
                        } else {
                            Logging.logError( "Method getPromoCode() - by some reason response is null!");
                        }

                    } else {
                        Logging.logError("Method getPromoCode() - response is not successful. " +
                                "Code: " + response.code() + "Message: " + response.message());
                    }
                }

                @Override
                public void onFailure(@NotNull Call<PromoCodeClass> call, @NotNull Throwable t) {
                    Logging.logError( "Method getPromoCode() - failure: " + t.toString());
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

        SharedPreferences.Editor editor = LoaderActivity.settings.edit();
        editor.putString(LoaderActivity.DISCOUNT_TYPE, type);
        editor.putString(LoaderActivity.DISCOUNT_AMOUNT, amount);
        editor.putString(LoaderActivity.DISCOUNT_NAME, name);
        editor.apply();

        switch (type) {
            case "full":
                binding.discountPercent.setText(String.format("%s",
                        getText(R.string.orderDiscount)));
                binding.discountPrice.setText(String.format("%s %s", discountPromo,
                        LoaderActivity.settings.getString(LoaderActivity.PRICE_IN, "")));
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
                        LoaderActivity.settings.getString(LoaderActivity.PRICE_IN, "")));
                deliveryCostFinal = deliveryCostFinal.subtract(discountDelivery);
                break;
            case "percent":
                binding.discountPercent.setText(String.format("%s %s%%", getText(R.string.orderDiscount), discountPromo));
                BigDecimal discount = discountPromo.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
                discount = discount.multiply(finalCost).setScale(2, BigDecimal.ROUND_HALF_UP);
                binding.discountPrice.setText(String.format("%s %s", discount,
                        LoaderActivity.settings.getString(LoaderActivity.PRICE_IN, "")));
                finalCost = finalCost.subtract(discount);
                if (finalCost.compareTo(new BigDecimal("0")) == 0) {
                    finalCost = new BigDecimal("1");
                }
                break;
        }
        paymentCost = finalCost.add(deliveryCostFinal);
        binding.finalPrice.setText(String.format("%s %s", finalCost.add(deliveryCostFinal), LoaderActivity.settings.getString(LoaderActivity.PRICE_IN, "")));
        Logging.logDebug( "finalCost: " + finalCost);
        Logging.logDebug( "paymentCost: " + paymentCost);
    }

    private void sendOrder() {
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
        Logging.logDebug( "jsonObjectItems: " + jsonObjectItems.toString());
        RetrofitClient.
                getClient(RestAPI.URL_API_MAIN)
                .create(RestAPI.class)
                .sendOrder("3.1",
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
                        "googlepay",
                        binding.floor.getText().toString(),
                        binding.entrance.getText().toString(),
                        paymentCost.toString(),
                        binding.phone.getText().toString(),
                        binding.flat.getText().toString(),
                        "delivery",
                        jsonObjectItems.toString(),
                        deliveryCostFinal.toString(),
                        currency,
                        Constants.ShopID,
                        discountType,
                        countCutlery
                ).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Logging.logDebug( "Method sendOrder() - response.code(): " + response.code());
                    Common.basketCartRepository.emptyBasketCart();
                    Intent intentGP = new Intent();
                    intentGP.putExtra("success", "googlePay");
                    setResult(RESULT_OK, intentGP);
                    finish();
                } else {
                    Logging.logError( "Method sendOrder() - response is not successful. " +
                            "Code: " + response.code() + "Message: " + response.message());
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                Logging.logError( "Method sendOrder() - failure: " + t.toString());
            }
        });
    }

    private boolean preparePayment() {
        String phone = binding.phone.getText().toString();
        Logging.logDebug( "phone: " + phone);
        phone = phone.replaceAll("\\D", "");
        Logging.logDebug("phone after replacement: " + phone);
        if (phone.trim().equals("") || phone.length() != 11) {
            showToast(getText(R.string.orderActivityEnterCorrectPhone).toString());
            return false;
        }

        String floor = binding.floor.getText().toString();
        if (floor.trim().equals("")) {
            showToast(getText(R.string.orderActivityEnterFloor).toString());
            return false;
        }

        String entrance = binding.entrance.getText().toString();
        if (entrance.trim().equals("")) {
            showToast(getText(R.string.orderActivityEnterEntrance).toString());
            return false;
        }

        String flat = binding.flat.getText().toString();
        if (flat.trim().equals("")) {
            showToast(getText(R.string.orderActivityEnterFlat).toString());
            return false;
        }
        return true;
    }

    private void bindingChosePaymentType() {
        binding.cardPay.setOnClickListener(view -> {
            binding.cardPay.setCardBackgroundColor(getResources().getColor(R.color.mainThemeColor));
            binding.cardPayText.setTextColor(getResources().getColor(R.color.whiteColor));
            binding.googlepayPay.setCardBackgroundColor(Color.TRANSPARENT);
            binding.googlePayText.setTextColor(getResources().getColor(R.color.colorText));
            binding.paymentCard.setVisibility(View.VISIBLE);
            binding.googlePay.setVisibility(View.GONE);
        });
        binding.googlepayPay.setOnClickListener(view -> {
            binding.googlepayPay.setCardBackgroundColor(getResources().getColor(R.color.mainThemeColor));
            binding.googlePayText.setTextColor(getResources().getColor(R.color.whiteColor));
            binding.cardPay.setCardBackgroundColor(Color.TRANSPARENT);
            binding.cardPayText.setTextColor(getResources().getColor(R.color.colorText));
            binding.paymentCard.setVisibility(View.GONE);
            binding.googlePay.setVisibility(View.VISIBLE);
        });
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
                        Logging.logDebug( "Method payment with GooglePay(): RESULT_ERROR");
                        Status status = AutoResolveHelper.getStatusFromIntent(data);
                        if (status != null) {
                            handlePaymentError(status.getStatusCode());
                            Logging.logDebug( "Method - status.getStatusMessage(): " + status.getStatusMessage());
                        } else {
                            Logging.logDebug("Method - status.getStatusMessage(): status is null");
                        }
                        break;
                }
                break;
        }
    }

    private void binding() {
        binding.startPrice.setText(String.format("%s %s", startCost, LoaderActivity.settings.getString(LoaderActivity.PRICE_IN, "")));
        binding.finalPrice.setText(String.format("%s %s", finalCost.add(deliveryCostStart), LoaderActivity.settings.getString(LoaderActivity.PRICE_IN, "")));
        binding.back.setOnClickListener(v -> finish());
        binding.deliveryPrice.setText(String.format("%s %s", deliveryCostStart, LoaderActivity.settings.getString(LoaderActivity.PRICE_IN, "")));
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
                intent.putExtra(UserAddress.class.getSimpleName(), currentAddress);
                startActivityForResult(intent, PAYMENT_SUCCESS);
            }
        });
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
                        Logging.logDebug( exception.toString());
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
                if (Objects.equals(LoaderActivity.settings.getString(LoaderActivity.CURRENCY, ""), "RUB")) {
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
        Logging.logDebug( "requestPayment");

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
                        LoaderActivity.settings.getString(LoaderActivity.CURRENCY, "")
                ).enqueue(new Callback<PriceConverterResponse>() {
            @Override
            public void onResponse(@NotNull Call<PriceConverterResponse> call, @NotNull Response<PriceConverterResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        Logging.logDebug( "Method convertPrice() - paymentCost: " + response.body().getPrice());
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
            Logging.logDebug( "token.getToken()" + token.getToken());
            Logging.logDebug("Method handlePaymentSuccess() - paymentCost: " + paymentCost);
            charge(token.getToken(), "Google Pay", paymentCost, order);
        }
    }

    private void handlePaymentError(int statusCode) {
        // At this stage, the user has already seen a popup informing them an error occurred.
        // Normally, only logging is required.
        // statusCode will hold the value of any constant from CommonStatusCode or one of the
        // WalletConstants.ERROR_CODE_* constants.
        Logging.logDebug( String.format("Error code: %d", statusCode));
    }

    // Запрос на проведение одностадийного платежа
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

    // Проверяем необходимо ли подтверждение с использованием 3DS
    private void checkResponse(Transaction transaction) {
        if (transaction.getPaReq() != null && transaction.getAcsUrl() != null) {
            // Показываем 3DS форму
            Logging.logDebug("show3DS");
            show3DS(transaction);
        } else {
            // Показываем результат
            Logging.logDebug( "transaction result: " + transaction.getCardHolderMessage());
            Logging.logDebug( "transaction.getReasonCode(): " + transaction.getReasonCode());
            showToast(transaction.getCardHolderMessage());
            if (transaction.getReasonCode() == 0) {
                transactionID = transaction.getId();
                Logging.logDebug( "transaction.getId(): " + transaction.getId());
                SharedPreferences.Editor editor = LoaderActivity.settings.edit();
                editor.putString(LoaderActivity.DISCOUNT_TYPE, "");
                editor.putString(LoaderActivity.DISCOUNT_AMOUNT, "0");
                editor.putString(LoaderActivity.DISCOUNT_NAME, "");
                editor.apply();
                sendOrder();
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
        Logging.logDebug( "onAuthorizationFailed: " + html);
    }

    private void show3DS(Transaction transaction) {
        // Открываем 3ds форму
        ThreeDsDialogFragment.newInstance(transaction.getAcsUrl(),
                transaction.getId(),
                transaction.getPaReq())
                .show(this.getSupportFragmentManager(), "3DS");
    }

    // Завершаем транзакцию после прохождения 3DS формы
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
            Logging.logDebug("apiError.getMessage(): " + apiError.getMessage());
            showToast(message);
        } else if (throwable instanceof UnknownHostException) {
            Logging.logDebug( "UnknownHostException: " + getString(R.string.common_no_internet_connection));
            showToast(getString(R.string.common_no_internet_connection));
        } else {
            Logging.logDebug("handleError: " + throwable.getMessage());
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
        if (userSettings.contains(ENTRANCE)) {
            binding.entrance.setText(userSettings.getString(ENTRANCE, ""));
            binding.floor.setText(userSettings.getString(FLOOR, ""));
            binding.flat.setText(userSettings.getString(FLAT, ""));
            binding.phone.setText(userSettings.getString(PHONE, ""));
        }
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