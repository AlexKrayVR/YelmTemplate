package yelm.io.raccoon.item;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SearchView;

import java.math.BigDecimal;
import java.util.ArrayList;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import yelm.io.raccoon.R;
import yelm.io.raccoon.basket.controller.BasketActivity;
import yelm.io.raccoon.database_new.Common;
import yelm.io.raccoon.database_new.basket_new.BasketCart;
import yelm.io.raccoon.databinding.ActivityItemsFromNewsBinding;
import yelm.io.raccoon.loader.app_settings.SharedPreferencesSetting;
import yelm.io.raccoon.loader.controller.LoaderActivity;
import yelm.io.raccoon.main.adapter.ProductsNewMenuSquareImageAdapter;
import yelm.io.raccoon.main.model.Item;
import yelm.io.raccoon.support_stuff.ItemOffsetDecorationBottom;
import yelm.io.raccoon.support_stuff.Logging;

public class ItemsOfOneCategoryActivity extends AppCompatActivity {
    ActivityItemsFromNewsBinding binding;
    private final CompositeDisposable compositeDisposableBasket = new CompositeDisposable();
    ProductsNewMenuSquareImageAdapter productsSquareAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItemsFromNewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCustomColor();
        binding();
    }

    private void setCustomColor() {
        binding.back.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.basket.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.basket.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        for (Drawable drawable : binding.basket.getCompoundDrawablesRelative()) {
            if (drawable != null) {
                drawable.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)), PorterDuff.Mode.SRC_IN));
            }
        }
        binding.back.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
    }

    private void binding() {
        binding.title.setText(getIntent().getStringExtra("title"));
        binding.recycler.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayout.VERTICAL));
        binding.recycler.setHasFixedSize(false);
        binding.recycler.addItemDecoration(new ItemOffsetDecorationBottom((int) getResources().getDimension(R.dimen.dimen_70dp)));
        binding.back.setOnClickListener(v -> finish());
        binding.basket.setOnClickListener(v -> startActivity(new Intent(this, BasketActivity.class)));
        binding.search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                productsSquareAdapter.getFilter().filter(s);
                binding.recycler.scrollToPosition(0);
                return false;
            }
        });
    }

    private void rewriteView() {
        ArrayList<Item> items = getIntent().getParcelableArrayListExtra("items");
        productsSquareAdapter = new ProductsNewMenuSquareImageAdapter(this, items);
        binding.recycler.setAdapter(productsSquareAdapter);
    }

    @Override
    public void onStop() {
        compositeDisposableBasket.clear();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateCost();
        rewriteView();
    }

    private void updateCost() {
        compositeDisposableBasket.add(Common.basketCartRepository.getBasketCarts()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(carts -> {
                    if (carts.size() == 0) {
                        binding.basket.setVisibility(View.GONE);
                        binding.basket.setText(String.format("0 %s", SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
                    } else {
                        binding.basket.setVisibility(View.VISIBLE);
                        BigDecimal basketPrice = new BigDecimal("0");
                        for (BasketCart cart : carts) {
                            basketPrice = basketPrice.add(new BigDecimal(cart.finalPrice).multiply(new BigDecimal(cart.count)));
                        }
                        binding.basket.setText(String.format("%s %s", basketPrice.toString(),
                                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
                        Logging.logDebug("Method updateCost() - carts.size(): " + carts.size() + "\n" +
                                "basketPrice.toString(): " + basketPrice.toString());
                    }
                }));
    }
}