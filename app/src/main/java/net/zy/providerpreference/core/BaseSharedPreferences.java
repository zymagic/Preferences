package net.zy.providerpreference.core;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class BaseSharedPreferences implements SharedPreferences {

  protected Map<String, Object> mData = new HashMap<>();
  private boolean mLoaded;
  private int mDiskWriteInFlight;
  private Object mWriteLock = new Object();
  private Executor mExecutor = Executors.newSingleThreadExecutor();

  private WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners = new WeakHashMap<>();

  public BaseSharedPreferences(Object... params) {
    init(params);
    load(false);
  }

  protected void init(Object... params) {
  }

  private void load(final boolean notify) {
    synchronized (this) {
      mLoaded = false;
    }
    new Thread("abs-preferences-load") {
      @Override
      public void run() {
        synchronized (BaseSharedPreferences.this) {
          loadLocked(notify);
        }
      }
    }.start();
  }

  private void loadLocked(boolean notify) {
    if (mLoaded) {
      return;
    }
    if (notify) {
      loadAndNotify();
    } else {
      normalLoad();
    }
    mLoaded = true;
    this.notifyAll();
  }

  private void normalLoad() {
    try {
      load(mData);
    } catch (Exception e) {
      mData.clear();
    }
  }

  private void loadAndNotify() {
    Map<String, Object> data = new HashMap<>();
    try {
      load(data);
      EditorImpl editor = (EditorImpl) edit();
      for (String key : mData.keySet()) {
        if (!data.containsKey(key)) {
          editor.remove(key);
        }
      }
      for (String key : data.keySet()) {
        editor.put(key, data.get(key));
      }
      editor.apply(false);
    } catch (Exception e) {
      // ignore
    }
  }

  protected abstract void load(Map<String, Object> data) throws Exception;

  protected final void onDataSetChanged() {
    load(true);
  }

  protected final void onDataChanged(String key, Object value, boolean remove) {
    EditorImpl editor = (EditorImpl) edit();
    if (remove) {
      editor.remove(key);
    } else {
      editor.put(key, value);
    }
    editor.apply(false);
  }

  private void awaitLoadedLocked() {
    while (!mLoaded) {
      try {
        wait();
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }

  @Override
  public Map<String, ?> getAll() {
    synchronized (this) {
      awaitLoadedLocked();
      return new HashMap<>(mData);
    }
  }

  @Nullable
  @Override
  public String getString(String key, @Nullable String defValue) {
    synchronized (this) {
      awaitLoadedLocked();
      Object ret = mData.get(key);
      return ret == null ? defValue : (String) ret;
    }
  }

  @Nullable
  @Override
  public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
    synchronized (this) {
      awaitLoadedLocked();
      Object ret = mData.get(key);
      return ret == null ? defValues : (Set<String>) ret;
    }
  }

  @Override
  public int getInt(String key, int defValue) {
    synchronized (this) {
      awaitLoadedLocked();
      Object ret = mData.get(key);
      return ret == null ? defValue : (Integer) ret;
    }
  }

  @Override
  public long getLong(String key, long defValue) {
    synchronized (this) {
      awaitLoadedLocked();
      Object ret = mData.get(key);
      return ret == null ? defValue : (Long) ret;
    }
  }

  @Override
  public float getFloat(String key, float defValue) {
    synchronized (this) {
      awaitLoadedLocked();
      Object ret = mData.get(key);
      return ret == null ? defValue : (Float) ret;
    }
  }

  @Override
  public boolean getBoolean(String key, boolean defValue) {
    synchronized (this) {
      awaitLoadedLocked();
      Object ret = mData.get(key);
      return ret == null ? defValue : (Boolean) ret;
    }
  }

  @Override
  public boolean contains(String key) {
    synchronized (this) {
      awaitLoadedLocked();
      return mData.containsKey(key);
    }
  }

  @Override
  public final Editor edit() {
    synchronized (this) {
      awaitLoadedLocked();
    }
    return new EditorImpl();
  }

  @Override
  public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    synchronized (this) {
      mListeners.put(listener, this);
    }
  }

  @Override
  public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    synchronized (this) {
      mListeners.remove(listener);
    }
  }

  private void notifyListeners(final MemoryCommitResult mcr) {
    if (!mcr.hasChange || mcr.changes.size() == 0
        || mcr.listeners == null || mcr.listeners.size() == 0) {
      return;
    }
    if (Looper.getMainLooper() == Looper.myLooper()) {
      for (String key : mcr.changes.keySet()) {
        notifyListeners(key, mcr.listeners);
      }
    } else {
      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          notifyListeners(mcr);
        }
      });
    }
  }

  private void notifyListeners(String key, Set<OnSharedPreferenceChangeListener> listeners) {
    for (OnSharedPreferenceChangeListener l : listeners) {
      if (l != null) {
        l.onSharedPreferenceChanged(this, key);
      }
    }
  }

  private void enqueDiskWrite(final MemoryCommitResult mcr, final boolean sync) {
    final Runnable writeToDiskRunnable = new Runnable() {
      @Override
      public void run() {
        synchronized (mWriteLock) {
          write(mcr);
        }
        synchronized (BaseSharedPreferences.this) {
          mDiskWriteInFlight--;
        }
        if (!sync) {
          try {
            mcr.latch.await();
          } catch (InterruptedException e) {
            // ignore
          }
        }
      }
    };
    if (sync) {
      boolean empty;
      synchronized (BaseSharedPreferences.this) {
        empty = mDiskWriteInFlight == 0;
      }
      if (empty) {
        writeToDiskRunnable.run();
        return;
      }
    }
    mExecutor.execute(writeToDiskRunnable);
  }

  private void write(MemoryCommitResult mcr) {
    if (!mcr.hasChange) {
      mcr.setResult(true);
      return;
    }
    try {
      boolean ret = write(mcr.mapToWrite, mcr.changes);
      mcr.setResult(ret);
    } catch (Exception e) {
      mcr.setResult(false);
    }
  }

  protected abstract boolean write(Map<String, Object> data, Map<String, Object> changes);

  private final class EditorImpl implements Editor {

    Map<String, Object> modified = new HashMap<>();
    boolean clear = false;

    @Override
    public Editor putString(String key, @Nullable String value) {
      synchronized (this) {
        modified.put(key, value);
        return this;
      }
    }

    @Override
    public Editor putStringSet(String key, @Nullable Set<String> values) {
      throw new UnsupportedOperationException("string set not supported yet");
    }

    @Override
    public Editor putInt(String key, int value) {
      synchronized (this) {
        modified.put(key, value);
        return this;
      }
    }

    @Override
    public Editor putLong(String key, long value) {
      synchronized (this) {
        modified.put(key, value);
        return this;
      }
    }

    @Override
    public Editor putFloat(String key, float value) {
      synchronized (this) {
        modified.put(key, value);
        return this;
      }
    }

    @Override
    public Editor putBoolean(String key, boolean value) {
      synchronized (this) {
        modified.put(key, value);
        return this;
      }
    }

    public Editor put(String key, Object value) {
      synchronized (this) {
        modified.put(key, value);
        return this;
      }
    }

    @Override
    public Editor remove(String key) {
      synchronized (this) {
        modified.put(key, this);
        return this;
      }
    }

    @Override
    public Editor clear() {
      synchronized (this) {
        clear = true;
        return this;
      }
    }

    @Override
    public boolean commit() {
      MemoryCommitResult mcr = commitToMemory();
      enqueDiskWrite(mcr, true);
      try {
        mcr.latch.await();
      } catch (InterruptedException e) {
        return false;
      }
      notifyListeners(mcr);
      return mcr.writeResult;
    }

    @Override
    public void apply() {
      apply(true);
    }

    protected final void apply(boolean persistant) {
      MemoryCommitResult mcr = commitToMemory();
      if (persistant) {
        enqueDiskWrite(mcr, false);
      }
      notifyListeners(mcr);
    }

    private MemoryCommitResult commitToMemory() {
      MemoryCommitResult mcr = new MemoryCommitResult();
      synchronized (BaseSharedPreferences.this) {
        if (mDiskWriteInFlight > 0) {
          mData = new HashMap<>(mData);
        }
        mcr.listeners = new HashSet<>(mListeners.keySet());
        synchronized (this) {
          mcr.mapToWrite = mData;
          mDiskWriteInFlight++;
          if (clear) {
            for (String key : mData.keySet()) {
              mcr.changes.put(key, this);
            }
            mData.clear();
            clear = false;
          }
          for (Map.Entry<String, Object> entry : modified.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (mData.containsKey(key) && BaseSharedPreferences.equals(value, mData.get(key))) {
              continue;
            }
            if (value == this || value == null) {
              if (!mData.containsKey(key)) {
                continue;
              }
              mData.remove(key);
              mcr.changes.put(key, this);
              mcr.hasChange = true;
            } else {
              mData.put(key, value);
              mcr.changes.put(key, value);
              mcr.hasChange = true;
            }
          }
          modified.clear();
        }
      }
      return mcr;
    }
  }

  private class MemoryCommitResult {
    boolean hasChange = false;
    Map<String, Object> mapToWrite;
    Map<String, Object> changes = new HashMap<>();
    Set<OnSharedPreferenceChangeListener> listeners;
    CountDownLatch latch = new CountDownLatch(1);
    boolean writeResult;

    void setResult(boolean result) {
      writeResult = result;
      latch.countDown();
    }
  }

  static boolean equals(Object a, Object b) {
    if (a == null) {
      return b == null;
    }
    return a.equals(b);
  }

}
