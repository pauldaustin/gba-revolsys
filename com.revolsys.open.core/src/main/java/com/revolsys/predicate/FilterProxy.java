package com.revolsys.predicate;

import java.util.function.Predicate;

public interface FilterProxy<T> {
  Predicate<T> getFilter();
}
