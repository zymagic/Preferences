package net.zy.providerpreference;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.zy.providerpreference.core.MemoryStorage;
import net.zy.providerpreference.core.PreferenceProviderHelper;
import net.zy.providerpreference.core.PreferenceStorage;

/**
 * Created by zy on 2017/8/9.
 */

public class MyProvider extends ContentProvider {

  public static final String AUTHORITY = "zy.preference";

  private PreferenceProviderHelper mHelper;

  @Override
  public boolean onCreate() {
    mHelper = new PreferenceProviderHelper(getContext(), AUTHORITY);
    mHelper.addStorage("bbb", new PreferenceStorage(getContext(), "bbb"));
    mHelper.addStorage("var", new MemoryStorage());
    return true;
  }

  @Nullable
  @Override
  public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
    return mHelper.query(uri);
  }

  @Nullable
  @Override
  public String getType(@NonNull Uri uri) {
    return mHelper.getType(uri);
  }

  @Nullable
  @Override
  public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
    return mHelper.insert(uri, contentValues);
  }

  @Override
  public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
    return 0;
  }

  @Override
  public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
    return mHelper.update(uri, contentValues);
  }
}
