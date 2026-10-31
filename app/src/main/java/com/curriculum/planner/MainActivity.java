package com.curriculum.planner;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.view.ViewGroup;
import android.view.View;
import android.graphics.Color;
import android.widget.FrameLayout;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
  private WebView web;
  private ValueCallback<Uri[]> fileCallback;
  private String pendingBackup;
  private static final int FILE_REQUEST=41, SAVE_REQUEST=42;
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(245,247,244));
    getWindow().setNavigationBarColor(Color.rgb(245,247,244));
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    web=new WebView(this); setContentView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
    web.setOnApplyWindowInsetsListener((view, insets) -> {
      view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
          insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
      return insets.consumeSystemWindowInsets();
    });
    web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setDomStorageEnabled(true);
    web.getSettings().setAllowFileAccess(false);
    web.getSettings().setAllowContentAccess(true);
    web.getSettings().setMediaPlaybackRequiresUserGesture(true);
    web.addJavascriptInterface(new BackupBridge(), "AndroidBridge");
    web.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params) {
        if(fileCallback!=null)fileCallback.onReceiveValue(null);
        fileCallback=callback;
        try { startActivityForResult(params.createIntent(),FILE_REQUEST); } catch(Exception ex) {fileCallback.onReceiveValue(null);fileCallback=null;return false;}
        return true;
      }
    });
    web.setWebViewClient(new WebViewClient(){
      @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
        Uri uri=request.getUrl();
        if(uri.getScheme().equals("https") && uri.getHost()!=null && uri.getHost().equals("curriculum.local")){
          try{return new WebResourceResponse("text/html","UTF-8",getAssets().open("index.html"));}catch(Exception e){return null;}
        }
        return null;
      }
      @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request) {
        Uri uri=request.getUrl();
        if("curriculum.local".equals(uri.getHost()))return false;
        if(uri.getScheme()!=null && (uri.getScheme().equals("https")||uri.getScheme().equals("http"))){
          try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception ignored){}
          return true;
        }
        return false;
      }
    });
    web.loadUrl("https://curriculum.local/");
  }
  private void finishAI(String id, int status, String body, String error) {
    runOnUiThread(() -> {
      try {
        JSONObject result = new JSONObject();
        result.put("id", id);
        result.put("status", status);
        result.put("body", body == null ? "" : body);
        result.put("error", error == null ? "" : error);
        web.evaluateJavascript("window.__curriculumNativeResponse && window.__curriculumNativeResponse("
            + result.toString() + ")", null);
      } catch (Exception ignored) {}
    });
  }
  private static String readStream(InputStream stream) throws Exception {
    if (stream == null) return "";
    try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192]; int n;
      while ((n = in.read(buffer)) != -1) {
        if (out.size() + n > 6_000_000) throw new Exception("API response too large");
        out.write(buffer, 0, n);
      }
      return out.toString("UTF-8");
    }
  }
  private class BackupBridge {
    @JavascriptInterface public void requestAI(String id, String apiKey, String payload) {
      if (id == null || !id.matches("[a-zA-Z0-9_-]{1,80}")) return;
      new Thread(() -> {
        HttpURLConnection connection = null;
        try {
          connection = (HttpURLConnection)new URL("https://api.openai.com/v1/chat/completions").openConnection();
          connection.setRequestMethod("POST");
          connection.setConnectTimeout(20000);
          connection.setReadTimeout(90000);
          connection.setDoOutput(true);
          connection.setRequestProperty("Authorization", "Bearer " + apiKey);
          connection.setRequestProperty("Content-Type", "application/json");
          try (OutputStream out = connection.getOutputStream()) {
            out.write(payload.getBytes(StandardCharsets.UTF_8));
          }
          int status = connection.getResponseCode();
          String body = readStream(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
          finishAI(id, status, body, "");
        } catch (Exception e) {
          finishAI(id, 0, "", "Network request failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
          if (connection != null) connection.disconnect();
        }
      }).start();
    }
    @JavascriptInterface public void saveBackup(String data) {
      runOnUiThread(()->{
        pendingBackup=data;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE,"curriculum-backup.json");
        startActivityForResult(intent,SAVE_REQUEST);
      });
    }
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(request==FILE_REQUEST){
      if(fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));fileCallback=null;}
    } else if(request==SAVE_REQUEST && result==RESULT_OK && data!=null && data.getData()!=null && pendingBackup!=null){
      try(OutputStream stream=getContentResolver().openOutputStream(data.getData())){stream.write(pendingBackup.getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}
      pendingBackup=null;
    }
  }
  @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
