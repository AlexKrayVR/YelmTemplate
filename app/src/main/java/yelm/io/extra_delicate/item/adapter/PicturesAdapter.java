package yelm.io.extra_delicate.item.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import yelm.io.extra_delicate.databinding.ModifierProductItemBinding;
import yelm.io.extra_delicate.databinding.PictureItemBinding;

public class PicturesAdapter extends RecyclerView.Adapter<PicturesAdapter.ProductHolder> {

    private List<String> images;
    private Context context;
    private Listener listener;


    public interface Listener {
        void onTap(String image);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }


    public PicturesAdapter(Context context, List<String> images) {
        this.context = context;
        this.images = images;
    }

    @NonNull
    @Override
    public PicturesAdapter.ProductHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ProductHolder(PictureItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ProductHolder holder, int position) {
        String imageUrl = images.get(position);
        Picasso.get()
                .load(imageUrl)
                .noPlaceholder()
                .centerCrop()
                .resize(400, 0)
                .into(holder.binding.image);
        holder.binding.image.setOnClickListener(v -> {
            listener.onTap(imageUrl);
        });
    }

    @Override
    public int getItemCount() {
        return images == null ? 0 : images.size();
    }

    public static class ProductHolder extends RecyclerView.ViewHolder {
        private PictureItemBinding binding;

        public ProductHolder(PictureItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}