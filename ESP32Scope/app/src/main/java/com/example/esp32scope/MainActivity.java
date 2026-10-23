package com.example.esp32scope;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Thin native shell around the oscilloscope page (assets/scope.html).
 * The important part is bindToWifi(): the ESP32 access point has no internet,
 * so Android would otherwise route traffic over mobile data and the
 * WebSocket to 192.168.4.1 would fail.
 */
public class MainActivity extends Activity {

    private WebView web;
    private ConnectivityManager cm;
    private ConnectivityManager.NetworkCallback netCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        web.setBackgroundColor(0xFF0B0F14);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(false);
        s.setSupportZoom(false);
        web.setWebViewClient(new WebViewClient());

        bindToWifi();
        web.loadUrl("file:///android_asset/scope.html");
    }

    private void bindToWifi() {
        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest req = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        netCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                cm.bindProcessToNetwork(network);
            }

            @Override
            public void onLost(Network network) {
                cm.bindProcessToNetwork(null);
            }
        };
        try {
            cm.requestNetwork(req, netCallback);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (cm != null && netCallback != null) {
                cm.unregisterNetworkCallback(netCallback);
                cm.bindProcessToNetwork(null);
            }
        } catch (Exception ignored) {
        }
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
