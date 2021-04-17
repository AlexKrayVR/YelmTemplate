package yelm.io.raccoon.payment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.cloudpayments.sdk.cp_card.CPCard;
import ru.cloudpayments.sdk.cp_card.api.CPCardApi;
import ru.cloudpayments.sdk.three_ds.ThreeDSDialogListener;
import ru.cloudpayments.sdk.three_ds.ThreeDsDialogFragment;
import yelm.io.raccoon.R;
import yelm.io.raccoon.database_new.Common;
import yelm.io.raccoon.database_new.basket_new.BasketCart;
import yelm.io.raccoon.database_new.user_addresses.UserAddress;
import yelm.io.raccoon.loader.app_settings.SharedPreferencesSetting;
import yelm.io.raccoon.loader.controller.LoaderActivity;
import yelm.io.raccoon.order.model.PriceConverterResponse;
import yelm.io.raccoon.payment.models.Transaction;
import yelm.io.raccoon.payment.response.PayApiError;
import yelm.io.raccoon.rest.rest_api.RestAPI;
import yelm.io.raccoon.rest.client.RetrofitClient;
import yelm.io.raccoon.support_stuff.Logging;

public class PaymentActivity extends AppCompatActivity implements ThreeDSDialogListener {

    private static final int CARD_NUMBER_TOTAL_SYMBOLS = 19; // size of pattern 0000-0000-0000-0000
    private static final int CARD_NUMBER_TOTAL_DIGITS = 16; // max numbers of digits in pattern: 0000 x 4
    private static final int CARD_NUMBER_DIVIDER_MODULO = 5; // means divider position is every 5th symbol beginning with 1
    private static final int CARD_NUMBER_DIVIDER_POSITION = CARD_NUMBER_DIVIDER_MODULO - 1; // means divider position is every 4th symbol beginning with 0
    private static final char CARD_NUMBER_DIVIDER = ' ';

    private static final int CARD_DATE_TOTAL_SYMBOLS = 5; // size of pattern MM/YY
    private static final int CARD_DATE_TOTAL_DIGITS = 4; // max numbers of digits in pattern: MM + YY
    private static final int CARD_DATE_DIVIDER_MODULO = 3; // means divider position is every 3rd symbol beginning with 1
    private static final int CARD_DATE_DIVIDER_POSITION = CARD_DATE_DIVIDER_MODULO - 1; // means divider position is every 2nd symbol beginning with 0
    private static final char CARD_DATE_DIVIDER = '/';

    private static final int CARD_CVC_TOTAL_SYMBOLS = 3;

    //order dara
    private BigDecimal paymentCost = new BigDecimal("0");
    private BigDecimal deliveryCost = new BigDecimal("0");
    private BigDecimal startCost = new BigDecimal("0");
    private BigDecimal discountPromo = new BigDecimal("0");
    private String order = "";
    private String discountType = "";

    private String userID = SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_NAME);
    private String currency = SharedPreferencesSetting.getDataString(SharedPreferencesSetting.CURRENCY);
    private String transactionID = "0";
    UserAddress currentAddress;
    //private String deliveryTime = "";
    private String entrance = "";
    private String floor = "";
    private String flat = "";
    private String phone = "";
    private String countCutlery = "1";
    protected CompositeDisposable compositeDisposable = new CompositeDisposable();

    @BindView(R.id.text_total)
    TextView textViewTotal;

    @BindView(R.id.edit_card_number)
    EditText editTextCardNumber;

    @BindView(R.id.progress)
    ProgressBar progress;

    @OnTextChanged(value = R.id.edit_card_number, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    protected void onCardNumberTextChanged(Editable s) {
        if (!isInputCorrect(s, CARD_NUMBER_TOTAL_SYMBOLS, CARD_NUMBER_DIVIDER_MODULO, CARD_NUMBER_DIVIDER)) {
            s.replace(0, s.length(), concatString(getDigitArray(s, CARD_NUMBER_TOTAL_DIGITS), CARD_NUMBER_DIVIDER_POSITION, CARD_NUMBER_DIVIDER));
        }
    }

    @BindView(R.id.edit_card_date)
    EditText editTextCardDate;

    @OnTextChanged(value = R.id.edit_card_date, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    protected void onCardDateTextChanged(Editable s) {
        if (!isInputCorrect(s, CARD_DATE_TOTAL_SYMBOLS, CARD_DATE_DIVIDER_MODULO, CARD_DATE_DIVIDER)) {
            s.replace(0, s.length(), concatString(getDigitArray(s, CARD_DATE_TOTAL_DIGITS), CARD_DATE_DIVIDER_POSITION, CARD_DATE_DIVIDER));
        }
    }

    @BindView(R.id.edit_card_cvc)
    EditText editTextCardCVC;

    @OnTextChanged(value = R.id.edit_card_cvc, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    protected void onCardCVCTextChanged(Editable s) {
        if (s.length() > CARD_CVC_TOTAL_SYMBOLS) {
            s.delete(CARD_CVC_TOTAL_SYMBOLS, s.length());
        }
    }

    @BindView(R.id.edit_card_holder_name)
    EditText editTextCardHolderName;

    @OnClick(R.id.back)
    void onBackClick() {
        finish();
    }

    @OnClick(R.id.button_payment)
    void onPaymentClick() {
        String cardNumber = editTextCardNumber.getText().toString().replace(" ", "");
        String cardDate = editTextCardDate.getText().toString().replace("/", "");
        String cardCVC = editTextCardCVC.getText().toString();
        String cardHolderName = editTextCardHolderName.getText().toString();

        CPCardApi api = new CPCardApi(this);

        // Проверям номер карты.
        if (!CPCard.isValidNumber(cardNumber)) {
            showToast(getString(R.string.checkout_error_card_number));
            return;
        }

        // Проверям срок действия карты.
        if (!CPCard.isValidExpDate(cardDate)) {
            showToast(getString(R.string.checkout_error_card_date));
            return;
        }

        // Проверям cvc код карты.
        if (cardCVC.length() != 3) {
            showToast(getString(R.string.checkout_error_card_cvc));
            return;
        }

        // Пример определения банка по номеру карты
        api.getBinInfo(cardNumber, binInfo -> Logging.logDebug("Bank name: " + binInfo.getBankName()),
                message -> Logging.logError("Bank name error: " + message));

        // После проверики, если все данные корректны, создаем объект CPCard, иначе при попытке создания объекта CPCard мы получим исключение.
        CPCard card = new CPCard(cardNumber, cardDate, cardCVC);

        // Создаем криптограмму карточных данных
        String cardCryptogram = null;

        try {
            // Чтобы создать криптограмму необходим PublicID (его можно посмотреть в личном кабинете)
            cardCryptogram = card.cardCryptogram(Constants.MERCHANT_PUBLIC_ID);
        } catch (UnsupportedEncodingException | NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        // Если данные карты введены корректно и криптограмма успешно созданна
        // используя методы API выполняем оплату по криптограмме

        Logging.logDebug("cardCryptogram: " + cardCryptogram);

        if (cardCryptogram != null) {
            if (Objects.equals(SharedPreferencesSetting.getDataString(SharedPreferencesSetting.CURRENCY), "RUB")) {
                auth(cardCryptogram, cardHolderName, paymentCost, order);
            } else {
                convertPrice(cardCryptogram, cardHolderName);
            }
        }
    }

    private void convertPrice(String cardCryptogram, String cardHolderName) {
        showLoading();
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
                        hideLoading();
                        Logging.logDebug("Method convertPrice() - response.code(): " + response.code());
                        auth(cardCryptogram, cardHolderName, new BigDecimal(response.body().getPrice()), order);
                    } else {
                        hideLoading();
                        Logging.logError("Method convertPrice() - by some reason response is null!");
                    }
                } else {
                    hideLoading();
                    Logging.logError("Method convertPrice() - response is not successful. " +
                            "Code: " + response.code() + "Message: " + response.message());
                }
            }

            @Override
            public void onFailure(@NotNull Call<PriceConverterResponse> call, @NotNull Throwable t) {
                hideLoading();
                Logging.logError("Method convertPrice() - failure: " + t.toString());
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_main);
        ButterKnife.bind(this);

        Bundle args = getIntent().getExtras();
        if (args != null) {
            startCost = new BigDecimal(args.getString("startCost"));
            deliveryCost = new BigDecimal(args.getString("deliveryCost"));
            paymentCost = new BigDecimal(args.getString("finalPrice")).add(deliveryCost);
            discountPromo = new BigDecimal(args.getString("discountPromo"));
            discountType = args.getString("discountType");
            floor = args.getString("floor");
            entrance = args.getString("entrance");
            phone = args.getString("phone");
            flat = args.getString("flat");
            currentAddress = (UserAddress) args.getSerializable(UserAddress.class.getSimpleName());
            countCutlery = args.getString("countCutlery");
            Logging.logDebug("countCutlery: " + countCutlery);
            Logging.logDebug("startCost: " + startCost);
            Logging.logDebug("paymentCost: " + paymentCost);
            Logging.logDebug("discountPromo: " + discountPromo);
            Logging.logDebug("deliveryCost: " + deliveryCost);
            Logging.logDebug("floor: " + floor);
            Logging.logDebug("entrance: " + entrance);
            Logging.logDebug("phone: " + phone);
            Logging.logDebug("flat: " + flat);
            Logging.logDebug("discountType: " + discountType);
            Logging.logDebug("currentAddress: " + currentAddress.toString());
        }

        textViewTotal.setText(new StringBuilder()
                .append(paymentCost)
                .append(" ")
                .append(SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
    }

    // Запрос на проведение двустадийного платежа
    private void auth(String cardCryptogramPacket, String cardHolderName, BigDecimal price, String order) {
        Logging.logDebug("Method auth()");
        compositeDisposable.add(PayApi
                .auth(cardCryptogramPacket, cardHolderName, price, order, "card")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> showLoading())
                .doOnEach(notification -> hideLoading())
                .subscribe(this::checkResponse, this::handleError));
    }

    // Проверяем необходимо ли подтверждение с использованием 3DS
    private void checkResponse(Transaction transaction) {
        Logging.logDebug("Method checkResponse()");
        if (transaction.getPaReq() != null && transaction.getAcsUrl() != null) {
            // Показываем 3DS форму
            Logging.logDebug("show3DS");
            show3DS(transaction);
        } else {
            // Показываем результат:
            Logging.logDebug("transaction.getCardHolderMessage(): " + transaction.getCardHolderMessage());
            Logging.logDebug("transaction.getReasonCode(): " + transaction.getReasonCode());
            Logging.logDebug("transaction.getId(): " + transaction.getId());
            showToast(transaction.getCardHolderMessage());
            if (transaction.getReasonCode() == 0) {
                transactionID = transaction.getId();
                SharedPreferencesSetting.setData(SharedPreferencesSetting.DISCOUNT_TYPE, "");
                SharedPreferencesSetting.setData(SharedPreferencesSetting.DISCOUNT_AMOUNT, "0");
                SharedPreferencesSetting.setData(SharedPreferencesSetting.DISCOUNT_NAME, "");
                sendOrder();
            }
        }
    }

    private void show3DS(Transaction transaction) {
        // Открываем 3ds форму
        ThreeDsDialogFragment.newInstance(transaction.getAcsUrl(),
                transaction.getId(),
                transaction.getPaReq())
                .show(this.getSupportFragmentManager(), "3DS");
    }

    public void handleError(Throwable throwable, Class... ignoreClasses) {
        Logging.logError("handleError");
        if (ignoreClasses.length > 0) {
            List<Class> classList = Arrays.asList(ignoreClasses);
            if (classList.contains(throwable.getClass())) {
                return;
            }
        }
        if (throwable instanceof PayApiError) {
            PayApiError apiError = (PayApiError) throwable;
            String message = apiError.getMessage();
            Logging.logError("apiError.getMessage: " + message);
            showToast(message);
        } else if (throwable instanceof UnknownHostException) {
            showToast(getString(R.string.common_no_internet_connection));
        } else {
            showToast(throwable.getMessage());
            Logging.logError("throwable.getMessage(): " + throwable.getMessage());
        }
    }

    // Завершаем транзакцию после прохождения 3DS формы
    private void post3ds(String md, String paRes) {
        compositeDisposable.add(PayApi
                .post3ds(md, paRes, "card")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> showLoading())
                .doOnEach(notification -> hideLoading())
                .subscribe(this::checkResponse, this::handleError));
    }

    @Override
    public void onAuthorizationCompleted(String md, String paRes) {
        post3ds(md, paRes);
    }

    @Override
    public void onAuthorizationFailed(String html) {
        Toast.makeText(this, "AuthorizationFailed: " + html, Toast.LENGTH_SHORT).show();
    }

    public void showLoading() {
        if (progress.getVisibility() == View.VISIBLE) {
            return;
        }
        progress.setVisibility(View.VISIBLE);
    }

    public void hideLoading() {
        if (progress.getVisibility() == View.GONE) {
            return;
        }
        progress.setVisibility(View.GONE);
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void sendOrder() {
        Logging.logDebug("sendOrder()");
        showLoading();
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
                        "card",
                        floor,
                        entrance,
                        paymentCost.toString(),
                        phone,
                        flat,
                        "delivery",
                        jsonObjectItems.toString(),
                        deliveryCost.toString(),
                        currency,
                        yelm.io.raccoon.constants.Constants.ShopID,
                        discountType,
                        countCutlery
                ).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    hideLoading();
                    Logging.logDebug("Method sendOrder() - response.code(): " + response.code());
                    Common.basketCartRepository.emptyBasketCart();
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    hideLoading();
                    Logging.logError("Method sendOrder() - response is not successful. " +
                            "Code: " + response.code() + "Message: " + response.message());
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                hideLoading();
                Logging.logError("Method sendOrder() - failure: " + t.toString());
            }
        });
    }

    private boolean isInputCorrect(Editable s, int size, int dividerPosition, char divider) {
        boolean isCorrect = s.length() <= size;
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (i + 1) % dividerPosition == 0) {
                isCorrect &= divider == s.charAt(i);
            } else {
                isCorrect &= Character.isDigit(s.charAt(i));
            }
        }
        return isCorrect;
    }

    private String concatString(char[] digits, int dividerPosition, char divider) {
        final StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < digits.length; i++) {
            if (digits[i] != 0) {
                formatted.append(digits[i]);
                if ((i > 0) && (i < (digits.length - 1)) && (((i + 1) % dividerPosition) == 0)) {
                    formatted.append(divider);
                }
            }
        }
        return formatted.toString();
    }

    private char[] getDigitArray(final Editable s, final int size) {
        char[] digits = new char[size];
        int index = 0;
        for (int i = 0; i < s.length() && index < size; i++) {
            char current = s.charAt(i);
            if (Character.isDigit(current)) {
                digits[index] = current;
                index++;
            }
        }
        return digits;
    }
}