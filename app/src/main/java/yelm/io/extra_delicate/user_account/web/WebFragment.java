package yelm.io.extra_delicate.user_account.web;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.jetbrains.annotations.NotNull;

import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.databinding.FragmentAccountBinding;
import yelm.io.extra_delicate.databinding.FragmentWebBinding;
import yelm.io.extra_delicate.user_account.HostLogin;

public class WebFragment extends Fragment {


    private static final String URL = "URL";
    private FragmentWebBinding binding;
    private String url;

    public WebFragment() {
        // Required empty public constructor
    }

    public static WebFragment newInstance(String url) {
        WebFragment fragment = new WebFragment();
        Bundle args = new Bundle();
        args.putString(URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            url = getArguments().getString(URL);
        }
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentWebBinding.inflate(getLayoutInflater(), container, false);
        binding.webView.setAlpha(0f);
        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                binding.progressBar.setVisibility(View.GONE);
                binding.webView.animate().setDuration(300).alpha(1f).start();
            }
        });
        binding.webView.loadUrl(url);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }


}