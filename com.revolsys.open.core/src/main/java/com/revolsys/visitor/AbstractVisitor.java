package com.revolsys.visitor;

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.revolsys.comparator.ComparatorProxy;
import com.revolsys.predicate.AndPredicate;
import com.revolsys.predicate.PredicateProxy;

public abstract class AbstractVisitor<T>
  implements Consumer<T>, PredicateProxy<T>, ComparatorProxy<T> {
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
  public Predicate<T> getPredicate() {
    return this.predicate;
  }

  public void setComparator(final Comparator<T> comparator) {
    this.comparator = comparator;
  }

  public void setFilter(final Predicate<T> filter) {
    this.predicate = filter;
  }

  public void setFilters(final Predicate<T>... filters) {
    this.predicate = new AndPredicate<T>(filters);
  }
}
