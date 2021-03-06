package yelm.io.extra_delicate.search;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import yelm.io.extra_delicate.item.controller.ItemActivity;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.main.model.Item;
import yelm.io.extra_delicate.rest.query.RestMethods;
import yelm.io.extra_delicate.support_stuff.Logging;
import yelm.io.extra_delicate.databinding.ProductItemSearcheableBinding;

public class SearchProductAdapter extends RecyclerView.Adapter<SearchProductAdapter.ProductHolder> implements Filterable {

    private Context context;
    private List<Item> products;
    private List<Item> productsSort;

    public SearchProductAdapter(Context context, List<Item> products) {
        this.context = context;
        this.products = products;
        productsSort = new ArrayList<>(products);
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    Filter filter = new Filter() {
        //run back
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            List<Item> filtered = new ArrayList<>();
            if (charSequence.toString().isEmpty()) {
                filtered.addAll(products);
            } else {
                for (Item product : products) {
                    if (product.getName().toLowerCase().contains(charSequence.toString().toLowerCase())) {
                        Logging.logDebug("Filter - request string: " + product.getName().toLowerCase());
                        filtered.add(product);
                    }
                }
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values = filtered;
            return filterResults;
        }

        //run ui
        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            productsSort.clear();
            productsSort.addAll((Collection<? extends Item>) filterResults.values);
            notifyDataSetChanged();
        }
    };

    @Override
    public void onBindViewHolder(@NonNull final SearchProductAdapter.ProductHolder holder, final int position) {
        Item current = productsSort.get(position);
        BigDecimal bd = new BigDecimal(current.getPrice());
        if (!current.getDiscount().equals("0")) {
            bd = new BigDecimal(current.getDiscount()).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            bd = bd.multiply(new BigDecimal(current.getPrice())).setScale(2, BigDecimal.ROUND_HALF_UP);
            bd = new BigDecimal(current.getPrice()).subtract(bd);
            //trim zeros if after comma there are only zeros: 45.00 -> 45
            if (bd.compareTo(new BigDecimal(String.valueOf(bd.setScale(0, BigDecimal.ROUND_HALF_UP)))) == 0) {
                bd = bd.setScale(0, BigDecimal.ROUND_HALF_UP);
            }
        }

        if (!current.getDiscount().equals("0")) {
            holder.binding.discountProcent.setText(String.format("- %s %%", current.getDiscount()));
            holder.binding.discountProcent.setVisibility(View.VISIBLE);
        } else {
            holder.binding.discountProcent.setVisibility(View.GONE);
        }

        holder.binding.priceFinal.setText(String.format("%s %s", bd.toString(),
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));

        holder.binding.description.setText(current.getName());
        holder.binding.weight.setText(String.format("%s / %s", current.getUnitType(), current.getType()));

        holder.binding.containerProduct.setOnClickListener(v -> {
            RestMethods.sendStatistic("open_item_search");
            Intent intent = new Intent(context, ItemActivity.class);
            intent.putExtra("item", current);
            context.startActivity(intent);
        });
        holder.binding.imageHolder.setAlpha(0f);
        Picasso.get()
                .load(current.getPreviewImage())
                .noPlaceholder()
                .centerCrop()
                .resize(300, 300)
                .into(holder.binding.imageHolder, new Callback() {
                    @Override
                    public void onSuccess() {
                        holder.binding.imageHolder.animate().setDuration(300).alpha(1f).start();
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });
    }

    @NonNull
    @Override
    public SearchProductAdapter.ProductHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ProductHolder productHolder = new ProductHolder(ProductItemSearcheableBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        productHolder.binding.discountProcent.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        return productHolder;
    }

    @Override
    public int getItemCount() {
        return productsSort == null ? 0 : productsSort.size();
    }

    public static class ProductHolder extends RecyclerView.ViewHolder {
        private ProductItemSearcheableBinding binding;

        public ProductHolder(ProductItemSearcheableBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}