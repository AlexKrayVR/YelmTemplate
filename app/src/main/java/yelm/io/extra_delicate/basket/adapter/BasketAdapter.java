package yelm.io.extra_delicate.basket.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.math.BigDecimal;
import java.util.List;

import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.database.basket_new.BasketCart;
import yelm.io.extra_delicate.database.Common;
import yelm.io.extra_delicate.databinding.BasketCartItemBinding;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.main.model.Modifier;

public class BasketAdapter extends RecyclerView.Adapter<BasketAdapter.BasketHolder> {

    private Context context;
    private List<BasketCart> basket;

    public BasketAdapter(Context context, List<BasketCart> basket) {
        this.context = context;
        this.basket = basket;
    }

    @Override
    public void onBindViewHolder(@NonNull final BasketAdapter.BasketHolder holder, final int position) {

        BasketCart current = basket.get(position);
        holder.binding.description.setText(String.format("%s\n%s %s / %s %s",
                current.name,
                current.finalPrice,
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN),
                current.quantityType,
                current.type));

        holder.binding.countProducts.setText(current.count);
        BigDecimal weight = new BigDecimal(current.count).multiply(new BigDecimal(current.quantityType));
        holder.binding.weight.setText(String.format("%s %s", weight.toString(), current.type));

        if (current.modifier.size() != 0) {
            holder.binding.modifiers.setVisibility(View.VISIBLE);
            StringBuilder modifiers = getModifiers(current.modifier);
            holder.binding.modifiers.setText(modifiers.toString());
        }

        Picasso.get()
                .load(current.imageUrl)
                .noPlaceholder()
                .centerCrop()
                .resize(300, 300)
                .into(holder.binding.imageHolder);

        BigDecimal currentStartFinal = new BigDecimal(current.finalPrice);
        for (Modifier modifier : current.modifier) {
            currentStartFinal = currentStartFinal.add(new BigDecimal(modifier.getValue()));
        }
        currentStartFinal = currentStartFinal.multiply(new BigDecimal(current.count));
        holder.binding.priceFinal.setText(String.format("%s %s", currentStartFinal, SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));

        holder.binding.addProduct.setOnClickListener(view -> {
            BigDecimal tempBD = new BigDecimal(current.count);
            current.count = tempBD.add(new BigDecimal("1")).toString();
            Common.basketCartRepository.updateBasketCart(current);
        });

        holder.binding.removeProduct.setOnClickListener(view -> {
            if (current.count.equals("1")) {
                Common.basketCartRepository.deleteBasketCart(current);
            } else {
                BigDecimal tempBD = new BigDecimal(current.count);
                current.count = tempBD.subtract(new BigDecimal("1")).toString();
                Common.basketCartRepository.updateBasketCart(current);
            }
        });

        if (new BigDecimal(current.count).compareTo(new BigDecimal(current.quantity)) >= 0) {
            holder.binding.addProduct.setEnabled(false);
            holder.binding.addProduct.getBackground().setTint(context.getResources().getColor(R.color.colorButtonOrderingDisable));
        }

        if (new BigDecimal(current.count).compareTo(new BigDecimal(current.quantity)) > 0) {
            holder.binding.textProductIsOver.setVisibility(View.VISIBLE);
            holder.binding.textProductIsOver.setText(String.format("%s: %s %s", context.getText(R.string.basketActivityProductIsOver), current.quantity, context.getText(R.string.basketActivityPC)));
            holder.binding.addProduct.setEnabled(false);
            holder.binding.addProduct.getBackground().setTint(context.getResources().getColor(R.color.colorButtonOrderingDisable));
        }
    }

    private StringBuilder getModifiers(List<Modifier> modifiersList) {
        StringBuilder modifiersString = new StringBuilder();
        for (Modifier modifier : modifiersList) {
            modifiersString = modifiersString.length() > 0 ?
                    modifiersString.append('\n').append(modifier.getName()) : modifiersString.append(modifier.getName());
        }
        return modifiersString;
    }

    @NonNull
    @Override
    public BasketAdapter.BasketHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BasketHolder basketHolder = new BasketHolder(BasketCartItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        basketHolder.binding.removeProduct.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        basketHolder.binding.addProduct.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        basketHolder.binding.removeProduct.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        basketHolder.binding.addProduct.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        return basketHolder;
    }

    @Override
    public int getItemCount() {
        return basket == null ? 0 : basket.size();
    }

    public static class BasketHolder extends RecyclerView.ViewHolder {
        private BasketCartItemBinding binding;

        public BasketHolder(BasketCartItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
