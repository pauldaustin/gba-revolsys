package com.revolsys.gis.graph.filter;

import com.revolsys.gis.graph.Node;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.jts.geom.GeometryFactory;
import java.util.function.Predicate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

public class NodeGeometryIntersectionFilter<T> implements Predicate<Node<T>> {

  private GeometryFactory geometryFactory;

  private PreparedGeometry preparedGeometry;

  public NodeGeometryIntersectionFilter() {
  }

  public NodeGeometryIntersectionFilter(final Geometry geometry) {
    setGeometry(geometry);
  }

  @Override
  public boolean test(final Node<T> node) {
    final Coordinates coordinates = node;
    final Point point = this.geometryFactory.createPoint(coordinates);
    final boolean intersects = this.preparedGeometry.intersects(point);
    return intersects;
  }

  public void setGeometry(final Geometry geometry) {
    this.preparedGeometry = PreparedGeometryFactory.prepare(geometry);
    this.geometryFactory = GeometryFactory.getFactory(geometry);
  }
}
