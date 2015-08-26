package com.revolsys.predicate;

import java.util.function.Predicate;

public class AcceptAllFilter<T> implements Predicate<T> {
  @Override
  public boolean test(final T object) {
    return true;
  }
}
