package com.revolsys.gis.jts;

import java.util.function.Consumer;

import com.revolsys.gis.algorithm.locate.RayCrossingCounter;
import com.revolsys.gis.model.coordinates.LineSegmentUtil;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.LineSegment;

public class PointInArea extends RayCrossingCounter implements Consumer<LineSegment> {

  private final GeometryFactory geometryFactory;

  public PointInArea(final GeometryFactory geometryFactory, final double x, final double y) {
    super(x, y);
    this.geometryFactory = geometryFactory;
  }

  @Override
  public void accept(final LineSegment segment) {
    final double x1 = segment.getX(0);
    final double y1 = segment.getY(0);
    final double x2 = segment.getX(1);
    final double y2 = segment.getY(1);
    final double x = getX();
    final double y = getY();
    if (!this.geometryFactory.isFloating()) {
      final double distance = LineSegmentUtil.distance(x1, y1, x2, y2, x, y);
      final double minDistance = 1.0 / this.geometryFactory.getScaleXY();
      if (distance < minDistance) {
        setPointOnSegment(true);
      }
    }
    countSegment(x1, y1, x2, y2);
  }
}
