package com.makasia.app;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;
import android.view.View;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private ValueCallback<Uri[]> filePathCallback;
    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private String fcmToken;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createOrderNotificationChannel();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 2001);
        }
        swipeRefresh = new SwipeRefreshLayout(this);
        webView = new WebView(this);
        swipeRefresh.addView(webView);
        // Admin uses standalone WebView rendering for reliable modal compositing.
        // SwipeRefreshLayout can create a separate/clipped drawing layer around fixed popups.
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        swipeRefresh.setOnChildScrollUpCallback((parent, child) -> webView != null && webView.canScrollVertically(-1));
        swipeRefresh.setOnRefreshListener(() -> {
            if (webView != null) webView.reload();
            else swipeRefresh.setRefreshing(false);
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                swipeRefresh.setRefreshing(false);
                deliverFcmToken();
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
                // APK diagnostic console: visible on-screen and captures JS errors, taps and modal state.
                view.evaluateJavascript(
                    "(function(){"
                    + "if(document.getElementById('__android_debug'))return;"
                    + "var box=document.createElement('div');box.id='__android_debug';"
                    + "box.style='position:fixed;left:8px;right:8px;top:70px;max-height:70vh;z-index:2147483647;background:rgba(15,15,20,.96);color:#fff;border:1px solid #555;border-radius:12px;font:12px monospace;display:none;overflow:auto;padding:10px;white-space:pre-wrap;box-shadow:0 8px 30px rgba(0,0,0,.5)';"
                    + "var head=document.createElement('div');head.style='display:flex;gap:8px;align-items:center;margin-bottom:7px';"
                    + "var title=document.createElement('b');title.textContent='🛠 APK DEBUG CONSOLE';head.appendChild(title);"
                    + "var clear=document.createElement('button');clear.textContent='გასუფთავება';clear.style='margin-left:auto;padding:5px 8px';head.appendChild(clear);"
                    + "box.appendChild(head);var logArea=document.createElement('div');box.appendChild(logArea);document.body.appendChild(box);"
                    + "clear.onclick=function(){logArea.innerHTML=''};"
                    + "var fab=document.createElement('button');fab.textContent='🐞';fab.style='position:fixed;right:12px;top:12px;z-index:2147483647;width:52px;height:52px;border:0;border-radius:50%;font-size:22px;background:#111;color:#fff;box-shadow:0 3px 15px rgba(0,0,0,.45)';"
                    + "fab.onclick=function(){box.style.display=box.style.display==='none'?'block':'none'};document.body.appendChild(fab);"
                    + "function log(x){var d=document.createElement('div');d.textContent=new Date().toLocaleTimeString()+'  '+x;logArea.prepend(d)};"
                    + "window.onerror=function(msg,src,line,col,err){log('❌ ERROR: '+msg+' @ '+line+':'+col);if(err&&err.stack)log(err.stack);return false};"
                    + "window.addEventListener('unhandledrejection',function(e){log('❌ PROMISE: '+(e.reason&&e.reason.stack||e.reason||'unknown'))});"
                    + "document.addEventListener('click',function(e){var t=e.target;var el=t.closest?t.closest('button,a,[onclick],input,select,textarea'):t;if(el)log('👆 CLICK: '+(el.tagName||'')+' '+(el.id?'#'+el.id:'')+' '+((el.textContent||'').trim().slice(0,45)));setTimeout(function(){document.querySelectorAll('.modal-overlay').forEach(function(m){if(m.classList.contains('show')){var r=m.getBoundingClientRect();log('🪟 MODAL SHOW: #'+m.id+' display='+getComputedStyle(m).display+' vis='+getComputedStyle(m).visibility+' rect='+Math.round(r.left)+','+Math.round(r.top)+','+Math.round(r.width)+','+Math.round(r.height));}})},80)},true);"
                    + "log('✅ Debug ready — tap 🐞 and reproduce the problem.');"
                    + "})();", null
                );
                // Force modal overlays into a top-level WebView layer after every page load.
                view.evaluateJavascript(
                    "(function(){"
                    + "document.querySelectorAll('.modal-overlay').forEach(function(m){"
                    + "if(m.parentElement!==document.body)document.body.appendChild(m);"
                    + "m.style.setProperty('position','fixed','important');"
                    + "m.style.setProperty('left','0','important');m.style.setProperty('top','0','important');"
                    + "m.style.setProperty('right','0','important');m.style.setProperty('bottom','0','important');"
                    + "m.style.setProperty('width','100vw','important');m.style.setProperty('height','100vh','important');"
                    + "m.style.setProperty('display',m.classList.contains('show')?'flex':'none','important');"
                    + "m.style.setProperty('align-items','center','important');m.style.setProperty('justify-content','center','important');"
                    + "m.style.setProperty('z-index','2147483647','important');"
                    + "var p=m.querySelector('.modal');if(p){p.style.setProperty('display','block','important');p.style.setProperty('position','relative','important');p.style.setProperty('z-index','2147483647','important');p.style.setProperty('visibility','visible','important');p.style.setProperty('opacity','1','important');}});"
                    + "})();", null
                );
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage message) {
                Log.d("MakasiaWebView", message.message() + " @" + message.lineNumber() + " " + message.sourceId());
                return true;
            }

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
            pageUrl = "https://teslamap.github.io/maku/mobile-admin.html?v=modalfix2";
        }
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.loadUrl(pageUrl);
        FirebaseMessaging.getInstance().getToken()
            .addOnSuccessListener(token -> { fcmToken = token; deliverFcmToken(); })
            .addOnFailureListener(error -> Log.w("MakasiaFCM", "Unable to retrieve FCM token", error));
        setContentView(swipeRefresh);
    }

    private void createOrderNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "makasia_orders", "Makasia შეკვეთები", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("ახალი შეკვეთების შეტყობინებები");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void deliverFcmToken() {
        if (webView == null || fcmToken == null || fcmToken.isEmpty()) return;
        String js = "window.__setMakasiaFcmToken && window.__setMakasiaFcmToken(" + JSONObject.quote(fcmToken) + ");";
        runOnUiThread(() -> {
            if (webView != null) webView.evaluateJavascript(js, null);
        });
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
