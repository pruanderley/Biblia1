package br.com.jfabiblia.offline;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;
import java.util.Locale;

public class MainActivity extends BridgeActivity {

    private TextToSpeech tts;
    private WebView webView;
    private boolean ttsReady = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = getBridge().getWebView();
        webView.addJavascriptInterface(new TTSBridge(), "Android");

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("pt", "BR"));
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {}

                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> webView.evaluateJavascript(
                            "if(typeof window.onTTSVerseFinished==='function')" +
                            "window.onTTSVerseFinished();", null));
                    }

                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> webView.evaluateJavascript(
                            "if(typeof window.onTTSVerseFinished==='function')" +
                            "window.onTTSVerseFinished();", null));
                    }
                });
                ttsReady = true;
            }
        });
    }

    public class TTSBridge {

        @JavascriptInterface
        public void startTTS(String text, String lang, float rate, float pitch) {
            if (!ttsReady || tts == null) return;
            tts.setSpeechRate(rate > 0 ? rate : 0.9f);
            tts.setPitch(pitch > 0 ? pitch : 1.0f);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "verse");
        }

        @JavascriptInterface
        public void stopTTS() {
            if (tts != null) tts.stop();
        }

        @JavascriptInterface
        public void acquireWakeLock() {}

        @JavascriptInterface
        public void releaseWakeLock() {}
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
