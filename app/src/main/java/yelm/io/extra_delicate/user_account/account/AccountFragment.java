package yelm.io.extra_delicate.user_account.account;

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
import android.widget.ImageView;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.chat.controller.ChatActivity;
import yelm.io.extra_delicate.databinding.FragmentAccountBinding;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.order.controller.OrderActivity;
import yelm.io.extra_delicate.user_account.HostLogin;
import yelm.io.extra_delicate.user_account.model.UserAuth;

public class AccountFragment extends Fragment {
    public AccountFragment() {
        // Required empty public constructor
    }

    private FragmentAccountBinding binding = null;
    private HostLogin hostLogin = null;

    public static AccountFragment newInstance() {
        AccountFragment fragment = new AccountFragment();

        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCustomColor();
        binding.myOffers.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), ChatActivity.class));
        });
        binding.edit.setOnClickListener(v -> {
            showDialogChangeName();
        });
    }

    private void setCustomColor() {
        binding.back.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.back.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.signOut.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.signOut.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.progress.getIndeterminateDrawable().setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)), PorterDuff.Mode.SRC_IN);
    }


    private void showDialogChangeName() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireActivity(), R.style.AlertDialogTheme);
        View view = LayoutInflater.from(requireActivity())
                .inflate(R.layout.layout_dialog_change_user_name,
                        requireActivity().findViewById(R.id.layoutDialogContainer));
        builder.setView(view);

        TextView save = view.findViewById(R.id.save);
        save.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        save.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));

        android.app.AlertDialog alertDialog = builder.create();
        save.setOnClickListener(v -> {
            SharedPreferencesSetting.setData(SharedPreferencesSetting.USER_NAME, "");

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


}