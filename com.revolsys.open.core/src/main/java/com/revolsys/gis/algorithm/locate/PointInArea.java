package com.revolsys.gis.algorithm.locate;

import com.revolsys.collection.Visitor;
import com.revolsys.gis.model.coordinates.LineSegmentUtil;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.LineSegment;

public class PointInArea extends RayCrossingCounter implements Visitor<LineSegment> {

  private final double minDistance;

  public PointInArea(final GeometryFactory geometryFactory, final double x, final double y) {
    super(x, y);
    if (geometryFactory != null && !geometryFactory.isFloating()) {
      this.minDistance = 1 / geometryFactory.getScaleXY();
    } else {
      this.minDistance = Double.NaN;
    }
  }

  @Override
  public boolean visit(final LineSegment segment) {
    final double x1 = segment.getX(0);
    final double y1 = segment.getY(0);
    final double x2 = segment.getX(1);
    final double y2 = segment.getY(1);
    if (!Double.isNaN(this.minDistance)) {
      final double x = getX();
      final double y = getY();
      final double distance = LineSegmentUtil.distanceLinePoint(x1, y1, x2, y2, x, y);
      if (distance < this.minDistance) {
        setPointOnSegment(true);
        return true;
      }
    }
    countSegment(x1, y1, x2, y2);
    return true;
  }
}
