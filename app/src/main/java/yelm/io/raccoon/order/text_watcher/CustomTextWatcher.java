package yelm.io.raccoon.order.text_watcher;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.core.content.ContextCompat;

import yelm.io.raccoon.R;

public class CustomTextWatcher implements TextWatcher {
    private EditText editText;
    private Context context;

    public CustomTextWatcher(EditText e, Context context) {
        this.editText = e;
        this.context = context;
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.toString().trim().length() == 0) {
            editText.setBackground(ContextCompat.getDrawable(context, R.drawable.back_edittext_red));
        } else {
            editText.setBackground(ContextCompat.getDrawable(context, R.drawable.back_edittext_green));
        }
    }

    public void afterTextChanged(Editable s) {
    }
}
