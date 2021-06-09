package yelm.io.extra_delicate.user_login;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.databinding.FragmentLoginBinding;
import yelm.io.extra_delicate.databinding.FragmentVerificationBinding;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.user_login.model.UserAuth;

public class VerificationFragment extends Fragment {

    public VerificationFragment() {
    }

    private UserAuth userAuth;
    private HostLogin hostLogin = null;
    private FragmentVerificationBinding binding;

    public static VerificationFragment newInstance(UserAuth userAuth) {
        VerificationFragment fragment = new VerificationFragment();
        Bundle args = new Bundle();
        args.putSerializable(UserAuth.class.getName(), userAuth);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userAuth = (UserAuth) getArguments().getSerializable(UserAuth.class.getName());
        }
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.back.setOnClickListener(v -> hostLogin.back());
        setCustomColor();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentVerificationBinding.inflate(getLayoutInflater(), container, false);

        return binding.getRoot();
    }

    private void setCustomColor() {
        binding.back.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.back.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.verification.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.verification.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.progress.getIndeterminateDrawable().setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)), PorterDuff.Mode.SRC_IN);
    }


    private void hideLoading() {
        binding.progress.setVisibility(View.GONE);
    }

    private void showLoading() {
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