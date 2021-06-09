package yelm.io.extra_delicate.user_login;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.List;

import yelm.io.extra_delicate.databinding.ActivityLoginHostBinding;
import yelm.io.extra_delicate.item.controller.ItemActivity;
import yelm.io.extra_delicate.user_login.model.UserAuth;

public class LoginHostActivity extends AppCompatActivity implements HostLogin {

    private ActivityLoginHostBinding binding;
    Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        openLoginFragment();
    }

    @Override
    public void onBackPressed() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments.size() == 1) {
            finish();
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }


    @Override
    public void openLoginFragment() {
        LoginFragment fragment = LoginFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(binding.container.getId(), fragment).commit();
    }

    @Override
    public void openVerificationFragment(UserAuth userAuth) {
        VerificationFragment fragment = VerificationFragment.newInstance(userAuth);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(binding.container.getId(), fragment).commit();
    }

    @Override
    public void openUserFragment() {

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


}