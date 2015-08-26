package com.revolsys.visitor;

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DelegatingVisitor<T> extends AbstractVisitor<T> {
  private Consumer<T> consumer;

  public DelegatingVisitor() {
  }

  public DelegatingVisitor(final Comparator<T> comparator) {
    super(comparator);
  }

  public DelegatingVisitor(final Comparator<T> comparator, final Consumer<T> visitor) {
    super(comparator);
    this.consumer = visitor;
  }

  public DelegatingVisitor(final Consumer<T> visitor) {
    this.consumer = visitor;
  }

  public DelegatingVisitor(final Predicate<T> filter) {
    super(filter);
  }

  public DelegatingVisitor(final Predicate<T> filter, final Comparator<T> comparator) {
    super(filter, comparator);
  }

  public DelegatingVisitor(final Predicate<T> filter, final Comparator<T> comparator,
    final Consumer<T> visitor) {
    super(filter, comparator);
    this.consumer = visitor;
  }

  public DelegatingVisitor(final Predicate<T> filter, final Consumer<T> visitor) {
    super(filter);
    this.consumer = visitor;
  }

  @Override
  public void accept(final T item) {
    final Predicate<T> filter = getPredicate();
    if (filter == null || filter.test(item)) {
      this.consumer.accept(item);
    }
  }

  public Consumer<T> getVisitor() {
    return this.consumer;
  }

  public void setVisitor(final Consumer<T> visitor) {
    this.consumer = visitor;
  }

  @Override
  public String toString() {
    return this.consumer.toString();
  }
}
