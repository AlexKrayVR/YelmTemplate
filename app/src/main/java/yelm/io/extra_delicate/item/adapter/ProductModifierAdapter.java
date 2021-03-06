package yelm.io.extra_delicate.item.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.support_stuff.Logging;
import yelm.io.extra_delicate.databinding.ModifierProductItemBinding;
import yelm.io.extra_delicate.main.model.Modifier;

public class ProductModifierAdapter extends RecyclerView.Adapter<ProductModifierAdapter.ProductHolder> {

    private List<Modifier> modifiers;
    private Context context;
    private Listener listener;


    public interface Listener {
        void onChecked(Modifier product, boolean check);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }


    public ProductModifierAdapter(Context context, List<Modifier> modifiers) {
        this.context = context;
        this.modifiers = modifiers;
    }

    @NonNull
    @Override
    public ProductModifierAdapter.ProductHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ProductHolder(ModifierProductItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ProductHolder holder, int position) {
        Modifier current = modifiers.get(position);
        holder.binding.name.setText(current.getName());
        holder.binding.value.setText(String.format("+%s %s", current.getValue(),
                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));

        holder.binding.selector.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (holder.binding.selector.isChecked()) {
                Logging.logDebug("isChecked: "+ current.getName());
                listener.onChecked(current, true);
            } else {
                Logging.logDebug("isNotChecked: "+ current.getName());
                listener.onChecked(current, false);
            }
        });
    }

    @Override
    public int getItemCount() {
        return modifiers == null ? 0 : modifiers.size();
    }

    public static class ProductHolder extends RecyclerView.ViewHolder {
        private ModifierProductItemBinding binding;

        public ProductHolder(ModifierProductItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
