package com.barnaguer.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import androidx.room.Room;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView webView;
    private WebViewAssetLoader assetLoader;
    private OfflineDatabase offlineDatabase;
    private final ExecutorService offlineExecutor = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        offlineDatabase = Room.databaseBuilder(getApplicationContext(), OfflineDatabase.class,
            "barnaguer-offline.db").build();
        // Re-enqueue synchronization on app launch so saved pending requests are picked up.
        OfflineSyncScheduler.schedule(this);
        assetLoader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();
        webView = new WebView(this);
        webView.setBackgroundColor(0xFFD2CCC2);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.setWebViewClient(new WebViewClientCompat() {
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, android.webkit.WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return assetLoader.shouldInterceptRequest(Uri.parse(url));
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(
                    "(function() {" +
                    " if (window.__barnaguerFeedbackInstalled) return;" +
                    " window.__barnaguerFeedbackInstalled = true;" +
                    " var audioContext;" +
                    " function getAudio() {" +
                    "   var AudioCtx = window.AudioContext || window.webkitAudioContext;" +
                    "   if (!AudioCtx) return null;" +
                    "   if (!audioContext) audioContext = new AudioCtx();" +
                    "   if (audioContext.state === 'suspended') audioContext.resume();" +
                    "   return audioContext;" +
                    " }" +
                    " function playScheduleClick() {" +
                    "   var ctx = getAudio(); if (!ctx) return;" +
                    "   var now = ctx.currentTime;" +
                    "   var osc = ctx.createOscillator();" +
                    "   var gain = ctx.createGain();" +
                    "   osc.type = 'triangle';" +
                    "   osc.frequency.setValueAtTime(155, now);" +
                    "   osc.frequency.exponentialRampToValueAtTime(72, now + 0.055);" +
                    "   gain.gain.setValueAtTime(0.0001, now);" +
                    "   gain.gain.exponentialRampToValueAtTime(0.24, now + 0.004);" +
                    "   gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.075);" +
                    "   osc.connect(gain); gain.connect(ctx.destination);" +
                    "   osc.start(now); osc.stop(now + 0.08);" +
                    " }" +
                    " function playReservationSuccess() {" +
                    "   var ctx = getAudio(); if (!ctx) return;" +
                    "   var now = ctx.currentTime;" +
                    "   [523.25, 659.25, 783.99].forEach(function(freq, i) {" +
                    "     var osc = ctx.createOscillator();" +
                    "     var gain = ctx.createGain();" +
                    "     var start = now + i * 0.105;" +
                    "     osc.type = 'sine'; osc.frequency.setValueAtTime(freq, start);" +
                    "     gain.gain.setValueAtTime(0.0001, start);" +
                    "     gain.gain.exponentialRampToValueAtTime(0.16, start + 0.018);" +
                    "     gain.gain.exponentialRampToValueAtTime(0.0001, start + 0.42);" +
                    "     osc.connect(gain); gain.connect(ctx.destination);" +
                    "     osc.start(start); osc.stop(start + 0.43);" +
                    "   });" +
                    " }" +
                    " document.addEventListener('click', function(event) {" +
                    "   var target = event.target;" +
                    "   if (!(target instanceof Element)) return;" +
                    "   var button = target.closest('button,[role=button],input[type=button],input[type=submit],input[type=reset]');" +
                    "   if (!button) return;" +
                    "   if (window.BarnaguerHaptics) window.BarnaguerHaptics.vibrate();" +
                    "   var label = (button.innerText || button.value || button.getAttribute('aria-label') || '').trim();" +
                    "   if (/\\d{1,2}:\\d{2}|\\b(?:AM|PM)\\b/i.test(label)) playScheduleClick();" +
                    " }, true);" +
                    " var successVisible = false;" +
                    " var successText = '¡Tu turno quedó registrado para el';" +
                    " var observer = new MutationObserver(function() {" +
                    "   var pageText = document.body ? document.body.innerText : '';" +
                    "   var nowVisible = pageText.toLowerCase().indexOf(successText.toLowerCase()) !== -1;" +
                    "   if (nowVisible && !successVisible) playReservationSuccess();" +
                    "   successVisible = nowVisible;" +
                    " });" +
                    " if (document.body) observer.observe(document.body, {childList:true, subtree:true, characterData:true});" +
                    "})();", null);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AlarmBridge(), "BarnaguerNative");
        webView.addJavascriptInterface(new HapticsBridge(), "BarnaguerHaptics");
        webView.addJavascriptInterface(new UpdatesBridge(), "BarnaguerUpdates");
        webView.addJavascriptInterface(new OfflineBridge(), "BarnaguerOfflineNative");
        setContentView(webView);
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }


    private void sendOfflineResult(String callbackId, JSONObject result) {
        if (webView == null || callbackId == null) return;
        String script = "window.__barnaguerOfflineResult && window.__barnaguerOfflineResult(" +
            JSONObject.quote(callbackId) + "," + result.toString() + ");";
        webView.post(() -> webView.evaluateJavascript(script, null));
    }

    private class OfflineBridge {
        @JavascriptInterface public void savePendingBooking(String json, String callbackId) {
            offlineExecutor.execute(() -> {
                JSONObject result = new JSONObject();
                try {
                    JSONObject input = new JSONObject(json);
                    String name = input.optString("name", "").trim();
                    String phone = input.optString("phone", "").trim();
                    String date = input.optString("date", "").trim();
                    String time = input.optString("time", "").trim();
                    if (name.isEmpty() || phone.isEmpty() || date.isEmpty() || time.isEmpty()) {
                        result.put("success", false);
                        result.put("message", "Faltan datos para guardar la solicitud.");
                    } else if (offlineDatabase.offlineDao().findPendingForSlot(date, time) != null) {
                        result.put("success", false);
                        result.put("message", "Ya existe una solicitud pendiente para ese horario en este dispositivo.");
                    } else {
                        String id = input.optString("id", "");
                        if (id.isEmpty()) id = UUID.randomUUID().toString();
                        PendingBooking booking = new PendingBooking(id, name, phone, date, time,
                            "PENDIENTE", System.currentTimeMillis());
                        offlineDatabase.offlineDao().insertPending(booking);
                        result.put("success", true);
                        result.put("id", id);
                        result.put("status", "PENDIENTE");
                        result.put("message", "Solicitud guardada y pendiente de confirmación.");
                        OfflineSyncScheduler.schedule(MainActivity.this);
                    }
                } catch (Exception error) {
                    try {
                        result.put("success", false);
                        result.put("message", "No se pudo guardar la solicitud en este dispositivo.");
                    } catch (JSONException ignored) {}
                }
                sendOfflineResult(callbackId, result);
            });
        }

        @JavascriptInterface public void cacheOccupiedSlots(String date, String json) {
            offlineExecutor.execute(() -> {
                try {
                    JSONArray values = new JSONArray(json);
                    OfflineDao dao = offlineDatabase.offlineDao();
                    dao.clearCachedSlotsForDate(date);
                    for (int i = 0; i < values.length(); i++) {
                        String time = values.optString(i, "");
                        if (!time.isEmpty()) dao.cacheOccupiedSlot(new CachedSlot(date, time));
                    }
                } catch (Exception ignored) {}
            });
        }

        @JavascriptInterface public void getCachedSlots(String date, String callbackId) {
            offlineExecutor.execute(() -> {
                JSONArray slots = new JSONArray(offlineDatabase.offlineDao().getOccupiedSlots(date));
                JSONObject result = new JSONObject();
                try {
                    result.put("success", true);
                    result.put("slots", slots);
                } catch (JSONException ignored) {}
                sendOfflineResult(callbackId, result);
            });
        }

        @JavascriptInterface public void getRecentBookings(String callbackId) {
            offlineExecutor.execute(() -> {
                JSONArray rows = new JSONArray();
                for (PendingBooking booking : offlineDatabase.offlineDao().getRecentBookings()) {
                    JSONObject row = new JSONObject();
                    try {
                        row.put("id", booking.id);
                        row.put("date", booking.bookingDate);
                        row.put("time", booking.bookingTime);
                        row.put("status", booking.status);
                        row.put("createdAt", booking.createdAt);
                        rows.put(row);
                    } catch (JSONException ignored) {}
                }
                JSONObject result = new JSONObject();
                try {
                    result.put("success", true);
                    result.put("bookings", rows);
                } catch (JSONException ignored) {}
                sendOfflineResult(callbackId, result);
            });
        }
    }

    private class UpdatesBridge {
        @JavascriptInterface public void refreshAfterUpdate() {
            runOnUiThread(() -> {
                if (webView != null) {
                    webView.clearCache(true);
                    webView.reload();
                }
            });
        }
    }

    private class HapticsBridge {
        @JavascriptInterface public void vibrate() {
            runOnUiThread(() -> {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (vibrator == null || !vibrator.hasVibrator()) return;
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(30);
                }
            });
        }
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
        if (offlineDatabase != null) offlineExecutor.execute(() -> offlineDatabase.close());
        offlineExecutor.shutdown();
        super.onDestroy();
    }
}
