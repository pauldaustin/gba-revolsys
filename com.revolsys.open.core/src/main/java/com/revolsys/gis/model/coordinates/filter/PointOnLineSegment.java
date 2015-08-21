package com.revolsys.gis.model.coordinates.filter;

import com.revolsys.filter.Filter;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.LineSegmentUtil;
import com.revolsys.jts.geom.LineSegment;

public class PointOnLineSegment implements Filter<Coordinates> {

  private final LineSegment lineSegment;

  private final double maxDistance;

  public PointOnLineSegment(final LineSegment lineSegment, final double maxDistance) {
    this.lineSegment = lineSegment;
    this.maxDistance = maxDistance;
  }

  @Override
  public boolean accept(final Coordinates point) {
    final Coordinates start = this.lineSegment.get(0);
    final Coordinates end = this.lineSegment.get(1);
    final boolean onLine = LineSegmentUtil.isPointOnLine(start, end, point, this.maxDistance);
    return onLine;
  }
}
