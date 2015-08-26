package com.revolsys.predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class AndFilter<T> implements Predicate<T> {
  private final List<Predicate<T>> predicates = new ArrayList<Predicate<T>>();

  public AndFilter() {
  }

  public AndFilter(final Collection<Predicate<T>> filters) {
    this.predicates.addAll(filters);
  }

  public AndFilter(final Predicate<T>... filters) {
    this(Arrays.asList(filters));
  }

  public void addFilter(final Predicate<T> filter) {
    this.predicates.add(filter);
  }

  @Override
  public boolean test(final T object) {
    for (final Predicate<T> filter : this.predicates) {
      final boolean accept = filter.test(object);
      if (!accept) {

        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return "AND" + this.predicates;
  }
}
