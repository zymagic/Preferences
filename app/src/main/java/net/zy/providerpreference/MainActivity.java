package net.zy.providerpreference;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener, ServiceConnection {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    PrefUtils.getPref(this).registerOnSharedPreferenceChangeListener(this);
//    PrefUtils.getVar(this).registerOnSharedPreferenceChangeListener(this);
//    PrefUtils.check(this);
    PrefUtils.clear0(this);
    PrefUtils.clear1(this);
    android.util.Log.e("XXXXXX", "onCreate end");
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
//        PrefUtils.check(MainActivity.this);
        PrefUtils.clear2(MainActivity.this);
        android.util.Log.e("XXXXXX", "handler end");
      }
    }, 3000);
  }

  @Override
  protected void onResume() {
    super.onResume();
    bindService(new Intent(this, MyService.class), this, BIND_AUTO_CREATE);
  }

  @Override
  protected void onPause() {
    super.onPause();
    unbindService(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
    android.util.Log.e("XXXXX", "onPreferenceChanged " + sharedPreferences + ", " + s);
  }

  @Override
  public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

  }

  @Override
  public void onServiceDisconnected(ComponentName componentName) {

  }
}
