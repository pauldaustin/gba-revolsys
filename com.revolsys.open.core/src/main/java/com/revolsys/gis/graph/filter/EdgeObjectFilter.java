package com.revolsys.gis.graph.filter;

import java.util.function.Predicate;

import com.revolsys.gis.graph.Edge;

public class EdgeObjectFilter<T> implements Predicate<Edge<T>> {
  private Predicate<T> predicate;

  public EdgeObjectFilter() {
  }

  public EdgeObjectFilter(final Predicate<T> filter) {
    this.predicate = filter;
  }

  public Predicate<T> getFilter() {
    return this.predicate;
  }

  public void setFilter(final Predicate<T> filter) {
    this.predicate = filter;
  }

  @Override
  public boolean test(final Edge<T> edge) {
    final T object = edge.getObject();
    return this.predicate.test(object);
  }
}
