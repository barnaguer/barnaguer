package com.barnaguer.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.IntentSender;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import org.json.JSONObject;
import java.util.Collections;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView webView;
    private static final int GOOGLE_AUTH_REQUEST_CODE = 7103;
    private boolean googleAuthInProgress = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFFD2CCC2);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
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
        setContentView(webView);
        webView.loadUrl("https://barnaguer.github.io/barnaguer/");
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
        @JavascriptInterface public void requestGoogleContactsAuthorization() {
            runOnUiThread(() -> startGoogleContactsAuthorization());
        }

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

    private void startGoogleContactsAuthorization() {
        if (googleAuthInProgress) {
            deliverGoogleAuthResult(null, "Ya hay una autorización de Google en curso.");
            return;
        }
        googleAuthInProgress = true;
        AuthorizationRequest request = AuthorizationRequest.builder()
            .setRequestedScopes(Collections.singletonList(
                new Scope("https://www.googleapis.com/auth/contacts")
            ))
            .build();

        Identity.getAuthorizationClient(this).authorize(request)
            .addOnSuccessListener(result -> {
                if (result.hasResolution()) {
                    try {
                        startIntentSenderForResult(
                            result.getPendingIntent().getIntentSender(),
                            GOOGLE_AUTH_REQUEST_CODE, null, 0, 0, 0
                        );
                    } catch (IntentSender.SendIntentException e) {
                        finishGoogleAuth(null, "No se pudo abrir la autorización de Google: " + e.getMessage());
                    }
                } else {
                    finishGoogleAuth(result.getAccessToken(), null);
                }
            })
            .addOnFailureListener(error ->
                finishGoogleAuth(null, "Google no pudo iniciar la autorización: " + error.getMessage())
            );
    }

    private void finishGoogleAuth(String token, String error) {
        googleAuthInProgress = false;
        deliverGoogleAuthResult(token, error);
    }

    private void deliverGoogleAuthResult(String token, String error) {
        if (webView == null) return;
        String tokenArg = token == null ? "null" : JSONObject.quote(token);
        String errorArg = error == null ? "null" : JSONObject.quote(error);
        String script = "if(window.__barnaguerGoogleAuthResult){window.__barnaguerGoogleAuthResult("
            + tokenArg + "," + errorArg + ");}";
        webView.post(() -> webView.evaluateJavascript(script, null));
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != GOOGLE_AUTH_REQUEST_CODE) return;
        if (resultCode != RESULT_OK || data == null) {
            finishGoogleAuth(null, "Se canceló la autorización de Google.");
            return;
        }
        try {
            AuthorizationResult result =
                Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data);
            finishGoogleAuth(result.getAccessToken(), null);
        } catch (Exception e) {
            finishGoogleAuth(null, "Google no autorizó el acceso: " + e.getMessage());
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
