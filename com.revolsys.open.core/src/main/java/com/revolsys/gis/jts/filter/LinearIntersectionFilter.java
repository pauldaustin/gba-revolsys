package com.revolsys.gis.jts.filter;

import java.util.function.Predicate;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.IntersectionMatrix;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

public class LinearIntersectionFilter implements Predicate<LineString> {

  private final Envelope envelope;

  private final LineString line;

  private final PreparedGeometry preparedLine;

  public LinearIntersectionFilter(final LineString line) {
    this.line = line;
    this.preparedLine = PreparedGeometryFactory.prepare(line);
    this.envelope = line.getEnvelopeInternal();
  }

  @Override
  public boolean test(final LineString line) {
    final Envelope envelope = line.getEnvelopeInternal();
    if (envelope.intersects(this.envelope)) {
      if (this.preparedLine.intersects(line)) {
        final IntersectionMatrix relate = this.line.relate(line);
        if (relate.isOverlaps(1, 1) || relate.isContains() || relate.isWithin()) {
          return true;
        }
      }
    }
    return false;
  }
}
