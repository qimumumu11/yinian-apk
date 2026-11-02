package com.qimumumu11.yinian;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

/**
 * 一念 · 离线条
 * 页面全部打包在 APK 的 assets 里，通过 WebViewAssetLoader 以 https 域名提供给 WebView，
 * 这样 localStorage / ServiceWorker / 安全上下文都正常，且打开不依赖网络。
 */
public class MainActivity extends Activity {

    private static final String HOME = "https://appassets.androidplatform.net/assets/index.html";
    private static final int BAR = 0xFFEEF1F6;

    private WebView web;
    private WebViewAssetLoader loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BAR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().setNavigationBarColor(BAR);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int f = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(f);
        }

        loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web = new WebView(this);
        web.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        web.setBackgroundColor(BAR);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setGeolocationEnabled(true);
        s.setUserAgentString(s.getUserAgentString() + " YinianApp/1.0");

        web.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {
                return loader.shouldInterceptRequest(req.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                String u = req.getUrl().toString();
                /* 站内（appassets）和外链 http(s) 都在本 WebView 打开 */
                return !(u.startsWith("http://") || u.startsWith("https://"));
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                /* 天气定位：交给页面自行兜底（IP 定位 / 手动选城市） */
                cb.invoke(origin, true, false);
            }
        });

        setContentView(web);

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(HOME);
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (web != null) web.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.setVisibility(View.GONE);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
