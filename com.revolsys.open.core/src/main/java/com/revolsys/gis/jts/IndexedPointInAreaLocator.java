package com.revolsys.gis.jts;

import java.util.function.Consumer;
import com.revolsys.gis.algorithm.locate.Location;
import com.revolsys.gis.algorithm.locate.PointOnGeometryLocator;
import com.revolsys.gis.algorithm.locate.SortedPackedIntervalRTree;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Geometry;

public class IndexedPointInAreaLocator implements PointOnGeometryLocator {

  private static class IntervalIndexedGeometry {
    private final SortedPackedIntervalRTree<LineSegment> index = new SortedPackedIntervalRTree<LineSegment>();

    public IntervalIndexedGeometry(final Geometry geom) {
      init(geom);
    }

    private void addLine(final CoordinatesList points) {
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

    private void init(final Geometry geometry) {
      for (final CoordinatesList points : CoordinatesListUtil.getAll(geometry)) {
        addLine(points);
      }
    }

    public void query(final double min, final double max, final Consumer<LineSegment> visitor) {
      this.index.query(min, max, visitor);
    }
  }

  private static final String KEY = IndexedPointInAreaLocator.class.getName();

  public static PointOnGeometryLocator get(final Geometry geometry) {
    PointOnGeometryLocator locator = JtsGeometryUtil.getGeometryProperty(geometry, KEY);
    if (locator == null) {
      locator = new IndexedPointInAreaLocator(geometry);
      JtsGeometryUtil.setGeometryProperty(geometry, KEY, locator);
    }
    return locator;
  }

  private final Geometry geometry;

  private final IntervalIndexedGeometry index;

  /**
   * Creates a new locator for a given {@link Geometry}
   *
   * @param geometry the Geometry to locate in
   */
  public IndexedPointInAreaLocator(final Geometry geometry) {
    this.geometry = geometry;
    this.index = new IntervalIndexedGeometry(geometry);
  }

  public Geometry getGeometry() {
    return this.geometry;
  }

  public GeometryFactory getGeometryFactory() {
    return GeometryFactory.getFactory(this.geometry);
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
    double resolutionXy = geometryFactory.getScaleXY();
    if (resolutionXy != 0) {
      resolutionXy = 1 / resolutionXy;
    }
    final double minY = y - resolutionXy;
    final double maxY = y + resolutionXy;

    final PointInArea visitor = new PointInArea(geometryFactory, x, y);
    this.index.query(minY, maxY, visitor);

    return visitor.getLocation();
  }

}
