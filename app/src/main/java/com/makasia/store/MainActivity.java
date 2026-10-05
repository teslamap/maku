package com.makasia.store;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;\nimport android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    private WebView webView;
    private WebViewAssetLoader assetLoader;

    private static class StoreWebView extends WebView {
        public StoreWebView(android.content.Context context) {
            super(context);
            setVerticalScrollBarEnabled(true);
            setHorizontalScrollBarEnabled(false);
            setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            setNestedScrollingEnabled(true);
            setFocusable(true);
            setFocusableInTouchMode(true);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            // Keep native WebView scrolling/touch handling enabled.
            getParent().requestDisallowInterceptTouchEvent(true);
            return super.onTouchEvent(event);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new StoreWebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
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
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        if ("embedded".equals(BuildConfig.FLAVOR)) {
            assetLoader = new WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                    .build();

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(
                        WebView view, WebResourceRequest request) {
                    return assetLoader.shouldInterceptRequest(request.getUrl());
                }

                @Override
                public WebResourceResponse shouldInterceptRequest(
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
        webView.requestFocus(View.FOCUS_DOWN);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
