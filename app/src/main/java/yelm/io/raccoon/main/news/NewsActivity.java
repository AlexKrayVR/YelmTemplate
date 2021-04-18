package yelm.io.raccoon.main.news;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.appbar.AppBarLayout;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import yelm.io.raccoon.R;
import yelm.io.raccoon.databinding.ActivityNewsBinding;
import yelm.io.raccoon.loader.app_settings.SharedPreferencesSetting;
import yelm.io.raccoon.main.adapter.ProductsNewMenuSquareImageAdapter;
import yelm.io.raccoon.main.model.Item;
import yelm.io.raccoon.rest.query.RestMethods;
import yelm.io.raccoon.support_stuff.Logging;

public class NewsActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener {

    ActivityNewsBinding binding;
    private int maxScrollSize;
    private static final int PERCENTAGE_TO_SHOW_IMAGE = 80;
    private boolean isImageHidden;
    List<Item> products = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.back.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.share.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));

        NewNews news = getIntent().getParcelableExtra("news");
        if (news != null) {
            binding(news);
            products = news.getItems();
            if (products.size() != 0) {
                binding.titleProducts.setVisibility(View.VISIBLE);
            }
            binding.recycler.setLayoutManager(new StaggeredGridLayoutManager(2, 1));
            //binding.recycler.setAdapter(new ProductsNewMenuSquareImageAdapter(this, products));
            binding.share.setOnClickListener(v -> {
                RestMethods.sendStatistic("share_news");
                String sharingLink = "https://yelm.io/news/" + news.getId();
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, R.string.app_name);
                intent.putExtra(Intent.EXTRA_TEXT, sharingLink);
                startActivity(Intent.createChooser(intent, getResources().getString(R.string.newsActivityShare)));
            });
        } else {
            Logging.logError( "Method onCreate() in NewsActivity: by some reason news==null");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        binding.recycler.setAdapter(new ProductsNewMenuSquareImageAdapter(this, products));
    }

    private void binding(NewNews news) {
        String body = news.getDescription();
        Logging.logDebug( "body" + body);
        String data = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<title></title>" +
                "<style>" +
//                "    img {" +
//                "      border-radius: 16px;" +
//                "      width: 100% !important;" +
//                "    }" +
//                "" +
                ".img-overlay {" +
                "  width: 100%;" +
                "  height: auto;" +
                "  border-radius: 16px;" +
                "  overflow: hidden;" +
                "}" +
                "" +
                ".img-overlay img {" +
                "  min-width: 100%;" +
                "  max-width: 100%;" +
                "  width: 100%;" +
                "  display: block;" +
                "  -o-object-fit: cover;" +
                "  object-fit: cover;" +
                "}" +
                "    iframe {" +
                "      width: 100% !important;" +
                "    }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                body +
                "<script> Array.prototype.forEach.call(document.getElementsByTagName('img'), (element) => {" +
                "  const parent = element.parentNode;" +
                "  const wrapper = document.createElement('div');" +
                "  wrapper.classList.add('img-overlay');" +
                "  parent.replaceChild(wrapper, element);" +
                "  wrapper.appendChild(element);" +
                "}); </script>" +
                "</body>" +
                "</html>";
        binding.web.getSettings().setJavaScriptEnabled(true);
        binding.web.loadData(data, "text/html", "utf-8");
        binding.back.setOnClickListener(v -> finish());
        binding.collapsingToolbar.setTitle(news.getTitle());
        binding.appbar.addOnOffsetChangedListener(this);
        Picasso.get()
                .load(news.getImage())
                .noPlaceholder()
                .centerCrop()
                .resize(600, 0)
                .into(binding.image);
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