package com.revolsys.gis.algorithm.locate;

import java.util.function.Consumer;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

public class IndexedPointInAreaLocator implements PointOnGeometryLocator {

  private static class IntervalIndexedGeometry {
    private final SortedPackedIntervalRTree<LineSegment> index = new SortedPackedIntervalRTree<LineSegment>();

    public IntervalIndexedGeometry(final Polygon geom) {
      init(geom);
    }

    private void addLine(final CoordinateSequence points) {
      final int size = points.size();
      if (size > 1) {
        for (int i = 1; i < size; i++) {
          final double x1 = points.getX(i - 1);
          final double x2 = points.getX(i);
          final double y1 = points.getY(i - 1);
          final double y2 = points.getY(i);
          final LineSegment seg = new LineSegment(x1, y1, x2, y2);
          final double min = Math.min(y1, y2);
          final double max = Math.max(y1, y2);
          this.index.insert(min, max, seg);
        }
      }
    }

    private void init(final Polygon polygon) {
      addLine(polygon.getExteriorRing().getCoordinateSequence());
      for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
        final LineString ring = polygon.getInteriorRingN(i);
        addLine(ring.getCoordinateSequence());
      }
    }

    public void query(final double min, final double max, final Consumer<LineSegment> visitor) {
      this.index.query(min, max, visitor);
    }
  }

  private final Polygon geometry;

  private final IntervalIndexedGeometry index;

  /**
   * Creates a netor for a given {@link Geometry}
   *
   * @param geometry the Geometry to locate in
   */
  public IndexedPointInAreaLocator(final Polygon geometry) {
    this.geometry = geometry;
    this.index = new IntervalIndexedGeometry(geometry);
  }

  public Polygon getGeometry() {
    return this.geometry;
  }

  public GeometryFactory getGeometryFactory() {
    return GeometryFactory.get(this.geometry);
  }

  public IntervalIndexedGeometry getIndex() {
    return this.index;
  }

  @Override
  public Location locate(final Coordinates coordinates) {
    return locate(coordinates.getX(), coordinates.getY());
  }

  @Override
  public Location locate(final double x, final double y) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    final PointInArea visitor = new PointInArea(geometryFactory, x, y);
    this.index.query(y, y, visitor);

    return visitor.getLocation();
  }

}
