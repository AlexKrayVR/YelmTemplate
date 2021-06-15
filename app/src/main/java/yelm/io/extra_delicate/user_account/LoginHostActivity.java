package yelm.io.extra_delicate.user_account;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import java.util.List;

import yelm.io.extra_delicate.databinding.ActivityLoginHostBinding;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.user_account.account.AccountFragment;
import yelm.io.extra_delicate.user_account.login.LoginFragment;
import yelm.io.extra_delicate.user_account.model.UserAuth;
import yelm.io.extra_delicate.user_account.verification.VerificationFragment;
import yelm.io.extra_delicate.user_account.web.WebFragment;

public class LoginHostActivity extends AppCompatActivity implements HostLogin {

    private ActivityLoginHostBinding binding;
    Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_NAME).isEmpty()) {
            openLoginFragment();
            receiveSMSPermission();
        } else {
            openAccountFragment();
        }
    }

    void receiveSMSPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, 5);
        }
    }

    @Override
    public void onBackPressed() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fr : fragments) {
            if (fr.getTag() != null && fr.getTag().equals("account")) {
                finish();
            }
        }

        if (fragments.size() == 1) {
            finish();
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void openWebFragment(String url) {
        WebFragment fragment = WebFragment.newInstance(url);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.
                add(binding.container.getId(), fragment)
                .addToBackStack("web")
                .commit();
    }


    @Override
    public void openLoginFragment() {
        LoginFragment fragment = LoginFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.
                add(binding.container.getId(), fragment)
                .addToBackStack("login")
                .commit();
    }

    @Override
    public void openVerificationFragment(UserAuth userAuth) {
        VerificationFragment fragment = VerificationFragment.newInstance(userAuth);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(binding.container.getId(), fragment)
                .addToBackStack("verification")
                .commit();
    }

    @Override
    public void openAccountFragment() {
        AccountFragment fragment = AccountFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.
                add(binding.container.getId(), fragment)
                .addToBackStack("account")
                .commit();
    }

    @Override
    public void showToast(int message) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public void back() {
        onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}