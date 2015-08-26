package com.revolsys.predicate;

import java.util.function.Predicate;

public class FilterAndValue<F, V> implements Predicate<F> {
  private Predicate<F> predicate;

  private V value;

  public FilterAndValue(final Predicate<F> filter, final V value) {
    this.predicate = filter;
    this.value = value;
  }

  public Predicate<F> getFilter() {
    return this.predicate;
  }

  public V getValue() {
    return this.value;
  }

  public void setFilter(final Predicate<F> filter) {
    this.predicate = filter;
  }

  public void setValue(final V value) {
    this.value = value;
  }

  @Override
  public boolean test(final F object) {
    return this.predicate.test(object);
  }

  @Override
  public String toString() {
    return "filter=" + this.predicate + "\nvalue=" + this.value;
  }
}
