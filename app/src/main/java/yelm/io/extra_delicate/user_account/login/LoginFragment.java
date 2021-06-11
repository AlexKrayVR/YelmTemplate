package yelm.io.extra_delicate.user_account.login;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.databinding.FragmentLoginBinding;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.rest.client.RetrofitClient;
import yelm.io.extra_delicate.rest.rest_api.RestAPI;
import yelm.io.extra_delicate.support_stuff.Logging;
import yelm.io.extra_delicate.user_account.HostLogin;
import yelm.io.extra_delicate.user_account.model.UserAuth;


public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding = null;
    private HostLogin hostLogin = null;

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    public LoginFragment() {
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCustomColor();

        Picasso.get()
                .load(R.drawable.app_icon)
                .noPlaceholder()
                .centerCrop()
                .resize(300, 300)
                .into(binding.icon);

        binding.login.setOnClickListener(v -> {
            if (binding.phoneNumber.getText().toString().trim().length() != 11) {
                hostLogin.showToast(R.string.enterCorrectPhoneNumber);
            } else {
                userAuth(binding.phoneNumber.getText().toString().trim());
            }
        });
        binding.back.setOnClickListener(v -> hostLogin.back());
    }

    private void userAuth(String phone) {
        showLoading();
        RetrofitClient.
                getClient(RestAPI.URL_API_MAIN).
                create(RestAPI.class).
                auth(
                        RestAPI.PLATFORM_NUMBER,
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_LOGIN),
                        phone).
                enqueue(new Callback<UserAuth>() {
                    @Override
                    public void onResponse(@NotNull Call<UserAuth> call, @NotNull final Response<UserAuth> response) {
                        hideLoading();
                        if (!response.isSuccessful()) {
                            Logging.logError("Method userAuth() - response is not successful." +
                                    "Code: " + response.code() + "Message: " + response.message());

                        } else {
                            hostLogin.openVerificationFragment(response.body());
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<UserAuth> call, @NotNull Throwable t) {
                        Logging.logError("Method userAuth() - failure: " + t.toString());
                        hideLoading();
                    }
                });
    }

    private void hideLoading() {
        binding.progress.setVisibility(View.GONE);
    }

    private void showLoading() {
        binding.progress.setVisibility(View.VISIBLE);
    }

    private void setCustomColor() {
        binding.back.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.back.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.login.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.login.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.progress.getIndeterminateDrawable().setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)), PorterDuff.Mode.SRC_IN);
    }


    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        if (getActivity() instanceof HostLogin) {
            hostLogin = (HostLogin) getActivity();
        } else {
            throw new RuntimeException(requireActivity().toString() + " must implement HostLogin interface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        hostLogin = null;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}