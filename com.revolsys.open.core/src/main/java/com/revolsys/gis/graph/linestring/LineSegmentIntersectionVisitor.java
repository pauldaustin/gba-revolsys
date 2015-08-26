package com.revolsys.gis.graph.linestring;

import java.util.LinkedHashSet;
import java.util.Set;

import com.revolsys.gis.graph.Edge;
import com.revolsys.gis.graph.visitor.AbstractEdgeListenerVisitor;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.jts.geom.LineSegment;

public class LineSegmentIntersectionVisitor extends AbstractEdgeListenerVisitor<LineSegment> {

  private final Set<CoordinatesList> intersections = new LinkedHashSet<CoordinatesList>();

  private final LineSegment querySeg;

  public LineSegmentIntersectionVisitor(final LineSegment querySeg) {
    this.querySeg = querySeg;
  }

  @Override
  public void accept(final Edge<LineSegment> edge) {
    final LineSegment lineSegment = edge.getObject();
    if (lineSegment.getEnvelope().intersects(this.querySeg.getEnvelope())) {
      final CoordinatesList intersection = this.querySeg.getIntersection(lineSegment);
      if (intersection != null && intersection.size() > 0) {
        this.intersections.add(intersection);
      }
    }
  }

  public Set<CoordinatesList> getIntersections() {
    return this.intersections;
  }
}
