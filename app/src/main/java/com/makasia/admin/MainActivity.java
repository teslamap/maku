package com.makasia.admin;

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
        webView.loadUrl("file:///android_asset/mobile-admin.html");
        setContentView(webView);
    }
    @Override public void onBackPressed() {
        WebView v = (WebView) findViewById(android.R.id.content).getRootView().findViewWithTag("web");
        super.onBackPressed();
    }
}
