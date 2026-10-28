package com.carimaproblox;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import org.json.JSONObject;
import java.net.URLEncoder;

public class MainActivity extends Activity {
    private WebView web;
    private WebView scraper;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int liveSearchId = 0;
    private String liveQuery = "";

    public class Bridge {
        @JavascriptInterface
        public void searchLive(String query) {
            final String q = query == null ? "" : query;
            handler.post(new Runnable() {
                public void run() { startLiveSearch(q); }
            });
        }
    }

    private void startLiveSearch(String query) {
        liveSearchId++;
        final int myId = liveSearchId;
        liveQuery = query;
        try {
            String url = "https://www.roblox.com/discover/?Keyword=" + URLEncoder.encode(query, "UTF-8");
            scraper.loadUrl(url);
        } catch (Exception e) {
            deliverLive(myId, "[]");
            return;
        }
        // Poll: Discover tiles are JS-rendered (~8 detik di tes browser).
        // Cek tiap 1 detik sampai 16 detik, kirim hasil begitu tile muncul.
        pollTiles(myId, 0);
    }

    private void pollTiles(final int myId, final int attempt) {
        if (myId != liveSearchId) return;
        if (attempt > 16) { deliverLive(myId, "[]"); return; }
        final String js =
            "(function(){try{" +
            "var out=[];var seen={};" +
            "var tiles=document.querySelectorAll('main a[href*\"/games/\"]');" +
            "for(var i=0;i<tiles.length;i++){var a=tiles[i];var u;try{u=new URL(a.href);}catch(e){continue;}" +
            "var place=u.searchParams.get('placeId')||u.pathname.split('/')[2];" +
            "if(!place||seen[place])continue;seen[place]=1;" +
            "var img=a.querySelector('img');var title=img&&img.alt?img.alt:a.textContent.trim();var imgSrc=img?img.src:null;" +
            "var txt=a.textContent||'';var m=txt.match(/(\\d+)%\\s*([\\d.]+[KM]?)/);" +
            "out.push({title:title,placeId:place,universeId:u.searchParams.get('universeId')||null,imgSrc:imgSrc," +
            "like:m?parseInt(m[1],10):null,playingStr:m?m[2]:null});}" +
            "return out;}catch(e){return [];}})()";
        scraper.evaluateJavascript(js, new android.webkit.ValueCallback<String>() {
            public void onReceiveValue(String value) {
                if (myId != liveSearchId) return;
                String json = value == null ? "[]" : value.trim();
                if (!json.equals("[]") && !json.equals("null") && json.length() > 2) {
                    deliverLive(myId, json);
                } else {
                    handler.postDelayed(new Runnable() {
                        public void run() { pollTiles(myId, attempt + 1); }
                    }, 1000);
                }
            }
        });
    }

    private void deliverLive(int myId, String json) {
        if (myId != liveSearchId) return;
        final String q = liveQuery;
        final String payload = json == null ? "[]" : json;
        web.evaluateJavascript("window.onLiveResults(" + JSONObject.quote(q) + "," + payload + ")", null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);

        scraper = new WebView(this);
        WebSettings ss = scraper.getSettings();
        ss.setJavaScriptEnabled(true);
        ss.setDomStorageEnabled(true);
        ss.setAllowFileAccess(true);
        // UA desktop supaya halaman Discover sama dengan yang dites di browser.
        ss.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        scraper.setWebViewClient(new WebViewClient());
        scraper.setVisibility(View.INVISIBLE);
        root.addView(scraper, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setLoadsImagesAutomatically(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        web.addJavascriptInterface(new Bridge(), "RoFindBridge");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("file://")) return false;
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                } catch (Exception ignored) {
                }
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");
        root.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack()) {
            web.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
