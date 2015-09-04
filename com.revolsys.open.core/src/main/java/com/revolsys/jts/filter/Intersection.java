package com.revolsys.jts.filter;

import java.util.function.Predicate;

import com.revolsys.gis.graph.linestring.LineStringGraph;
import com.vividsolutions.jts.geom.LineString;

public class Intersection implements Predicate<LineString> {

  private final LineStringGraph graph;

  private final LineString line;

  public Intersection(final LineString line) {
    this.line = line;
    this.graph = new LineStringGraph(line);
  }

  public LineString getLine() {
    return this.line;
  }

  @Override
  public boolean test(final LineString line) {
    return this.graph.intersects(line);
  }
}
