package com.revolsys.gis.algorithm.index.visitor;

import java.util.LinkedHashSet;
import java.util.Set;

import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.geometry.LineSegment;
import com.vividsolutions.jts.index.ItemVisitor;

public class LineSegmentIntersectionVisitor implements ItemVisitor {

  private final Set<CoordinatesList> intersections = new LinkedHashSet<CoordinatesList>();

  private final LineSegment querySeg;

  public LineSegmentIntersectionVisitor(final LineSegment querySeg) {
    this.querySeg = querySeg;
  }

  public Set<CoordinatesList> getIntersections() {
    return this.intersections;
  }

  @Override
  public void visitItem(final Object item) {
    final LineSegment segment = (LineSegment)item;
    if (segment.getEnvelope().intersects(this.querySeg.getEnvelope())) {
      final CoordinatesList intersection = this.querySeg.getIntersection(segment);
      if (intersection != null && intersection.size() > 0) {
        this.intersections.add(intersection);
      }
    }

  }
}
