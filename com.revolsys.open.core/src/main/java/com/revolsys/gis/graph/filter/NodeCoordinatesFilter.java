package com.revolsys.gis.graph.filter;

import java.util.function.Predicate;

import com.revolsys.gis.graph.Node;
import com.revolsys.gis.model.coordinates.Coordinates;

public class NodeCoordinatesFilter<T> implements Predicate<Node<T>> {
  private Predicate<Coordinates> predicate;

  public NodeCoordinatesFilter() {
  }

  public NodeCoordinatesFilter(final Predicate<Coordinates> filter) {
    this.predicate = filter;
  }

  public Predicate<Coordinates> getFilter() {
    return this.predicate;
  }

  public void setFilter(final Predicate<Coordinates> filter) {
    this.predicate = filter;
  }

  @Override
  public boolean test(final Node<T> node) {
    return this.predicate.test(node);
  }
}
