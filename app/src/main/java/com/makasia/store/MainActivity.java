package com.makasia.store;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    private WebView webView;
    private WebViewAssetLoader assetLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(false);

        if ("embedded".equals(BuildConfig.FLAVOR)) {
            assetLoader = new WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                    .build();

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public android.webkit.WebResourceResponse shouldInterceptRequest(
                        WebView view, WebResourceRequest request) {
                    return assetLoader.shouldInterceptRequest(request.getUrl());
                }

                @Override
                public android.webkit.WebResourceResponse shouldInterceptRequest(
                        WebView view, String url) {
                    return assetLoader.shouldInterceptRequest(android.net.Uri.parse(url));
                }
            });

            webView.loadUrl("https://appassets.androidplatform.net/assets/mobile-index.html");
        } else {
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl("https://teslamap.github.io/maku/mobile-index.html");
        }

        webView.setWebChromeClient(new WebChromeClient());
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
