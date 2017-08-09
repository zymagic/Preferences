package net.zy.providerpreference;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by zy on 2017/8/9.
 */

public class MyService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener{

  @Override
  public void onCreate() {
    super.onCreate();
    PrefUtils.getPref(this).registerOnSharedPreferenceChangeListener(this);
//    PrefUtils.getVar(this).registerOnSharedPreferenceChangeListener(this);
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    PrefUtils.clear0(this);
    Log.e("XXXXXXX", "service bind");
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    PrefUtils.clear0(this);
    Log.e("XXXXXXX", "service start");
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
    Log.e("XXXXXXX", "service preference change " + sharedPreferences + ", " + s);
  }
}
