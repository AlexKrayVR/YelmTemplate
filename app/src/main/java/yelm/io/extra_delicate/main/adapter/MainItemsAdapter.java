package yelm.io.extra_delicate.main.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.List;

import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.by_category.ProductsByCategoriesActivity;
import yelm.io.extra_delicate.databinding.FragmentCategoryBinding;
import yelm.io.extra_delicate.databinding.SquareCategoryItemBinding;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.main.model.CategoriesWithProductsClass;
import yelm.io.extra_delicate.support_stuff.ItemOffsetDecorationRight;

public class MainItemsAdapter extends RecyclerView.Adapter<MainItemsAdapter.ProductHolder> {
    private Context context;
    private List<CategoriesWithProductsClass> catalogsWithProductsList;

    public MainItemsAdapter(Context context, List<CategoriesWithProductsClass> catalogsWithProductsList) {
        this.context = context;
        this.catalogsWithProductsList = catalogsWithProductsList;
    }

    @NonNull
    @Override
    public MainItemsAdapter.ProductHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ProductHolder productHolder = new ProductHolder(FragmentCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        productHolder.binding.categoryExpand.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        productHolder.binding.categoryExpand.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        return productHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final MainItemsAdapter.ProductHolder holder, final int position) {
        CategoriesWithProductsClass current = catalogsWithProductsList.get(position);
        holder.binding.title.setText(current.getName());
        holder.binding.categoryExpand.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductsByCategoriesActivity.class);
            intent.putExtra("catalogID", current.getCategoryID());
            intent.putExtra("catalogName", current.getName());
            context.startActivity(intent);
        });
        if (current.getItems().size() <= 5) {
            holder.binding.recycler.setAdapter(new ProductsNewMenuAdapter(context, current.getItems()));
            holder.binding.recycler.addItemDecoration(new ItemOffsetDecorationRight((int) context.getResources().getDimension(R.dimen.dimens_16dp)));
        } else {
            LinearLayout.LayoutParams marginLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            marginLayoutParams.setMargins((int) context.getResources().getDimension(R.dimen.dimens_8dp),
                    0,
                    (int) context.getResources().getDimension(R.dimen.dimens_8dp),
                    0);
            holder.binding.recycler.setLayoutParams(marginLayoutParams);
            holder.binding.recycler.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayout.VERTICAL));
            holder.binding.recycler.setAdapter(new ProductsNewMenuSquareImageAdapter(context, current.getItems()));
        }
    }

    @Override
    public int getItemCount() {
        return catalogsWithProductsList == null ? 0 : catalogsWithProductsList.size();
    }

    public static class ProductHolder extends RecyclerView.ViewHolder {
        private FragmentCategoryBinding binding;

        public ProductHolder(FragmentCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}