package net.zy.providerpreference.core;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

import java.util.Map;

public class ProviderPreferences extends BaseSharedPreferences {

  private Context mContext;
  private ContentProviderClient mProvider;
  private DataParcel mParcel;
  private Uri mUri;
  private String mInstanceKey;

  public ProviderPreferences(Context context, String authority, String table) {
    super(context, authority, table);
  }

  @Override
  protected void init(Object... params) {
    mInstanceKey = Integer.toString(android.os.Process.myPid());
    mContext = ((Context) params[0]).getApplicationContext();
    mUri = Uri.parse("content://" + params[1] + "/" + params[2] + "?ink=" + mInstanceKey);
    mParcel = new DataParcel();
    mContext.getContentResolver().registerContentObserver(mUri, true, new ContentObserver(new Handler(Looper.getMainLooper())) {

      @Override
      public void onChange(boolean selfChange) {
        onChange(selfChange, null);
      }

      @Override
      public void onChange(boolean selfChange, Uri uri) {
        onChanged(uri);
      }
    });
  }

  @Override
  protected void load(Map<String, Object> data) throws Exception {
    ContentProviderClient provider = lazyGetProvider();
    if (provider == null) {
      return;
    }
    Cursor cursor = provider.query(mUri, null, null, null, null);
    mParcel.parseCursor(cursor, data);
  }

  private void onChanged(Uri uri) {
    if (uri == null || uri.getPathSegments().size() != 4) {
      onDataSetChanged();
      return;
    }

    String param = uri.getQueryParameter("ink");
    if (mInstanceKey.equals(param)) {
      return;
    }

    DataParcel.Data data = mParcel.parseUri(uri);
    if (data == null) {
      return;
    }

    onDataChanged(data.key, data.value, data.isDelete());
  }

  @Override
  protected boolean write(Map<String, Object> data, Map<String, Object> changes) {
    ContentProviderClient client = lazyGetProvider();
    try {
      if (changes.size() <= data.size()) {
        int updated = client.update(mUri, mParcel.mapContentValues(changes), null, null);
        return updated > 0;
      } else {
        Uri uri = client.insert(mUri, mParcel.mapContentValues(data));
        return uri != null;
      }
    } catch (RemoteException e) {
      // ignore
    }
    return false;
  }

  private ContentProviderClient lazyGetProvider() {
    if (mProvider == null) {
      mProvider = mContext.getContentResolver().acquireContentProviderClient(mUri);
    }
    return mProvider;
  }

}
