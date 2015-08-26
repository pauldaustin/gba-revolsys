package com.revolsys.predicate;

public interface Factory<T, V> {
  T create(V object);
}
