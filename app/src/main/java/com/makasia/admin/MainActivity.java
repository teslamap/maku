package com.makasia.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        String pageUrl = getPackageName().equals("com.makasia.store")
                ? "https://teslamap.github.io/maku/mobile-index.html"
                : "https://teslamap.github.io/maku/mobile-admin.html";
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.loadUrl(pageUrl);
        setContentView(webView);
    }
}
