package com.revolsys.jts.filter;

import java.util.function.Predicate;

import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.jts.geom.LineSegment;

public class LineSegmentCoordinateDistanceFilter implements Predicate<LineSegment> {

  private final double maxDistance;

  private final Coordinates point;

  public LineSegmentCoordinateDistanceFilter(final Coordinates point, final double maxDistance) {
    this.point = point;
    this.maxDistance = maxDistance;
  }

  @Override
  public boolean test(final LineSegment lineSegment) {
    final double distance = lineSegment.distance(this.point);
    if (distance < this.maxDistance) {
      return true;
    } else {
      return false;
    }
  }

}
