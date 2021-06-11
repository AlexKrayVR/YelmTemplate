package yelm.io.extra_delicate.user_account.verification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.databinding.FragmentVerificationBinding;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.support_stuff.Logging;
import yelm.io.extra_delicate.user_account.HostLogin;
import yelm.io.extra_delicate.user_account.model.UserAuth;

public class VerificationFragment extends Fragment implements VerificationView {

    public VerificationFragment() {
    }

    private HostLogin hostLogin = null;
    private FragmentVerificationBinding binding;
    private final char[] codeArray = new char[]{' ', ' ', ' ', ' '};
    private VerificationPresenter presenter;
    private BroadcastReceiver smsReceiver;

    public static VerificationFragment newInstance(UserAuth userAuth) {
        VerificationFragment fragment = new VerificationFragment();
        Bundle args = new Bundle();
        args.putSerializable(UserAuth.class.getName(), userAuth);
        fragment.setArguments(args);
        return fragment;
    }

    public void addTextChangeListener(EditText editText, int index) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    codeArray[index] = ' ';
                } else {
                    codeArray[index] = s.toString().toCharArray()[0];
                }
                if (isCodeFulled()) {
                    presenter.compareSHA2(Arrays.toString(codeArray).replaceAll("\\D", ""),
                            ((UserAuth) getArguments().getSerializable(UserAuth.class.getName())).getHash());
                }
            }
        });
    }

    private boolean isCodeFulled() {
        for (char item : codeArray) {
            if (item == ' ') {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter = new VerificationPresenter(this);
        setCustomColor();
        bindAction();
        initSMSReceiver();
    }

    private void initSMSReceiver() {
        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.getAction() != null && "android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
                    Logging.logDebug("message received");
                    presenter.receiveSMS((Object[]) intent.getExtras().get("pdus"));
                }
            }
        };
    }


    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentVerificationBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    private void bindAction() {
        binding.back.setOnClickListener(v -> hostLogin.back());
        binding.enter.setOnClickListener(v -> {
            if (isCodeFulled()) {
                presenter.compareSHA2(Arrays.toString(codeArray).replaceAll("\\D", ""),
                        ((UserAuth) getArguments().getSerializable(UserAuth.class.getName())).getHash());
            }else {
                hostLogin.showToast(R.string.enterCode);
            }
        });

        addTextChangeListener(binding.first, 0);
        addTextChangeListener(binding.second, 1);
        addTextChangeListener(binding.third, 2);
        addTextChangeListener(binding.fourth, 3);
    }

    private void setCustomColor() {
        binding.back.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.back.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.enter.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.enter.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.progress.getIndeterminateDrawable().setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)), PorterDuff.Mode.SRC_IN);
    }

    public void hideLoading() {
        binding.progress.setVisibility(View.GONE);
    }

    public void showLoading() {
        binding.progress.setVisibility(View.VISIBLE);
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
    public void onStart() {
        super.onStart();
        requireActivity().registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        hostLogin = null;
        requireActivity().unregisterReceiver(smsReceiver);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        presenter.detachView();
        super.onDestroyView();
    }

    @Override
    public void loginPhoneError(int error) {
        hostLogin.showToast(error);
    }

    @Override
    public void codeIsCorrect() {

        SharedPreferencesSetting.setData(SharedPreferencesSetting.USER_BALANCE,
                ((UserAuth) getArguments().getSerializable(UserAuth.class.getName())).getUser().getInfo().getBalance());

        SharedPreferencesSetting.setData(SharedPreferencesSetting.USER_NAME,
                ((UserAuth) getArguments().getSerializable(UserAuth.class.getName())).getUser().getInfo().getName());

        hostLogin.openAccountFragment();
    }

    @Override
    public void openAccountFragment() {

    }

    @Override
    public void codeFromSMS(String code) {
        presenter.compareSHA2(code,
                ((UserAuth) getArguments().getSerializable(UserAuth.class.getName())).getHash());
    }
}