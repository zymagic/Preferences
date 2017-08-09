package net.zy.providerpreference;

import android.content.Context;
import android.content.SharedPreferences;

import net.zy.providerpreference.core.ProviderPreferences;

/**
 * Created by zy on 2017/8/9.
 */

public class PrefUtils {

  static SharedPreferences sPref;
  static SharedPreferences var;

  static SharedPreferences getPref(Context context) {
    if (sPref == null) {
      sPref = new ProviderPreferences(context, MyProvider.AUTHORITY, "bbb");
    }
    return sPref;
  }

  static SharedPreferences getVar(Context context) {
    if (var == null) {
      var = new ProviderPreferences(context, MyProvider.AUTHORITY, "var");
    }
    return var;
  }

  public static void check(Context context) {
    SharedPreferences sf = getPref(context);
    android.util.Log.e("XXXXXX", "get pid " + sf.getInt("pid", -100));
    sf.edit().putInt("pid", android.os.Process.myPid()).commit();
//    checkVar(context);
  }

  public static void clear0(Context context) {
    SharedPreferences sf = getPref(context);
    android.util.Log.e("XXXXXX", "get pidddd " + sf.getString("pidddd", "afafafa"));
    sf.edit().putString("pidddd", "ssss").commit();
//    checkVar(context);
  }

  public static void clear1(Context context) {
    SharedPreferences sf = getPref(context);
    android.util.Log.e("XXXXXX", "get pidddd " + sf.getString("pidddd", "afafafa"));
    sf.edit().putString("pidddd", null).commit();
//    checkVar(context);
  }

  public static void clear2(Context context) {
    SharedPreferences sf = getPref(context);
    android.util.Log.e("XXXXXX", "get pidddd " + sf.getString("pidddd", "afafafa"));
    sf.edit().remove("pidddd").commit();
//    checkVar(context);
  }

  public static void checkVar(Context context) {
    SharedPreferences sf = getVar(context);
    android.util.Log.e("XXXXXX", "get pids " + sf.getString("pids", "ffff"));
    sf.edit().putString("pids", android.os.Process.myPid() + "fff").commit();
  }
}
