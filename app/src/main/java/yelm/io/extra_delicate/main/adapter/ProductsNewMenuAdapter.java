package yelm.io.extra_delicate.main.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.squareup.picasso.Picasso;

import java.math.BigDecimal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import yelm.io.extra_delicate.item.controller.ItemActivity;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.rest.query.RestMethods;
import yelm.io.extra_delicate.support_stuff.Logging;
import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.database.basket_new.BasketCart;
import yelm.io.extra_delicate.database.Common;
import yelm.io.extra_delicate.databinding.NewMenuProductItemBinding;
import yelm.io.extra_delicate.main.model.Item;
import yelm.io.extra_delicate.main.model.Modifier;
import yelm.io.extra_delicate.item.adapter.ProductModifierAdapter;

public class ProductsNewMenuAdapter extends RecyclerView.Adapter<ProductsNewMenuAdapter.ProductHolder> implements Filterable {
    private Context context;
    private List<Item> products;
    private List<Item> productsSort;

    public ProductsNewMenuAdapter(Context context, List<Item> products) {
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

    @NonNull
    @Override
    public ProductsNewMenuAdapter.ProductHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ProductHolder productHolder = new ProductHolder(NewMenuProductItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        productHolder.binding.layoutAddRemove.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        productHolder.binding.priceFinal.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        productHolder.binding.removeProduct.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        productHolder.binding.addProduct.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        productHolder.binding.discountProcent.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        return productHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ProductsNewMenuAdapter.ProductHolder holder, final int position) {
        Item current = productsSort.get(position);
        //set count of item in basket into layout
        List<BasketCart> listBasketCartByItemID = Common.basketCartRepository.getListBasketCartByItemID(current.getId());
        if (listBasketCartByItemID.size() != 0) {
            BigInteger countOfAllProducts = new BigInteger("0");
            for (BasketCart basketCart : listBasketCartByItemID) {
                countOfAllProducts = countOfAllProducts.add(new BigInteger(basketCart.count));
            }
            holder.binding.countItemInCart.setText(String.format("%s", countOfAllProducts));
            holder.binding.removeProduct.setVisibility(View.VISIBLE);
            holder.binding.countItemsLayout.setVisibility(View.VISIBLE);
            if (countOfAllProducts.compareTo(new BigInteger(current.getQuantity())) >= 0) {
                holder.binding.addProduct.setVisibility(View.GONE);
            }
        }

        holder.binding.imageHolder.setOnClickListener(v -> {
            RestMethods.sendStatistic("open_item");
            Intent intent = new Intent(context, ItemActivity.class);
            intent.putExtra("item", current);
            context.startActivity(intent);
        });

        //calculate final price depending on the discount
        BigDecimal bd = new BigDecimal(current.getPrice());
        if (current.getDiscount().equals("0")) {
            holder.binding.priceFinal.setText(String.format("%s %s", bd.toString(),
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
            holder.binding.priceStart.setVisibility(View.GONE);
            holder.binding.discountProcent.setVisibility(View.GONE);
        } else {
            holder.binding.discountProcent.setVisibility(View.VISIBLE);
            holder.binding.discountProcent.setText(String.format("- %s %%", current.getDiscount()));
            bd = new BigDecimal(current.getDiscount()).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            bd = bd.multiply(new BigDecimal(current.getPrice())).setScale(2, BigDecimal.ROUND_HALF_UP);
            bd = new BigDecimal(current.getPrice()).subtract(bd);
            //trim zeros if after comma there are only zeros: 45.00 -> 45
            if (bd.compareTo(new BigDecimal(String.valueOf(bd.setScale(0, BigDecimal.ROUND_HALF_UP)))) == 0) {
                bd = bd.setScale(0, BigDecimal.ROUND_HALF_UP);
            }
            holder.binding.priceFinal.setText(String.format("%s %s", bd.toString(),
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
            holder.binding.priceStart.setText(String.format("%s %s", current.getPrice(),
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
            holder.binding.priceStart.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
        }

        holder.binding.description.setText(current.getName());
        holder.binding.weight.setText(String.format("%s %s", current.getUnitType(), current.getType()));

        //add product into basket
        BigDecimal finalBd = bd;
        holder.binding.addProduct.setOnClickListener(v -> {
            if (current.getModifier().size() != 0) {
                showBottomSheetDialog(holder, current, finalBd);
            } else {
                holder.binding.countItemsLayout.setVisibility(View.VISIBLE);
                List<BasketCart> listCartsByID = Common.basketCartRepository.getListBasketCartByItemID(current.getId());

                BigDecimal countOfAllProducts = new BigDecimal("0");
                for (BasketCart basketCart : listCartsByID) {
                    countOfAllProducts = countOfAllProducts.add(new BigDecimal(basketCart.count));
                }
                if (countOfAllProducts.add(new BigDecimal("1")).compareTo(new BigDecimal(current.getQuantity())) >= 0) {
                    holder.binding.addProduct.setVisibility(View.GONE);
                }
                if (countOfAllProducts.compareTo(new BigDecimal(current.getQuantity())) >= 0) {
                    showToast(context.getString(R.string.productsNotAvailable) +
                            " " + listCartsByID.get(0).quantity + " " + context.getString(R.string.basketActivityPC));
                    return;
                }

                if (listCartsByID.size() != 0) {
                    for (BasketCart basketCart : listCartsByID) {
                        if (basketCart.modifier.equals(current.getModifier())) {
                            basketCart.count = new BigDecimal(basketCart.count).add(new BigDecimal("1")).toString();
                            holder.binding.countItemInCart.setText(String.format("%s", countOfAllProducts.add(new BigDecimal("1"))));
                            Common.basketCartRepository.updateBasketCart(basketCart);
                            Logging.logDebug("Method add BasketCart to Basket. No modifiers - listCartsByID !=null:  " + basketCart.toString());
                            return;
                        }
                    }
                }
                holder.binding.removeProduct.setVisibility(View.VISIBLE);
                holder.binding.countItemInCart.setText("1");
                BasketCart cartItem = new BasketCart();
                cartItem.itemID = current.getId();
                cartItem.name = current.getName();
                cartItem.discount = current.getDiscount();
                cartItem.startPrice = current.getPrice();
                cartItem.finalPrice = finalBd.toString();
                cartItem.type = current.getType();
                cartItem.count = "1";
                cartItem.imageUrl = current.getPreviewImage();
                cartItem.quantity = current.getQuantity();
                cartItem.discount = current.getDiscount();
                cartItem.modifier = current.getModifier();
                cartItem.isPromo = false;
                cartItem.isExist = true;
                cartItem.quantityType = current.getUnitType();
                Common.basketCartRepository.insertToBasketCart(cartItem);
                Logging.logDebug("Method add BasketCart to Basket. No modifiers - listCartsByID == null:  " + cartItem.toString());
            }
        });

        //remove product from basket
        holder.binding.removeProduct.setOnClickListener(v -> {
            List<BasketCart> listCartsByID = Common.basketCartRepository.getListBasketCartByItemID(current.getId());
            BigInteger countOfAllProducts = new BigInteger("0");
            for (BasketCart basketCart : listCartsByID) {
                countOfAllProducts = countOfAllProducts.add(new BigInteger(basketCart.count));
            }

            if (countOfAllProducts.compareTo(new BigInteger(current.getQuantity())) <= 0) {
                holder.binding.addProduct.setVisibility(View.VISIBLE);
            }

            if (listCartsByID.size() != 0) {
                if (listCartsByID.size() == 1) {

                    BasketCart cartItem = listCartsByID.get(0);
                    BigInteger countOfProduct = new BigInteger(cartItem.count);
                    if (countOfProduct.equals(new BigInteger("1"))) {
                        holder.binding.countItemsLayout.setVisibility(View.GONE);
                        holder.binding.removeProduct.setVisibility(View.GONE);
                        Common.basketCartRepository.deleteBasketCart(cartItem);
                    } else {
                        countOfProduct = countOfProduct.subtract(new BigInteger("1"));
                        cartItem.count = countOfProduct.toString();
                        holder.binding.countItemInCart.setText(cartItem.count);
                        Common.basketCartRepository.updateBasketCart(cartItem);
                    }
                } else {

                    BasketCart cartItem = listCartsByID.get(listCartsByID.size() - 1);
                    BigInteger countOfProduct = new BigInteger(cartItem.count);
                    if (countOfProduct.equals(new BigInteger("1"))) {
                        Common.basketCartRepository.deleteBasketCart(cartItem);
                    } else {
                        countOfProduct = countOfProduct.subtract(new BigInteger("1"));
                        cartItem.count = countOfProduct.toString();
                        holder.binding.countItemInCart.setText(String.format("%s", countOfAllProducts.subtract(new BigInteger("1"))));
                        Common.basketCartRepository.updateBasketCart(cartItem);
                    }
                }
            }
        });

        Picasso.get()
                .load(current.getPreviewImage())
                .noPlaceholder()
                .centerCrop()
                .resize(400, 0)
                .into(holder.binding.image);
    }

    //if product have modifiers then we show bottomSheetDialog for its choice
    private void showBottomSheetDialog(ProductsNewMenuAdapter.ProductHolder holder, Item current, BigDecimal bd) {
        ProductModifierAdapter productModifierAdapter = new ProductModifierAdapter(context, current.getModifier());
        BottomSheetDialog productModifierSelectionBottomSheet = new BottomSheetDialog(context, R.style.AppBottomSheetDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.product_modifier_bottom_sheep_dialog, null);

        ImageView imageView = view.findViewById(R.id.image);
        Picasso.get()
                .load(current.getImages().get(0))
                .noPlaceholder()
                .centerCrop()
                .resize(800, 0)
                .into(imageView);

        TextView textName = view.findViewById(R.id.name);
        textName.setText(current.getName());
        TextView textCost = view.findViewById(R.id.cost);
        textCost.setText(String.format("%s %s", bd.toString(), SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        TextView countProducts = view.findViewById(R.id.countProducts);
        countProducts.setText("1");

        //control the count of modifiers
        HashMap<String, String> modifiers = new HashMap<>();
        productModifierAdapter.setListener((modifier, check) -> {
            if (check) {
                modifiers.put(modifier.getName(), modifier.getValue());
            } else {
                modifiers.remove(modifier.getName());
            }
            Logging.logDebug("modifiers: " + modifiers.toString());

            BigDecimal costCurrent = new BigDecimal(bd.toString());
            for (Map.Entry<String, String> modifierEntry : modifiers.entrySet()) {
                costCurrent = costCurrent.add(new BigDecimal(modifierEntry.getValue()));
            }
            costCurrent = costCurrent.multiply(new BigDecimal(countProducts.getText().toString()));
            textCost.setText(String.format("%s %s", costCurrent.toString(),
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        });

        view.findViewById(R.id.addProduct).setOnClickListener(v -> {
            BigInteger counter = new BigInteger(countProducts.getText().toString()).add(new BigInteger("1"));
            countProducts.setText(String.format("%s", counter.toString()));

            BigDecimal costCurrent = new BigDecimal(bd.toString());
            for (Map.Entry<String, String> modifierEntry : modifiers.entrySet()) {
                costCurrent = costCurrent.add(new BigDecimal(modifierEntry.getValue()));
            }
            costCurrent = costCurrent.multiply(new BigDecimal(counter.toString()));
            textCost.setText(String.format("%s %s", costCurrent.toString(),
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        });

        view.findViewById(R.id.removeProduct).setOnClickListener(v -> {
            if (!countProducts.getText().toString().equals("1")) {
                BigInteger counter = new BigInteger(countProducts.getText().toString()).subtract(new BigInteger("1"));
                countProducts.setText(String.format("%s", counter.toString()));

                BigDecimal costCurrent = new BigDecimal(bd.toString());
                for (Map.Entry<String, String> modifierEntry : modifiers.entrySet()) {
                    costCurrent = costCurrent.add(new BigDecimal(modifierEntry.getValue()));
                }
                costCurrent = costCurrent.multiply(new BigDecimal(counter.toString()));
                textCost.setText(String.format("%s %s", costCurrent.toString(),
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
            }
        });

        RecyclerView recyclerModifier = view.findViewById(R.id.recyclerModifiers);
        recyclerModifier.setLayoutManager(new LinearLayoutManager(context));
        recyclerModifier.setAdapter(productModifierAdapter);

        view.findViewById(R.id.addToCart).setOnClickListener(v -> {

            holder.binding.countItemsLayout.setVisibility(View.VISIBLE);
            holder.binding.removeProduct.setVisibility(View.VISIBLE);

            List<BasketCart> listBasketCartByItemID = Common.basketCartRepository.getListBasketCartByItemID(current.getId());

            BigInteger countOfAllProducts = new BigInteger("0");
            List<Modifier> listModifiers = new ArrayList<>();
            for (Map.Entry<String, String> modifierEntry : modifiers.entrySet()) {
                listModifiers.add(new Modifier(modifierEntry.getKey(), modifierEntry.getValue()));
            }
            if (listBasketCartByItemID != null && listBasketCartByItemID.size() != 0) {
                for (BasketCart basketCart : listBasketCartByItemID) {
                    countOfAllProducts = countOfAllProducts.add(new BigInteger(basketCart.count));
                }

                for (BasketCart basketCart : listBasketCartByItemID) {
                    if (basketCart.modifier.equals(listModifiers)) {
                        BigInteger countOfProductsToShow = new BigInteger(countProducts.getText().toString()).add(countOfAllProducts);
                        holder.binding.countItemInCart.setText(String.format("%s", countOfProductsToShow.toString()));
                        basketCart.count = new BigInteger(basketCart.count).add(new BigInteger(countProducts.getText().toString())).toString();
                        Common.basketCartRepository.updateBasketCart(basketCart);
                        Logging.logDebug("Method update Product in Basket - found similar!:  " + basketCart.toString());
                        productModifierSelectionBottomSheet.dismiss();
                        return;
                    }
                }
            }

            BasketCart cartItem = new BasketCart();
            cartItem.itemID = current.getId();
            cartItem.name = current.getName();
            cartItem.discount = current.getDiscount();
            cartItem.startPrice = current.getPrice();
            cartItem.finalPrice = bd.toString();
            cartItem.type = current.getType();
            cartItem.count = countProducts.getText().toString();
            cartItem.imageUrl = current.getPreviewImage();
            cartItem.quantity = current.getQuantity();
            cartItem.discount = current.getDiscount();
            cartItem.modifier = listModifiers;
            cartItem.isPromo = false;
            cartItem.isExist = true;
            cartItem.quantityType = current.getUnitType();
            Common.basketCartRepository.insertToBasketCart(cartItem);
            if (listBasketCartByItemID == null) {
                holder.binding.countItemInCart.setText(cartItem.count);
            } else {
                BigInteger countOfProductsToShow = new BigInteger(cartItem.count).add(countOfAllProducts);
                holder.binding.countItemInCart.setText(String.format("%s", countOfProductsToShow.toString()));
            }
            Logging.logDebug("Method add Product to Basket - not found similar!:  " + cartItem.toString());
            productModifierSelectionBottomSheet.dismiss();
        });

        productModifierSelectionBottomSheet.setContentView(view);
        productModifierSelectionBottomSheet.show();
    }

    @Override
    public int getItemCount() {
        return productsSort == null ? 0 : productsSort.size();
    }

    public static class ProductHolder extends RecyclerView.ViewHolder {
        private NewMenuProductItemBinding binding;

        public ProductHolder(NewMenuProductItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }


    private void showToast(String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

}
