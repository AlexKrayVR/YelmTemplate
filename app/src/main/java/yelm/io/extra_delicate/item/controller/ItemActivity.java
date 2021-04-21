package yelm.io.extra_delicate.item.controller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.decoder.DecoderFactory;
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder;
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder;
import com.google.android.material.appbar.AppBarLayout;
import com.squareup.picasso.Picasso;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.database.Common;
import yelm.io.extra_delicate.database.basket_new.BasketCart;
import yelm.io.extra_delicate.databinding.ActivityItemBinding;
import yelm.io.extra_delicate.item.adapter.PicturesAdapter;
import yelm.io.extra_delicate.item.adapter.ProductModifierAdapter;
import yelm.io.extra_delicate.item.adapter.ProductSpecificationsAdapter;
import yelm.io.extra_delicate.item.decoder.PicassoDecoder;
import yelm.io.extra_delicate.item.decoder.PicassoRegionDecoder;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.main.model.Item;
import yelm.io.extra_delicate.main.model.Modifier;
import yelm.io.extra_delicate.notification.CustomToast;
import yelm.io.extra_delicate.rest.query.RestMethods;
import yelm.io.extra_delicate.support_stuff.ItemOffsetDecorationStartEnd;
import yelm.io.extra_delicate.support_stuff.Logging;

public class ItemActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener {
    ActivityItemBinding binding;
    private int maxScrollSize;
    private static final int PERCENTAGE_TO_SHOW_IMAGE = 80;
    private boolean isImageHidden;
    ProductSpecificationsAdapter productSpecificationsAdapter;
    ProductModifierAdapter productModifierAdapter;
    PicturesAdapter picturesAdapter;

    HashMap<String, String> modifiers = new HashMap<>();

    private BigDecimal finalPrice = new BigDecimal("0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCustomColor();

        //TODO try to fix weight of title
        //binding.toolbar.setTitleMarginEnd(300);

        Item item = getIntent().getParcelableExtra("item");
        if (item != null) {
            finalPrice = getPrice(item, new BigDecimal(item.getPrice()));
            binding(item);
            bindingAddSubtractProductCount(item);
            bindingAddProductToBasket(item);
            binding.share.setOnClickListener(v -> {
                RestMethods.sendStatistic("share_item");
                String sharingLink = "https://yelm.io/item/" + item.getId();
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, R.string.app_name);
                intent.putExtra(Intent.EXTRA_TEXT, sharingLink);
                startActivity(Intent.createChooser(intent, getResources().getString(R.string.newsActivityShare)));
            });
        } else {
            Logging.logError("Method onCreate() in ItemActivity: by some reason item==null");
        }
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
        binding.cost.setText(String.format("%s %s", bd.toString(),
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        return bd;
    }

    private void setCustomColor() {
        binding.share.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.removeProduct.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.addProduct.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.back.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.addToCart.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.addToCart.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.back.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.share.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.removeProduct.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.addProduct.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.imageDiscount.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.countItems.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
    }

    private void binding(Item item) {
        checkItemQuantity(item);
        if (item.getModifier().size() == 0) {
            binding.modifierTitle.setVisibility(View.GONE);
        }
        if (item.getSpecification().size() == 0) {
            binding.specificationsTitle.setVisibility(View.GONE);
        }

        if (new BigInteger(item.getQuantity()).compareTo(new BigInteger("5")) < 0) {
            binding.countItems.setText(getString(R.string.itemActivityFew));
        } else {
            binding.countItems.setText(getString(R.string.itemActivityMany));
        }

        if (item.getDiscount().equals("0")) {
            binding.imageDiscount.setVisibility(View.GONE);
            binding.discount.setVisibility(View.GONE);
        } else {
            binding.discount.setText(String.format("%s %s %%", getText(R.string.product_discount), item.getDiscount()));
        }

        picturesAdapter = new PicturesAdapter(this, item.getImages());
        binding.recyclerPictures.setAdapter(picturesAdapter);
        picturesAdapter.setListener(this::showImage);
        binding.recyclerPictures.addItemDecoration(new ItemOffsetDecorationStartEnd(
                (int) getResources().getDimension(R.dimen.dimens_8dp),
                (int) getResources().getDimension(R.dimen.dimens_16dp)));

        productSpecificationsAdapter = new ProductSpecificationsAdapter(this, item.getSpecification());
        binding.recyclerSpecifications.setAdapter(productSpecificationsAdapter);

        productModifierAdapter = new ProductModifierAdapter(this, item.getModifier());
        productModifierAdapter.setListener((modifier, check) -> {
            if (check) {
                modifiers.put(modifier.getName(), modifier.getValue());
            } else {
                modifiers.remove(modifier.getName());
            }
            Logging.logDebug("modifiers: " + modifiers.toString());
            BigDecimal costCurrent = new BigDecimal(finalPrice.toString());
            for (Map.Entry<String, String> modifierEntry : modifiers.entrySet()) {
                costCurrent = costCurrent.add(new BigDecimal(modifierEntry.getValue()));
            }
            costCurrent = costCurrent.multiply(new BigDecimal(binding.countProducts.getText().toString()));
            binding.cost.setText(String.format("%s %s", costCurrent.toString(),
                    SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
        });
        binding.recyclerModifier.setAdapter(productModifierAdapter);

        binding.back.setOnClickListener(v -> finish());
        binding.collapsingToolbar.setTitle(item.getName());


        binding.name.setText(item.getName());
        binding.appbar.addOnOffsetChangedListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.description.setText(Html.fromHtml(item.getDescription(), Html.FROM_HTML_MODE_COMPACT));
        } else {
            binding.description.setText(Html.fromHtml(item.getDescription()));
        }

        binding.description.setText(item.getDescription());
        binding.ratingBar.setRating(Float.parseFloat(item.getRating()));

        Picasso.get()
                .load(item.getImages().get(0))
                .noPlaceholder()
                .centerCrop()
                .resize(1000, 0)
                .into(binding.image);

    }

    private void showImage(String imageUrl) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(ItemActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(ItemActivity.this)
                .inflate(R.layout.layout_image_details,
                        findViewById(R.id.layoutDialogContainer));
        builder.setView(view);

        com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView image = view.findViewById(R.id.imageView);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)), PorterDuff.Mode.SRC_IN);
        android.app.AlertDialog alertDialog = builder.create();

        image.setMinimumDpi(60);
        image.setBitmapDecoderFactory(new DecoderFactory<ImageDecoder>() {
            @NonNull
            public ImageDecoder make() {
                return new PicassoDecoder(imageUrl, Picasso.get());
            }
        });

        image.setRegionDecoderFactory(new DecoderFactory<ImageRegionDecoder>() {
            @NonNull
            @Override
            public ImageRegionDecoder make() throws IllegalAccessException, InstantiationException {
                return new PicassoRegionDecoder(new OkHttpClient());
            }
        });
        image.setImage(ImageSource.uri(imageUrl));
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();
    }

    private void bindingAddSubtractProductCount(Item item) {
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
            checkItemQuantity(item);
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
                checkItemQuantity(item);
            }
        });
    }

    private void bindingAddProductToBasket(Item product) {
        binding.addToCart.setOnClickListener(v -> {
            //get count of item in db
            List<BasketCart> listCartsByID = Common.basketCartRepository.getListBasketCartByItemID(product.getId());
            BigInteger countOfAllProducts = new BigInteger("0");
            for (BasketCart basketCart : listCartsByID) {
                countOfAllProducts = countOfAllProducts.add(new BigInteger(basketCart.count));
            }

            //cant add product if limit is over
            if (new BigInteger(binding.countProducts.getText().toString()).add(countOfAllProducts).compareTo(new BigInteger(product.getQuantity())) > 0) {
                CustomToast.showStatus(this, getString(R.string.productsNotAvailable) +
                        " " + product.getQuantity() + " " + getString(R.string.basketActivityPC));
                return;
            }

            //show status - how much we added in basket
            String added = (binding.countProducts.getText().toString().equals("1")
                    ? getString(R.string.productNewActivityAddedOne)
                    : getString(R.string.productNewActivityAddedMulti));
            CustomToast.showStatus(this, "" +
                    product.getName() + " " +
                    binding.countProducts.getText().toString() + " " +
                    getText(R.string.productNewActivityPC) + " " +
                    added + " " +
                    getText(R.string.productNewActivityAddedToBasket));

            //add modifiers that chosen
            List<Modifier> listModifiers = new ArrayList<>();
            for (Map.Entry<String, String> modifierEntry : modifiers.entrySet()) {
                listModifiers.add(new Modifier(modifierEntry.getKey(), modifierEntry.getValue()));
            }

            //if we found similar modifiers  -> increase count of item
            for (BasketCart basketCart : listCartsByID) {
                if (basketCart.modifier.equals(listModifiers)) {
                    basketCart.count = new BigInteger(basketCart.count).add(new BigInteger(binding.countProducts.getText().toString())).toString();
                    Common.basketCartRepository.updateBasketCart(basketCart);
                    Logging.logDebug("Update Item in Basket: " + basketCart.toString());
                    checkItemQuantity(product);
                    return;
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
            Logging.logDebug("Add Item to Basket:  " + cartItem.toString());

            checkItemQuantity(product);
        });
    }

    private void checkItemQuantity(Item item) {
        //get count of item in db
        List<BasketCart> listCartsByID = Common.basketCartRepository.getListBasketCartByItemID(item.getId());
        BigInteger countOfAllProducts = new BigInteger("0");
        for (BasketCart basketCart : listCartsByID) {
            countOfAllProducts = countOfAllProducts.add(new BigInteger(basketCart.count));
        }

        Logging.logDebug("countOfAllProducts:  " + countOfAllProducts);
        Logging.logDebug("item.getQuantity():  " + item.getQuantity());
        Logging.logDebug("binding.countProducts.getText().toString():  " + binding.countProducts.getText().toString());


        if (countOfAllProducts.add(new BigInteger(binding.countProducts.getText().toString())).compareTo(new BigInteger(item.getQuantity())) >= 0) {
            binding.addProduct.setEnabled(false);
            binding.addProduct.getBackground().setTint(getResources().getColor(R.color.colorButtonOrderingDisable));
        } else {
            binding.addProduct.setEnabled(true);
            binding.addProduct.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        }


        if (countOfAllProducts.add(new BigInteger(binding.countProducts.getText().toString())).compareTo(new BigInteger(item.getQuantity())) > 0) {
            binding.addToCart.setEnabled(false);
            binding.addToCart.getBackground().setTint(getResources().getColor(R.color.colorButtonOrderingDisable));
        } else {
            binding.addToCart.setEnabled(true);
            binding.addToCart.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        }


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
                //ViewCompat.animate(mFab).scaleY((float) 0.9).scaleX((float) 0.9).start();
                //ViewCompat.animate(imageButton).scaleY((float) 0.2).scaleX((float) 0.2).setDuration(200).start();
            }
        }

        if (currentScrollPercentage < PERCENTAGE_TO_SHOW_IMAGE) {
            if (isImageHidden) {
                isImageHidden = false;
                //ViewCompat.animate(mFab).scaleY(1).scaleX(1).start();
                //ViewCompat.animate(imageButton).scaleY(1).scaleX(1).start();
            }
        }
    }
}