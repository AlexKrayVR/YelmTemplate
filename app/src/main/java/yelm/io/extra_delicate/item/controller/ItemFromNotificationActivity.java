package yelm.io.extra_delicate.item.controller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.item.adapter.ProductModifierAdapter;
import yelm.io.extra_delicate.item.adapter.ProductSpecificationsAdapter;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.support_stuff.Logging;
import yelm.io.extra_delicate.database.Common;
import yelm.io.extra_delicate.database.basket_new.BasketCart;
import yelm.io.extra_delicate.databinding.ActivityItemFromNotificationBinding;
import yelm.io.extra_delicate.main.model.Item;
import yelm.io.extra_delicate.main.model.Modifier;
import yelm.io.extra_delicate.rest.rest_api.RestAPI;
import yelm.io.extra_delicate.rest.client.RetrofitClient;

public class ItemFromNotificationActivity extends AppCompatActivity  implements AppBarLayout.OnOffsetChangedListener{
    ActivityItemFromNotificationBinding binding;
    private int maxScrollSize;
    private static final int PERCENTAGE_TO_SHOW_IMAGE = 80;
    private boolean isImageHidden;
    ProductSpecificationsAdapter productSpecificationsAdapter;
    ProductModifierAdapter productModifierAdapter;

    HashMap<String, String> modifiers = new HashMap<>();

    private BigDecimal finalPrice = new BigDecimal("0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItemFromNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle args = getIntent().getExtras();
        if (args != null) {
            Logging.logDebug("ItemFromNotificationActivity - id: " + args.getString("id"));
            String id = args.getString("id");
            getItemById(id);
        }
    }

    private void getItemById(String id) {
        RetrofitClient.
                getClient(RestAPI.URL_API_MAIN).
                create(RestAPI.class).
                getIemByID(
                        "3.1",
                        RestAPI.PLATFORM_NUMBER,
                        getResources().getConfiguration().locale.getLanguage(),
                        getResources().getConfiguration().locale.getCountry(),
                        id).
                enqueue(new Callback<Item>() {
                    @Override
                    public void onResponse(@NotNull Call<Item> call, @NotNull final Response<Item> response) {
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                setItem(response.body());
                            } else {
                                Logging.logError("Method getItemById() - by some reason response is null!");
                            }
                        } else {
                            Logging.logError( "Method getItemById() - response is not successful." +
                                    "Code: " + response.code() + "Message: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<Item> call, @NotNull Throwable t) {
                        Logging.logError("Method getItemById() - failure: " + t.toString());
                    }
                });

    }

    private void setItem(Item item) {
        finalPrice = getPrice(item, new BigDecimal(item.getPrice()));
        binding(item);
        bindingAddSubtractProductCount();
        bindingAddProductToBasket(item);
        binding.share.setOnClickListener(v -> {
            String sharingLink = "https://yelm.io/item/" + item.getId();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, R.string.app_name);
            intent.putExtra(Intent.EXTRA_TEXT, sharingLink);
            startActivity(Intent.createChooser(intent, getResources().getString(R.string.newsActivityShare)));
        });


    }

    private BigDecimal getPrice(Item product, BigDecimal bd) {
        if (!product.getDiscount().equals("0")) {
            bd = new BigDecimal(product.getDiscount()).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            bd = bd.multiply(new BigDecimal(product.getPrice())).setScale(2, BigDecimal.ROUND_HALF_UP);
            bd = new BigDecimal(product.getPrice()).subtract(bd);
            //trim zeros if after comma there are only zeros: 45.00 -> 45
            if (bd.compareTo(new BigDecimal(String.valueOf(bd.setScale(0, BigDecimal.ROUND_HALF_UP)))) == 0) {
                bd = bd.setScale(0, BigDecimal.ROUND_HALF_UP);
            }
        }
        binding.cost.setText(String.format("%s %s", bd.toString(), SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        return bd;
    }

    private void binding(Item item) {
        if (item.getModifier() != null && item.getModifier().size() == 0) {
            binding.modifierTitle.setVisibility(View.GONE);
        }
        if (item.getSpecification() != null && item.getSpecification().size() == 0) {
            binding.specificationsTitle.setVisibility(View.GONE);
        }

        productSpecificationsAdapter = new ProductSpecificationsAdapter(this, item.getSpecification());
        binding.recyclerSpecifications.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSpecifications.setAdapter(productSpecificationsAdapter);

        productModifierAdapter = new ProductModifierAdapter(this, item.getModifier());
        productModifierAdapter.setListener((modifier, check) -> {
            if (check) {
                modifiers.put(modifier.getName(), modifier.getValue());
            } else {
                modifiers.remove(modifier.getName());
            }
            Logging.logDebug( "modifiers: " + modifiers.toString());
            BigDecimal costCurrent = new BigDecimal(finalPrice.toString());
            for (Map.Entry<String, String> modifierEntry : modifiers.entrySet()) {
                costCurrent = costCurrent.add(new BigDecimal(modifierEntry.getValue()));
            }
            costCurrent = costCurrent.multiply(new BigDecimal(binding.countProducts.getText().toString()));
            binding.cost.setText(String.format("%s %s", costCurrent.toString(),
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        });
        binding.recyclerModifier.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerModifier.setAdapter(productModifierAdapter);

        binding.back.setOnClickListener(v -> finish());
        binding.collapsingToolbar.setTitle(item.getName());
        binding.name.setText(item.getName());
        binding.appbar.addOnOffsetChangedListener(this);
        binding.description.setText(item.getDescription());
        binding.discount.setText(String.format("%s %s %%", getText(R.string.product_discount), item.getDiscount()));
        binding.ratingBar.setRating(Float.parseFloat(item.getRating()));
        Picasso.get()
                .load(item.getImages().get(0))
                .noPlaceholder()
                .centerCrop()
                .resize(800, 0)
                .into(binding.image);
    }

    private void bindingAddSubtractProductCount() {
        binding.addProduct.setOnClickListener(v -> {
            BigInteger counter = new BigInteger(binding.countProducts.getText().toString());
            counter = counter.add(new BigInteger("1"));
            binding.countProducts.setText(String.format("%s", counter.toString()));
            BigDecimal costCurrent = new BigDecimal(finalPrice.toString());
            for (Map.Entry<String, String> modifierEntry : modifiers.entrySet()) {
                costCurrent = costCurrent.add(new BigDecimal(modifierEntry.getValue()));
            }
            costCurrent = costCurrent.multiply(new BigDecimal(counter.toString()));
            binding.cost.setText(String.format("%s %s", costCurrent.toString(),
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        });

        binding.removeProduct.setOnClickListener(v -> {
            if (!binding.countProducts.getText().toString().equals("1")) {
                BigInteger counter = new BigInteger(binding.countProducts.getText().toString());
                counter = counter.subtract(new BigInteger("1"));
                binding.countProducts.setText(String.format("%s", counter.toString()));
                BigDecimal costCurrent = new BigDecimal(finalPrice.toString());
                for (Map.Entry<String, String> modifierEntry : modifiers.entrySet()) {
                    costCurrent = costCurrent.add(new BigDecimal(modifierEntry.getValue()));
                }
                costCurrent = costCurrent.multiply(new BigDecimal(counter.toString()));
                binding.cost.setText(String.format("%s %s", costCurrent.toString(),
                        SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
            }
        });
    }

    private void bindingAddProductToBasket(Item product) {
        binding.addToCart.setOnClickListener(v -> {
            String added = (String) (binding.countProducts.getText().toString().equals("1") ? getText(R.string.productNewActivityAddedOne) : getText(R.string.productNewActivityAddedMulti));
            Toast.makeText(this, "" +
                    product.getName() + " " +
                    binding.countProducts.getText().toString() + " " +
                    getText(R.string.productNewActivityPC) + " " +
                    added + " " +
                    getText(R.string.productNewActivityAddedToBasket), Toast.LENGTH_SHORT).show();

            List<BasketCart> listCartsByID = Common.basketCartRepository.getListBasketCartByItemID(product.getId());

            List<Modifier> listModifiers = new ArrayList<>();
            for (Map.Entry<String, String> modifierEntry : modifiers.entrySet()) {
                listModifiers.add(new Modifier(modifierEntry.getKey(), modifierEntry.getValue()));
            }

            if (listCartsByID != null && listCartsByID.size() != 0) {
                BigInteger countOfAllProducts = new BigInteger("0");
                for (BasketCart basketCart : listCartsByID) {
                    countOfAllProducts = countOfAllProducts.add(new BigInteger(basketCart.count));
                }
                for (BasketCart basketCart : listCartsByID) {
                    if (basketCart.modifier.equals(listModifiers)) {
                        basketCart.count = new BigInteger(basketCart.count).add(new BigInteger(binding.countProducts.getText().toString())).toString();
                        Common.basketCartRepository.updateBasketCart(basketCart);
                        Logging.logDebug( "Method update Product in Basket. listCartsByID !=null:  " + basketCart.toString());
                        return;
                    }
                }
            }
            BasketCart cartItem = new BasketCart();
            cartItem.itemID = product.getId();
            cartItem.name = product.getName();
            cartItem.discount = product.getDiscount();
            cartItem.startPrice = product.getPrice();
            cartItem.finalPrice = finalPrice.toString();
            cartItem.type = product.getType();
            cartItem.count = binding.countProducts.getText().toString();
            cartItem.imageUrl = product.getPreviewImage();
            cartItem.quantity = product.getQuantity();
            cartItem.discount = product.getDiscount();
            cartItem.modifier = listModifiers;
            cartItem.isPromo = false;
            cartItem.isExist = true;
            cartItem.quantityType = product.getUnitType();
            Common.basketCartRepository.insertToBasketCart(cartItem);
            Logging.logDebug("Method add Product to Basket. listCartsByID == null:  " + cartItem.toString());
        });
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (maxScrollSize == 0)
            maxScrollSize = appBarLayout.getTotalScrollRange();
        int currentScrollPercentage = (Math.abs(i)) * 100
                / maxScrollSize;
        if (currentScrollPercentage >= PERCENTAGE_TO_SHOW_IMAGE) {
            if (!isImageHidden) {
                isImageHidden = true;
            }
        }
        if (currentScrollPercentage < PERCENTAGE_TO_SHOW_IMAGE) {
            if (isImageHidden) {
                isImageHidden = false;
            }
        }
    }
}