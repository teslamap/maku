package com.makasia.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private ValueCallback<Uri[]> filePathCallback;
    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        swipeRefresh = new SwipeRefreshLayout(this);
        webView = new WebView(this);
        swipeRefresh.addView(webView);
        swipeRefresh.setOnChildScrollUpCallback((parent, child) -> webView != null && webView.canScrollVertically(-1));
        swipeRefresh.setOnRefreshListener(() -> {
            if (webView != null) webView.reload();
            else swipeRefresh.setRefreshing(false);
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                swipeRefresh.setRefreshing(false);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> filePath,
                    FileChooserParams fileChooserParams) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = filePath;
                Intent intent;
                try {
                    intent = fileChooserParams.createIntent();
                } catch (Exception e) {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                }
                try {
                    startActivityForResult(Intent.createChooser(intent, "აირჩიეთ სურათი"), FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    filePath.onReceiveValue(null);
                    return false;
                }
                return true;
            }
        });
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        String pkg = getPackageName();
        String pageUrl;
        if (pkg.equals("com.makasia.store")) {
            pageUrl = "https://teslamap.github.io/maku/mobile-index.html";
        } else if (pkg.equals("com.makasia.manager")) {
            pageUrl = "https://teslamap.github.io/maku/management-mobile.html";
        } else {
            pageUrl = "https://teslamap.github.io/maku/mobile-admin.html";
        }
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.loadUrl(pageUrl);
        setContentView(swipeRefresh);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) return;
        Uri[] results = null;
        if (resultCode == RESULT_OK && data != null) {
            Uri selected = data.getData();
            if (selected != null) results = new Uri[] { selected };
            else if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                results = new Uri[count];
                for (int i = 0; i < count; i++) results[i] = data.getClipData().getItemAt(i).getUri();
            }
        }
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
