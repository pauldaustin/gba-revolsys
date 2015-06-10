package com.revolsys.collection.map;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.revolsys.collection.ReferenceEntrySet;
import com.revolsys.collection.ReferenceSet;

public class WeakCache<K, V> implements Map<K, V> {
  private final Map<K, Reference<V>> cache = new WeakHashMap<K, Reference<V>>();

  private final Map<K, V> map;

  public WeakCache() {
    this(null);
  }

  public WeakCache(final Map<K, V> map) {
    this.map = map;
  }

  @Override
  public void clear() {
    this.cache.clear();
  }

  @Override
  public boolean containsKey(final Object obj) {
    if (this.map == null) {
      return this.cache.containsKey(obj);
    } else {
      return this.map.containsKey(obj);
    }
  }

  @Override
  public boolean containsValue(final Object value) {
    if (this.map == null) {
      return this.cache.containsKey(value);
    } else {
      return this.map.containsKey(value);
    }
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    if (this.map == null) {
      return new ReferenceEntrySet<K, V>(this.cache.entrySet());
    } else {
      throw new UnsupportedOperationException();
    }
  }

  public void evict(final K key) {
    this.cache.remove(key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public V get(final Object key) {
    V value = null;
    final Reference<V> reference = this.cache.get(key);
    if (reference != null) {
      value = reference.get();
    }
    if (value == null) {
      if (this.map != null) {
        value = this.map.get(key);
      }
      if (value == null) {
        this.cache.remove(key);
      } else {
        this.cache.put((K)key, new WeakReference<V>(value));
      }
    }
    return value;
  }

  @Override
  public boolean isEmpty() {
    if (this.map == null) {
      return this.cache.isEmpty();
    } else {
      return this.map.isEmpty();
    }
  }

  @Override
  public Set<K> keySet() {
    if (this.map == null) {
      return this.cache.keySet();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public V put(final K key, final V value) {
    V oldValue = null;
    if (value == null) {
      final Reference<V> oldReference = this.cache.remove(key);

      if (this.map == null) {
        if (oldReference != null) {
          oldValue = oldReference.get();
        }
      } else {
        oldValue = this.map.remove(key);
      }
    } else {
      final Reference<V> oldReference = this.cache.put(key, new WeakReference<V>(value));
      if (this.map == null) {
        if (oldReference != null) {
          oldValue = oldReference.get();
        }
      } else {
        oldValue = this.map.put(key, value);
      }

    }
    return oldValue;
  }

  @Override
  public void putAll(final Map<? extends K, ? extends V> map) {
    for (final Entry<? extends K, ? extends V> entry : map.entrySet()) {
      final K key = entry.getKey();
      final V value = entry.getValue();
      put(key, value);
    }
  }

  @Override
  public V remove(final Object obj) {
    final Reference<V> oldReference = this.cache.remove(obj);
    if (this.map == null) {
      return oldReference.get();
    } else {
      return this.map.remove(obj);
    }
  }

  @Override
  public int size() {
    if (this.map == null) {
      return this.cache.size();
    } else {
      return this.map.size();
    }
  }

  @Override
  public Collection<V> values() {
    if (this.map == null) {
      return new ReferenceSet<V>(this.cache.values());
    } else {
      throw new UnsupportedOperationException();
    }
  }
}
