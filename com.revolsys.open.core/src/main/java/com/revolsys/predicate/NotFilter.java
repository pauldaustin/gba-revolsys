package com.revolsys.predicate;

import java.util.function.Predicate;

public class NotFilter<T> implements Predicate<T> {
  private Predicate<T> predicate;

  public NotFilter() {
  }

  public NotFilter(final Predicate<T> filter) {
    this.predicate = filter;
  }

  public Predicate<T> getFilter() {
    return this.predicate;
  }

  public void setFilter(final Predicate<T> filter) {
    this.predicate = filter;
  }

  @Override
  public boolean test(final T object) {
    return !this.predicate.test(object);
  }

  @Override
  public String toString() {
    return "NOT " + this.predicate;
  }
}
