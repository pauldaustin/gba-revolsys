package com.revolsys.gis.jts.filter;

import java.util.function.Predicate;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class GeometryFilter {
  public static <T extends Geometry> Predicate<T> intersects(final Envelope envelope) {
    return (geometry) -> {
      final Envelope geometryEnvelope = geometry.getEnvelopeInternal();
      return envelope.intersects(geometryEnvelope);
    };
  }

  public static Predicate<LineString> lineContainedWithinTolerance(final LineString line,
    final double maxDistance) {
    return new LineContainsWithinToleranceFilter(line, maxDistance, true);
  }

  public static Predicate<LineString> lineContainsWithinTolerance(final LineString line,
    final double maxDistance) {
    return new LineContainsWithinToleranceFilter(line, maxDistance);
  }

  public static Predicate<LineString> lineEqualWithinTolerance(final LineString line,
    final double maxDistance) {
    return new LineEqualWithinToleranceFilter(line, maxDistance);
  }

  public static Predicate<LineString> lineWithinDistance(final LineString line,
    final double maxDistance) {
    return new LineStringLessThanDistanceFilter(line, maxDistance);
  }
}
