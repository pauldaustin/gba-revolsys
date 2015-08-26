package com.revolsys.predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class OrFilter<T> implements Predicate<T> {
  private List<Predicate<T>> predicates = new ArrayList<Predicate<T>>();

  public OrFilter() {
  }

  public OrFilter(final Collection<Predicate<T>> filters) {
    this.predicates.addAll(filters);
  }

  public OrFilter(final Predicate<T>... filters) {
    this(Arrays.asList(filters));
  }

  public List<Predicate<T>> getFilters() {
    return this.predicates;
  }

  public void setFilters(final List<Predicate<T>> filters) {
    this.predicates = filters;
  }

  @Override
  public boolean test(final T object) {
    for (final Predicate<T> filter : this.predicates) {
      if (filter.test(object)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "OR" + this.predicates;
  }
}
