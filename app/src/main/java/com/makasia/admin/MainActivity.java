package com.makasia.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

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
                // Android WebView can paint CSS fixed/animated modal sheets behind
                // their own backdrop on some WebView versions. Normalize modal
                // compositing only inside the native app; browser HTML stays unchanged.
                view.evaluateJavascript(
                    "(function(){"
                    + "var st=document.getElementById('__android_modal_fix');"
                    + "if(!st){st=document.createElement('style');st.id='__android_modal_fix';"
                    + "st.textContent='.modal-overlay{position:absolute!important;left:0!important;top:0!important;width:100%!important;min-height:100vh!important;"
                    + "display:none!important;align-items:center!important;justify-content:center!important;padding:20px!important;"
                    + "background:rgba(0,0,0,.55)!important;z-index:99999!important;}"
                    + ".modal-overlay.show{display:flex!important;visibility:visible!important;opacity:1!important;}"
                    + ".modal-overlay .modal{position:relative!important;z-index:100000!important;"
                    + "width:min(720px,100%)!important;max-width:720px!important;margin:auto!important;"
                    + "max-height:90vh!important;border-radius:20px!important;overflow-y:auto!important;"
                    + "transform:none!important;animation:none!important;visibility:visible!important;opacity:1!important;}"
                    + ".modal-overlay .modal.small{max-width:450px!important;}';"
                    + "document.head.appendChild(st);}"
                    + "})();",
                    null
                );
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
        // Keep the web UI's explicitly designed light colors in Android WebView.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            s.setForceDark(WebSettings.FORCE_DARK_OFF);
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(s, false);
            }
        }
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
