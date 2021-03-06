package yelm.io.extra_delicate.user_address.controller;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.database.Common;
import yelm.io.extra_delicate.database.user_addresses.UserAddress;
import yelm.io.extra_delicate.databinding.AdressesBottomSheepDialogBinding;

import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.support_stuff.Logging;
import yelm.io.extra_delicate.user_address.adapter.UserAddressesAdapter;

public class AddressesBottomSheet extends BottomSheetDialogFragment {

    private AddressesBottomSheetListener listener;
    private AdressesBottomSheepDialogBinding binding;
    private UserAddressesAdapter userAddressesAdapter;
    private static final int USER_ADDRESS_CHOOSE_REQUEST_CODE = 464;

    public void setListener(AddressesBottomSheetListener listener) {
        this.listener = listener;
    }

    public interface AddressesBottomSheetListener {
        void selectedAddress(UserAddress address);
    }

    public AddressesBottomSheet() {
    }

    @Override
    public int getTheme() {
        return R.style.AppBottomSheetDialogTheme;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AdressesBottomSheepDialogBinding.inflate(inflater, container, false);
        binding.addressesDone.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.addressesDone.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));


        binding.addressesDone.setOnClickListener(v -> this.dismiss());
        binding.addNewAddress.setOnClickListener(v -> startActivityForResult(new Intent(getContext(), AddressChooseActivity.class), USER_ADDRESS_CHOOSE_REQUEST_CODE));

        binding.recyclerUserAddresses.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        userAddressesAdapter = new UserAddressesAdapter(getContext(), Common.userAddressesRepository.getUserAddressesList());
        userAddressesAdapter.setListener(() -> {
            Logging.logDebug("addressChangeListener.onAddressChange()");
            for (UserAddress current : Common.userAddressesRepository.getUserAddressesList()) {
                if (current.isChecked) {
                    listener.selectedAddress(current);
                    break;
                }
            }
            userAddressesAdapter.setNewUserAddressList(Common.userAddressesRepository.getUserAddressesList());
        });
        binding.recyclerUserAddresses.setAdapter(userAddressesAdapter);
        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (AddressesBottomSheet.AddressesBottomSheetListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement BottomSheetShopListener");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (data == null) {
            return;
        }
        if (requestCode == USER_ADDRESS_CHOOSE_REQUEST_CODE) {
            Logging.logDebug( "USER_ADDRESS_CHOOSE_REQUEST_CODE");
            for (UserAddress current : Common.userAddressesRepository.getUserAddressesList()) {
                if (current.isChecked) {
                    listener.selectedAddress(current);
                    break;
                }
            }
            userAddressesAdapter.setNewUserAddressList(Common.userAddressesRepository.getUserAddressesList());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}