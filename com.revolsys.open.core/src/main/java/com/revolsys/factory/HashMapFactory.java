package com.revolsys.factory;

import java.util.HashMap;
import java.util.Map;

public class HashMapFactory<K, V> implements Factory<Map<K, V>> {
  @SuppressWarnings("rawtypes")
  private static final HashMapFactory INSTANCE = new HashMapFactory<>();

  @SuppressWarnings("unchecked")
  public static <K1, V1> HashMapFactory<K1, V1> get() {
    return INSTANCE;
  }

  @Override
  public Map<K, V> create() {
    return new HashMap<>();
  }
}