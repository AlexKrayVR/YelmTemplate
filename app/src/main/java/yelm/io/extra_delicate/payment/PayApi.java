package yelm.io.extra_delicate.payment;

import java.math.BigDecimal;

import io.reactivex.Observable;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.payment.models.PayRequestArgs;
import yelm.io.extra_delicate.payment.models.Post3dsRequestArgs;
import yelm.io.extra_delicate.payment.models.Transaction;
import yelm.io.extra_delicate.payment.response.PayApiResponse;
import yelm.io.extra_delicate.support_stuff.Logging;

public class PayApi {

    private static final String CONTENT_TYPE = "application/json";

    public static Observable<Transaction> charge(String cardCryptogramPacket, String cardHolderName, BigDecimal amount, String order, String type) {

        // Параметры:
        PayRequestArgs args = new PayRequestArgs();
        args.setAmount(amount.toString());  // Сумма платежа (Обязательный)
        args.setCurrency("RUB"); // Валюта (Обязательный)
        args.setName(cardHolderName); // Имя держателя карты в латинице (Обязательный для всех платежей кроме Apple Pay и Google Pay)
        args.setCardCryptogramPacket(cardCryptogramPacket); // Криптограмма платежных данных (Обязательный)
        args.setInvoiceId(""); // Номер счета или заказа в вашей системе (необязательный)
        args.setDescription(""); // Описание оплаты в свободной форме (необязательный)
        args.setAccountId(SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_LOGIN)); // Идентификатор пользователя в вашей системе (необязательный)
        args.setJsonData(order); // Любые другие данные, которые будут связаны с транзакцией (необязательный)
        args.setType(type); // Payment Type

        return PayApiFactory.getPayMethods()
                .charge(CONTENT_TYPE, args)
                .flatMap(PayApiResponse::handleError)
                .map((PayApiResponse<Transaction> transactionPayApiResponse) -> {
                    return transactionPayApiResponse.getData();
                });
    }

    public static Observable<Transaction> auth(String cardCryptogramPacket, String cardHolderName, BigDecimal amount, String order, String type) {

        // Параметры:
        PayRequestArgs args = new PayRequestArgs();
        args.setAmount(amount.toString());  // Сумма платежа (Обязательный)
        args.setCurrency(SharedPreferencesSetting.getDataString(SharedPreferencesSetting.CURRENCY)); // Валюта (Обязательный)
        args.setName(cardHolderName); // Имя держателя карты в латинице (Обязательный для всех платежей кроме Apple Pay и Google Pay)
        args.setCardCryptogramPacket(cardCryptogramPacket); // Криптограмма платежных данных (Обязательный)

        args.setInvoiceId(""); // Номер счета или заказа в вашей системе (необязательный)
        args.setDescription(""); // Описание оплаты в свободной форме (необязательный)
        args.setAccountId(SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_LOGIN)); // Идентификатор пользователя в вашей системе (необязательный)
        args.setJsonData(order); // Любые другие данные, которые будут связаны с транзакцией (необязательный)
        args.setType(type); // Payment Type

        Logging.logDebug("cardCryptogramPacket");

        return PayApiFactory.getPayMethods()
                .auth(CONTENT_TYPE, args)
                .flatMap(transactionPayApiResponse -> transactionPayApiResponse.handleError())
                .map((PayApiResponse<Transaction> transactionPayApiResponse1) -> {
                    return transactionPayApiResponse1.getData();
                });
    }

    public static Observable<Transaction> post3ds(String transactionId, String paRes, String type ) {
        Logging.logDebug( "post3ds");
        Post3dsRequestArgs args = new Post3dsRequestArgs();
        args.setTransactionId(transactionId);
        args.setPaRes(paRes);
        args.setType(type);
        return PayApiFactory.getPayMethods()
                .post3ds(CONTENT_TYPE, args)
                .flatMap(transactionPayApiResponse1 -> {
                    return transactionPayApiResponse1.handleError();
                })
                .map(transactionPayApiResponse -> {
                    return transactionPayApiResponse.getData();
                });
    }
}