package com.revolsys.gis.algorithm.index.quadtree.linesegment;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.jts.filter.LineSegmentCoordinateDistanceFilter;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryEditUtil;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.LineSegment;
import com.revolsys.util.ExitLoopException;
import com.revolsys.visitor.CreateListVisitor;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class LineSegmentQuadTree {
  public static Envelope ensureExtent(final Envelope envelope, final double minExtent) {
    double minX = envelope.getMinX();
    double maxX = envelope.getMaxX();
    double minY = envelope.getMinY();
    double maxY = envelope.getMaxY();
    if (minX != maxX && minY != maxY) {
      return envelope;
    }

    if (minX == maxX) {
      minX = minX - minExtent / 2.0;
      maxX = minX + minExtent / 2.0;
    }
    if (minY == maxY) {
      minY = minY - minExtent / 2.0;
      maxY = minY + minExtent / 2.0;
    }
    return new Envelope(minX, maxX, minY, maxY);
  }

  private final Geometry geometry;

  private double minExtent = 1.0;

  private final Root root = new Root();

  private int size = 0;

  public LineSegmentQuadTree(final Geometry geometry) {
    this.geometry = geometry;
    if (geometry == null || geometry.isEmpty()) {
    } else if (geometry instanceof Point) {
    } else if (geometry instanceof MultiPoint) {
    } else if (geometry instanceof LineString) {
      final CoordinatesList points = CoordinatesListUtil.get(geometry);
      add(points);
    } else if (geometry instanceof Polygon) {
      final Polygon polygon = (Polygon)geometry;
      final List<CoordinatesList> rings = CoordinatesListUtil.getAll(polygon);
      for (int ringIndex = 0; ringIndex < rings.size(); ringIndex++) {
        final CoordinatesList points = rings.get(ringIndex);
        add(points, ringIndex);
      }
    } else {
      for (int partIndex = 0; partIndex < geometry.getNumGeometries(); partIndex++) {
        final Geometry part = geometry.getGeometryN(partIndex);
        if (part instanceof Point) {
        } else if (part instanceof LineString) {
          final LineString line = (LineString)part;
          final CoordinatesList points = CoordinatesListUtil.get(line);
          add(points, partIndex);
        } else if (part instanceof Polygon) {
          final Polygon polygon = (Polygon)part;
          final List<CoordinatesList> rings = CoordinatesListUtil.getAll(polygon);
          for (int ringIndex = 0; ringIndex < rings.size(); ringIndex++) {
            final CoordinatesList points = rings.get(ringIndex);
            add(points, partIndex, ringIndex);
          }
        }
      }
    }
  }

  private void add(final CoordinatesList points, final int... parentIndex) {
    double x1 = points.getX(0);
    double y1 = points.getY(0);
    for (int segmentIndex = 0; segmentIndex < points.size() - 1; segmentIndex++) {
      final int segmentEndVertexIndex = segmentIndex + 1;
      final double x2 = points.getX(segmentEndVertexIndex);
      final double y2 = points.getY(segmentEndVertexIndex);
      final int[] index = GeometryEditUtil.createVertexIndex(parentIndex, segmentIndex);
      final Envelope envelope = new Envelope(x1, x2, y1, y2);
      insert(envelope, index);
      x1 = x2;
      y1 = y2;
    }
  }

  private void collectStats(final Envelope envelope) {
    final double width = envelope.getWidth();
    if (width < this.minExtent && width > 0.0) {
      this.minExtent = width;
    }

    final double height = envelope.getHeight();
    if (height < this.minExtent && height > 0.0) {
      this.minExtent = height;
    }
  }

  public int depth() {
    return this.root.depth();
  }

  public List<LineSegment> getAll() {
    final CreateListVisitor<LineSegment> visitor = new CreateListVisitor<LineSegment>();
    this.root.forEach(this, visitor);
    return visitor.getList();
  }

  protected Envelope getEnvelope(final int[] index) {
    final CoordinatesList points = GeometryEditUtil.getPoints(this.geometry, index);
    final int vertexIndex = GeometryEditUtil.getVertexIndex(index);
    final double x1 = points.getX(vertexIndex);
    final double y1 = points.getY(vertexIndex);
    final double x2 = points.getX(vertexIndex + 1);
    final double y2 = points.getY(vertexIndex + 1);
    return new Envelope(x1, x2, y1, y2);
  }

  public List<LineSegment> getIntersecting(final BoundingBox boundingBox) {
    final CreateListVisitor<LineSegment> visitor = new CreateListVisitor<LineSegment>();
    visit(boundingBox, visitor);
    return visitor.getList();
  }

  public List<LineSegment> getIntersectingBoundingBox(final Geometry geometry) {
    final BoundingBox boundingBox = BoundingBox.getBoundingBox(geometry);
    return getIntersecting(boundingBox);
  }

  protected LineSegment getLineSegment(final int[] index) {
    final CoordinatesList points = GeometryEditUtil.getPoints(this.geometry, index);
    final int vertexIndex = GeometryEditUtil.getVertexIndex(index);
    final Coordinates p1 = points.get(vertexIndex);
    final Coordinates p2 = points.get(vertexIndex + 1);
    return new LineSegment(GeometryFactory.getFactory(this.geometry), p1, p2);
  }

  public int getSize() {
    return this.size;
  }

  public List<LineSegment> getWithin(final BoundingBox boundingBox,
    final Predicate<LineSegment> filter) {
    final CreateListVisitor<LineSegment> visitor = new CreateListVisitor<LineSegment>(filter);
    visit(boundingBox, visitor);
    return visitor.getList();
  }

  public List<LineSegment> getWithinDistance(final Coordinates point, final double maxDistance) {
    BoundingBox boundingBox = new BoundingBox(point);
    boundingBox = boundingBox.expand(maxDistance);
    final LineSegmentCoordinateDistanceFilter filter = new LineSegmentCoordinateDistanceFilter(
      point, maxDistance);
    return getWithin(boundingBox, filter);
  }

  public void insert(final Envelope envelope, final int[] index) {
    this.size++;
    collectStats(envelope);
    final Envelope insertEnv = ensureExtent(envelope, this.minExtent);
    this.root.insert(insertEnv, index);
  }

  public int size() {
    return getSize();
  }

  public void visit(final BoundingBox boundingBox, final Consumer<LineSegment> visitor) {
    try {
      this.root.forEach(this, boundingBox, visitor);
    } catch (final ExitLoopException e) {

    }
  }

}
