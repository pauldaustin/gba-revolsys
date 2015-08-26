package com.revolsys.visitor;

import java.util.Comparator;

import com.revolsys.collection.Visitor;
import com.revolsys.comparator.ComparatorProxy;
import com.revolsys.predicate.AndFilter;
import com.revolsys.predicate.FilterProxy;
import java.util.function.Predicate;

public abstract class AbstractVisitor<T> implements Visitor<T>, FilterProxy<T>, ComparatorProxy<T> {
  private Comparator<T> comparator;

  private Predicate<T> predicate;

  public AbstractVisitor() {
  }

  public AbstractVisitor(final Comparator<T> comparator) {
    this.comparator = comparator;
  }

  public AbstractVisitor(final Predicate<T> filter) {
    this.predicate = filter;
  }

  public AbstractVisitor(final Predicate<T> filter, final Comparator<T> comparator) {
    this.predicate = filter;
    this.comparator = comparator;
  }

  @Override
  public Comparator<T> getComparator() {
    return this.comparator;
  }

  @Override
  public Predicate<T> getFilter() {
    return this.predicate;
  }

  public void setComparator(final Comparator<T> comparator) {
    this.comparator = comparator;
  }

  public void setFilter(final Predicate<T> filter) {
    this.predicate = filter;
  }

  public void setFilters(final Predicate<T>... filters) {
    this.predicate = new AndFilter<T>(filters);
  }
}
