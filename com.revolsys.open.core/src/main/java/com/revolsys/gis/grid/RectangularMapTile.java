package com.revolsys.gis.grid;

import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public interface RectangularMapTile {
  BoundingBox getBoundingBox();

  String getFormattedName();

  RectangularMapGrid getGrid();

  String getName();

  Polygon getPolygon(GeometryFactory factory, int numPoints);

  Polygon getPolygon(GeometryFactory factory, final int numXPoints,
    final int numYPoints);

  Polygon getPolygon(int numPoints);

  Polygon getPolygon(final int numXPoints, final int numYPoints);
}
