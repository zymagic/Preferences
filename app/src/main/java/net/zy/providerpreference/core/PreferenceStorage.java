package net.zy.providerpreference.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

public class PreferenceStorage implements PreferenceProviderHelper.Storage {

  SharedPreferences mPref;
  SharedPreferences.Editor mTransactionEditor;

  public PreferenceStorage(Context context, String name) {
    mPref = context.getSharedPreferences(name, Context.MODE_PRIVATE);
  }

  @Override
  public Map<String, ?> query() {
    return mPref.getAll();
  }

  @Override
  public boolean put(String key, Object value) {
    boolean isInTransaction = mTransactionEditor != null;
    SharedPreferences.Editor editor = isInTransaction ? mTransactionEditor : mPref.edit();
    boolean ret = putObject(editor, key, value);
    if (!isInTransaction) {
      editor.commit();
    }
    return ret;
  }

  @Override
  public int putAll(Map<String, Object> values) {
    boolean isInTransaction = mTransactionEditor != null;
    SharedPreferences.Editor editor = isInTransaction ? mTransactionEditor : mPref.edit();
    int count = 0;
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      if (putObject(editor, entry.getKey(), entry.getValue())) {
        count++;
      }
    }
    if (!isInTransaction) {
      editor.commit();
    }
    return count;
  }

  @Override
  public boolean remove(String key) {
    boolean isInTransaction = mTransactionEditor != null;
    SharedPreferences.Editor editor = isInTransaction ? mTransactionEditor : mPref.edit();
    boolean ret = mPref.contains(key);
    editor.remove(key);
    if (!isInTransaction) {
      editor.commit();
    }
    return ret;
  }

  @Override
  public void clear() {
    boolean isInTransaction = mTransactionEditor != null;
    SharedPreferences.Editor editor = isInTransaction ? mTransactionEditor : mPref.edit();
    editor.clear();
    if (!isInTransaction) {
      editor.commit();
    }
  }

  @Override
  public void beginTransaction() {
    if (mTransactionEditor == null) {
      mTransactionEditor = mPref.edit();
    }
  }

  @Override
  public void endTransaction() {
    if (mTransactionEditor != null) {
      mTransactionEditor.commit();
    }
  }

  private boolean putObject(SharedPreferences.Editor editor, String key, Object value) {
    if (value == null) {
      editor.putString(key, null);
    } else if (value instanceof Integer) {
      editor.putInt(key, (Integer) value);
    } else if (value instanceof Float) {
      editor.putFloat(key, (Float) value);
    } else if (value instanceof Long) {
      editor.putLong(key, (Long) value);
    } else if (value instanceof Boolean) {
      editor.putBoolean(key, (Boolean) value);
    } else if (value instanceof String) {
      editor.putString(key, (String) value);
    } else {
      return false;
    }
    return true;
  }
}
