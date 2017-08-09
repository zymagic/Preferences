package net.zy.providerpreference.core;

import java.util.HashMap;
import java.util.Map;

public class MemoryStorage implements PreferenceProviderHelper.Storage {

  Map<String, Object> map = new HashMap<>();

  @Override
  public Map<String, ?> query() {
    return new HashMap<>(map);
  }

  @Override
  public boolean put(String key, Object value) {
    map.put(key, value);
    return true;
  }

  @Override
  public int putAll(Map<String, Object> values) {
    map.putAll(values);
    return values.size();
  }

  @Override
  public boolean remove(String key) {
    Object removed = map.remove(key);
    return removed != null;
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public void beginTransaction() {
    // ignore
  }

  @Override
  public void endTransaction() {
    // ignore
  }
}
