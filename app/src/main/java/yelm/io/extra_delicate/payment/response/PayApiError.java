package yelm.io.extra_delicate.payment.response;

import yelm.io.extra_delicate.support_stuff.Logging;

public class PayApiError extends Throwable {

    private String message;

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public PayApiError(String message) {
        Logging.logDebug("message"+ message);
        this.message = message;
    }
}
