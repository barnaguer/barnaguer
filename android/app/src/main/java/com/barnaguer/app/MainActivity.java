package com.barnaguer.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView webView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFFD2CCC2);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AlarmBridge(), "BarnaguerNative");
        setContentView(webView);
        webView.loadUrl("https://barnaguer.github.io/barnaguer/");
    }

    private class AlarmBridge {
        @JavascriptInterface public boolean hasNotificationPermission() {
            return Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        @JavascriptInterface public boolean requestNotificationPermission() {
            if (Build.VERSION.SDK_INT < 33 || hasNotificationPermission()) return true;
            runOnUiThread(() -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42));
            return false;
        }

        @JavascriptInterface public boolean hasExactAlarmPermission() {
            if (Build.VERSION.SDK_INT < 31) return true;
            AlarmManager manager = getSystemService(AlarmManager.class);
            return manager != null && manager.canScheduleExactAlarms();
        }

        @JavascriptInterface public boolean requestExactAlarmPermission() {
            if (Build.VERSION.SDK_INT < 31 || hasExactAlarmPermission()) return true;
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return true;
            } catch (Exception e) { return false; }
        }

        @JavascriptInterface public boolean scheduleAlarms(String json) {
            if (!hasExactAlarmPermission()) return false;
            try { AlarmScheduler.replace(MainActivity.this, json); return true; }
            catch (Exception e) { return false; }
        }

        @JavascriptInterface public boolean cancelAlarms() {
            try { AlarmScheduler.replace(MainActivity.this, "[]"); return true; }
            catch (Exception e) { return false; }
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 42 && webView != null && grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            webView.post(() -> webView.evaluateJavascript(
                "document.getElementById('alarm-enabled')?.dispatchEvent(new Event('change'))", null));
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
