package yelm.io.extra_delicate.user_login;

import yelm.io.extra_delicate.user_login.model.UserAuth;

public interface HostLogin {

    void openLoginFragment();

    void openVerificationFragment(UserAuth userAuth);

    void openUserFragment();

    void showToast(int message);
    void back();

}
