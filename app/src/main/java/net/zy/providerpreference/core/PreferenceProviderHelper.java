package net.zy.providerpreference.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreferenceProviderHelper {

  private Context mContext;
  private String mAuthority;
  private Uri mBaseUri;
  private Map<String, Storage> mStores = new HashMap<>();

  private DataParcel mParcel = new DataParcel();

  private static final int MATCH_CODE_ITEM = 0x010000;
  private static final int MATCH_CODE_ITEMS = 0x100000;

  private UriMatcher mUriMatcher;

  public PreferenceProviderHelper(Context context, String authority) {
    this.mContext = context.getApplicationContext();
    this.mAuthority = authority;
    mBaseUri = Uri.parse("content://" + authority);
    mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    mUriMatcher.addURI(authority, "*/*", MATCH_CODE_ITEM);
    mUriMatcher.addURI(authority, "*", MATCH_CODE_ITEMS);
  }

  public void addStorage(String key, Storage store) {
    mStores.put(key, store);
  }

  public String getType(Uri uri) {
    switch (mUriMatcher.match(uri)) {
      case MATCH_CODE_ITEM:
        return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + mAuthority + ".item";
      case MATCH_CODE_ITEMS:
        return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + mAuthority + ".dir";
    }
    return null;
  }

  public int update(Uri uri, ContentValues contentValues) {
    SqlArgument arg = new SqlArgument(uri);
    Storage store = mStores.get(arg.table);
    if (store == null) {
      return 0;
    }
    int updated = 0;
    if (arg.data != null) {
      if (arg.data.isDelete()) {
        if (store.remove(arg.data.key)) {
          updated++;
        }
      } else if(store.put(arg.data.key, arg.data.value)) {
        updated++;
      }
      if (updated > 0) {
        mContext.getContentResolver().notifyChange(uri, null);
      }
    } else {
      if (contentValues == null || contentValues.size() == 0) {
        return 0;
      }
      boolean batch = contentValues.size() > 1;
      ArrayList<Uri> changedUris = new ArrayList<>(contentValues.size());
      try {
        if (batch) {
          store.beginTransaction();
        }
        List<DataParcel.Data> datas = mParcel.parseContentValues(contentValues);
        for (DataParcel.Data data : datas) {
          if (data.isDelete()) {
            if (store.remove(data.key)) {
              changedUris.add(mParcel.buildUri(uri, data.key, data.value));
              updated++;
            }
          } else if (store.put(data.key, data.value)) {
            changedUris.add(mParcel.buildUri(uri, data.key, data.value));
            updated++;
          }
        }
      } finally {
        if (batch) {
          store.endTransaction();
        }
      }
      for (Uri u : changedUris) {
        mContext.getContentResolver().notifyChange(u, null);
      }
    }
    return updated;
  }

  public Uri insert(Uri uri, ContentValues contentValues) {
    if (contentValues == null || contentValues.size() == 0) {
      return null;
    }
    SqlArgument arg = new SqlArgument(uri);
    Storage store = mStores.get(arg.table);
    if (store == null) {
      return null;
    }
    Map<String, Object> map = new HashMap<>(contentValues.size());
    for (String key : contentValues.keySet()) {
      map.put(key, contentValues.get(key));
    }
    store.clear();
    store.putAll(map);
    mContext.getContentResolver().notifyChange(uri, null);
    return uri;
  }

  public Cursor query(Uri uri) {
    SqlArgument arg = new SqlArgument(uri);
    Storage store = mStores.get(arg.table);
    if (store == null) {
      return null;
    }
    return mParcel.buildCursor(store.query());
  }

  /*
    valid form:
    1. table/key/type/value
    2. table
    3. table/key/type
  */
  private class SqlArgument {
    String table;
    DataParcel.Data data;

    SqlArgument(Uri uri) {
      List<String> paths = uri.getPathSegments();
      int len = paths.size();
      if (len < 1) {
        throw new IllegalArgumentException("Malformed uri");
      }
      table = paths.get(0);
      if (len > 1) {
        data = mParcel.parseUri(uri);
        if (data == null) {
          throw new IllegalArgumentException("Malformed uri");
        }
      }
    }
  }

  public interface Storage {
    Map<String, ?> query();
    boolean put(String key, Object value);
    int putAll(Map<String, Object> values);
    boolean remove(String key);
    void clear();
    void beginTransaction();
    void endTransaction();
  }

}
