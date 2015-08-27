package com.revolsys.gis.graph.filter;

import com.revolsys.gis.graph.Edge;
import com.revolsys.gis.graph.Node;
import java.util.function.Predicate;

public class EdgeHasNodeFilter<T> implements Predicate<Edge<T>> {
  private final Node<T> node;

  public EdgeHasNodeFilter(final Node<T> node) {
    this.node = node;
  }

  @Override
  public boolean test(final Edge<T> edge) {
    return edge.hasNode(this.node);
  }
}
