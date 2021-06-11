package yelm.io.extra_delicate.user_account.verification;

public interface VerificationView {
        void showLoading();
    void hideLoading();
    void loginPhoneError(int error);
    void codeIsCorrect();
    void openAccountFragment();
    void codeFromSMS(String code);

}
