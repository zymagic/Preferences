package net.zy.providerpreference.core;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataParcel {

  static final String TYPE_INTEGER = "integer";
  static final String TYPE_FLOAT = "float";
  static final String TYPE_LONG = "long";
  static final String TYPE_BOOLEAN = "boolean";
  static final String TYPE_STRING = "string";
  static final String TYPE_DELETE = "-[[delete]]-";

  static final String[] COLUMNS = {"key", "type", "value"};

  public static class Data {
    String key;
    String type;
    Object value;

    public boolean isDelete() {
      return TYPE_DELETE.equals(type);
    }
  }

  public Data parseUri(Uri uri) {
    if (uri == null) {
      return null;
    }

    Data data = new Data();

    if (parseUri(uri, data)) {
      return data;
    }

    return null;
  }

  public boolean parseUri(Uri uri, Data out) {
    if (uri == null || out == null) {
      return false;
    }

    List<String> segments = uri.getPathSegments();

    if (segments.size() != 4) {
      return false;
    }
    out.key = segments.get(1);
    out.type = segments.get(2);
    out.value = parseValue(out.type, segments.get(3));

    return true;
  }

  private Object parseValue(String type, String valueString) {
    switch (type) {
      case TYPE_INTEGER:
        return Integer.parseInt(valueString);
      case TYPE_FLOAT:
        return Float.parseFloat(valueString);
      case TYPE_LONG:
        return Long.parseLong(valueString);
      case TYPE_BOOLEAN:
        return Boolean.parseBoolean(valueString);
      case TYPE_STRING:
        if (TYPE_DELETE.equals(valueString)) {
          return null;
        }
        return valueString;
    }
    return null;
  }

  public Uri buildUri(Uri uri, String key, Object value) {
    return uri.buildUpon().appendPath(key).appendPath(getValueType(value)).appendPath(valueToString(value)).build();
  }

  private String getValueType(Object value) {
    if (value == null) {
      return TYPE_DELETE;
    } else if (value instanceof Integer) {
      return TYPE_INTEGER;
    } else if (value instanceof Long) {
      return TYPE_LONG;
    } else if (value instanceof Float) {
      return TYPE_FLOAT;
    } else if (value instanceof Boolean) {
      return TYPE_BOOLEAN;
    } else if (value instanceof String) {
      if (TYPE_DELETE.equals(value)) {
        return TYPE_DELETE;
      }
      return TYPE_STRING;
    }
    return TYPE_DELETE;
  }

  private String valueToString(Object value) {
    if (value == null) {
      return TYPE_DELETE;
    }
    return String.valueOf(value);
  }

  public Cursor buildCursor(Map<String, ?> map) {
    MatrixCursor cursor = new MatrixCursor(COLUMNS, map.size());
    for (Map.Entry<String, ?> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      String type = getValueType(value);
      cursor.newRow().add(key).add(type).add(valueToString(value));
    }
    return cursor;
  }

  public void parseCursor(Cursor cursor, Map<String, Object> map) {
    if (cursor == null || map == null) {
      return;
    }
    while (cursor.moveToNext()) {
      String key = cursor.getString(0);
      String type = cursor.getString(1);
      String valueString = cursor.getString(2);
      Object value = parseValue(type, valueString);
      if (TYPE_DELETE.equals(type)) {
        map.remove(key);
      } else {
        map.put(key, value);
      }
    }
  }

  public ContentValues mapContentValues(Map<String, Object> map) {
    ContentValues contentValues = new ContentValues(map.size());
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      insertContentValues(contentValues, entry.getKey(), entry.getValue());
    }
    return contentValues;
  }

  private void insertContentValues(ContentValues cv, String key, Object value) {
    if (value == null) {
      cv.put(key, TYPE_DELETE);
    } else if (value instanceof Integer) {
      cv.put(key, (Integer) value);
    } else if (value instanceof Float) {
      cv.put(key, (Float) value);
    } else if (value instanceof Long) {
      cv.put(key, (Long) value);
    } else if (value instanceof Boolean) {
      cv.put(key, (Boolean) value);
    } else if (value instanceof String) {
      if (TYPE_DELETE.equals(value)) {
        cv.put(key, TYPE_DELETE);
      } else {
        cv.put(key, (String) value);
      }
    } else {
      cv.put(key, TYPE_DELETE);
    }
  }

  public List<Data> parseContentValues(ContentValues cv) {
    ArrayList<Data> list = new ArrayList<>();
    if (cv == null || cv.size() == 0) {
      return list;
    }
    for (String key : cv.keySet()) {
      Object value = cv.get(key);
      Data data = new Data();
      data.key = key;
      data.type = getValueType(value);
      data.value = value;
      list.add(data);
    }
    return list;
  }

}
