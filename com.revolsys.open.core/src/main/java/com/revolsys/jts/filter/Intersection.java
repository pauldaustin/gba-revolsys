package com.revolsys.jts.filter;

import com.revolsys.filter.Filter;
import com.revolsys.gis.graph.linestring.LineStringGraph;
import com.vividsolutions.jts.geom.LineString;

public class Intersection implements Filter<LineString> {

  private final LineStringGraph graph;

  private final LineString line;

  public Intersection(final LineString line) {
    this.line = line;
    this.graph = new LineStringGraph(line);
  }

  @Override
  public boolean accept(final LineString line) {
    return this.graph.intersects(line);
  }

  public LineString getLine() {
    return this.line;
  }
}
