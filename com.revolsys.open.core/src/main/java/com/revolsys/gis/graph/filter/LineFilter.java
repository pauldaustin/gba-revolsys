package com.revolsys.gis.graph.filter;

import java.util.function.Predicate;

import com.revolsys.gis.graph.Edge;
import com.vividsolutions.jts.geom.LineString;

public class LineFilter<T> implements Predicate<Edge<T>> {
  private final Predicate<LineString> predicate;

  public LineFilter(final Predicate<LineString> filter) {
    this.predicate = filter;
  }

  @Override
  public boolean test(final Edge<T> edge) {
    final LineString line = edge.getLine();
    return this.predicate.test(line);
  }

}
