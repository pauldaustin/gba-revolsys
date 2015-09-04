package com.revolsys.gis.graph.linestring;

import java.util.function.Predicate;

import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.jts.geom.LineSegment;

public class LineSegmentIntersectingFilter implements Predicate<LineSegment> {

  private final LineSegment line;

  public LineSegmentIntersectingFilter(final LineSegment line) {
    this.line = line;
  }

  @Override
  public boolean test(final LineSegment line) {
    if (line == this.line) {
      return false;
    } else {
      final CoordinatesList intersection = this.line.getIntersection(line);
      return intersection != null && intersection.size() > 0;
    }
  }
}
