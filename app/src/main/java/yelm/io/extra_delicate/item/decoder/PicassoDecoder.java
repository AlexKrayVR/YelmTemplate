package yelm.io.extra_delicate.item.decoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

public class PicassoDecoder implements ImageDecoder {
    private String tag;
    private Picasso picasso;

    public PicassoDecoder(String tag, Picasso picasso) {
        this.tag = tag;
        this.picasso = picasso;
    }

    @Override
    public Bitmap decode(Context context, @NonNull Uri uri) throws Exception {
        return picasso
                .load(uri)
                .tag(tag)
                .config(Bitmap.Config.RGB_565)
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .get();
    }
}
