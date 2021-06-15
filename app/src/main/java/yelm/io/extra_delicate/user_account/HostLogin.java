package yelm.io.extra_delicate.user_account;

import yelm.io.extra_delicate.user_account.model.UserAuth;

public interface HostLogin {

    void openLoginFragment();

    void openWebFragment(String url);

    void openVerificationFragment(UserAuth userAuth);

    void openAccountFragment();

    void showToast(int message);

    void back();

}
