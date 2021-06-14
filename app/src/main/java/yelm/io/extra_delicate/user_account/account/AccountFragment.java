package yelm.io.extra_delicate.user_account.account;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.chat.controller.ChatActivity;
import yelm.io.extra_delicate.databinding.FragmentAccountBinding;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.rest.client.RetrofitClient;
import yelm.io.extra_delicate.rest.rest_api.RestAPI;
import yelm.io.extra_delicate.support_stuff.Logging;
import yelm.io.extra_delicate.user_account.HostLogin;
import yelm.io.extra_delicate.user_account.model.UserAuth;

public class AccountFragment extends Fragment {
    public AccountFragment() {
        // Required empty public constructor
    }

    private FragmentAccountBinding binding = null;
    private HostLogin hostLogin = null;

    public static AccountFragment newInstance() {
        return new AccountFragment();
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCustomColor();

        binding.userName.setText(SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_NAME));

        binding.bonus.setText(String.format("%s %s", SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_BALANCE), SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));

        binding.myOffers.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), ChatActivity.class));
        });
        binding.edit.setOnClickListener(v -> {
            showDialogChangeName();
        });
        binding.signOut.setOnClickListener(v -> {
            SharedPreferencesSetting.setData(SharedPreferencesSetting.USER_NAME, "");
            requireActivity().finish();
        });
        binding.back.setOnClickListener(v -> hostLogin.back());

        setNotificationSettings();
    }

    private void setNotificationSettings() {
        String notification = SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_NOTIFICATION);
        if (notification.equals("1")) {
            binding.notificationSwitch.setChecked(true);
        }
        if (notification.equals("0")) {
            binding.notificationSwitch.setChecked(false);
        }

        binding.notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setUserData(SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_NAME), "1");
            } else {
                setUserData(SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_NAME), "0");
            }
        });
    }

    private void setUserData(String name, String notification) {
        showLoading();
        RetrofitClient.
                getClient(RestAPI.URL_API_MAIN).
                create(RestAPI.class).
                setUserData(
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_LOGIN),
                        RestAPI.PLATFORM_NUMBER,
                        name,
                        notification).
                enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NotNull Call<ResponseBody> call, @NotNull final Response<ResponseBody> response) {
                        hideLoading();
                        if (response.isSuccessful()) {
                            Logging.logDebug("Method userAuth() - response is not successful." +
                                    "Code: " + response.code() + "Message: " + response.message());
                            if (response.code() == 200) {
                                hostLogin.showToast(R.string.dataUpdatedSuccessfully);
                                SharedPreferencesSetting.setData(SharedPreferencesSetting.USER_NOTIFICATION,
                                        notification);
                                SharedPreferencesSetting.setData(SharedPreferencesSetting.USER_NAME,
                                        name);
                                binding.userName.setText(name);
                            } else {
                                hostLogin.showToast(R.string.serverError);
                            }
                        } else {
                            hostLogin.showToast(R.string.serverError);
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                        Logging.logError("Method userAuth() - failure: " + t.toString());
                        hideLoading();
                        hostLogin.showToast(R.string.serverError);
                    }
                });
    }

    private void setCustomColor() {
        binding.back.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.back.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.signOut.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.signOut.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.progress.getIndeterminateDrawable().setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)), PorterDuff.Mode.SRC_IN);
    }

    private void hideLoading() {
        binding.progress.setVisibility(View.GONE);
    }

    private void showLoading() {
        binding.progress.setVisibility(View.VISIBLE);
    }

    private void showDialogChangeName() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireActivity(), R.style.AlertDialogTheme);
        View view = LayoutInflater.from(requireActivity())
                .inflate(R.layout.layout_dialog_change_user_name,
                        requireActivity().findViewById(R.id.layoutDialogContainer));
        builder.setView(view);

        TextView save = view.findViewById(R.id.save);
        EditText userName = view.findViewById(R.id.userName);
        save.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        save.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));

        android.app.AlertDialog alertDialog = builder.create();
        save.setOnClickListener(v -> {
            setUserData(userName.getText().toString().trim(),
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.USER_NOTIFICATION));
            alertDialog.dismiss();
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
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

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        if (getActivity() instanceof HostLogin) {
            hostLogin = (HostLogin) getActivity();
        } else {
            throw new RuntimeException(requireActivity().toString() + " must implement HostLogin interface");
        }
    }


}