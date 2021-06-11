package yelm.io.extra_delicate.user_account.verification;

import android.telephony.SmsMessage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.support_stuff.Logging;

public class VerificationPresenter {

    private VerificationView view;

    public VerificationPresenter(VerificationView view) {
        this.view = view;
    }

    public void detachView() {
        view = null;
    }


    /**
     * get SHA2 from our sms message code
     */
    private String getSHA2(String code) {
        final String MD5 = "SHA-256";
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(code.getBytes());
            byte[] messageDigest = digest.digest();
            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * compare sha2 of sms message code with hash from server respond
     * send result to view
     */
    void compareSHA2(String code, String hash) {
        Logging.logDebug("code: " + code);
        String sha2 = getSHA2(code);
        Logging.logDebug("sha2: " + sha2);
        Logging.logDebug("hash: " + hash);
        if (hash.equals(sha2)) {
            view.codeIsCorrect();
        } else {
            view.loginPhoneError(R.string.codeIncorrect);
        }
    }

    /**
     * getting code from sms
     * @param pdus - sms object
     */
    void receiveSMS(Object[] pdus) {
        SmsMessage[] messages = new SmsMessage[pdus.length];
        for (int i = 0; i < pdus.length; i++) {
            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
        }
        for (SmsMessage sms : messages) {
            Logging.logDebug("address: " + sms.getOriginatingAddress());
            String body = sms.getMessageBody();
            Logging.logDebug("body: " + body);
            String code = body.substring(body.length() - 4, body.length());
            Logging.logDebug("code: " + code);
            view.codeFromSMS(code);
        }
    }
}
