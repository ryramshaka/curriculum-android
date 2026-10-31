package com.curriculum.planner;

import android.test.ActivityInstrumentationTestCase2;
import android.webkit.WebView;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AppSmokeTest extends ActivityInstrumentationTestCase2<MainActivity> {
  public AppSmokeTest() { super(MainActivity.class); }
  private WebView web() {
    return (WebView) ((android.view.ViewGroup) getActivity()
        .findViewById(android.R.id.content)).getChildAt(0);
  }
  private String js(String expression) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<String> result = new AtomicReference<>();
    getInstrumentation().runOnMainSync(() -> web().evaluateJavascript(expression, value -> {
      result.set(value);
      latch.countDown();
    }));
    assertTrue("JavaScript callback timed out", latch.await(10, TimeUnit.SECONDS));
    return result.get();
  }
  private void ready() throws Exception {
    getActivity();
    for (int i = 0; i < 100; i++) {
      if ("true".equals(js("typeof act === 'function' && document.body.innerText.includes('Your Week')"))) return;
      Thread.sleep(100);
    }
    fail("Bundled app did not load");
  }
  public void testStartupAndNavigation() throws Exception {
    ready();
    assertEquals("true", js("typeof AndroidBridge.saveBackup === 'function'"));
    assertEquals("true", js("act('calendar'); document.querySelector('.nav button.active').innerText.includes('Calendar')"));
    assertEquals("true", js("act('progress'); document.querySelector('h1').innerText === 'Progress'"));
    js("act('home')");
  }
  public void testCreateAndPersistProgram() throws Exception {
    ready();
    js("window.alert=()=>{}; act('create'); document.getElementById('name').value='Emulator test'; document.getElementById('goal').value='Practice daily'; act('saveCreate')");
    assertEquals("true", js("document.querySelector('h1').innerText === 'Emulator test'"));
    assertEquals("true", js("JSON.parse(localStorage.getItem('curriculum.app.v1')).programs.some(p=>p.name==='Emulator test')"));
    getInstrumentation().runOnMainSync(() -> web().reload());
    ready();
    assertEquals("true", js("document.body.innerText.includes('Emulator test')"));
  }
}
