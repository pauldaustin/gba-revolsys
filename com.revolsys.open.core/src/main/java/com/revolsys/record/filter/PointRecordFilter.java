package com.revolsys.record.filter;

import java.util.function.Predicate;

import com.revolsys.record.Record;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class PointRecordFilter implements Predicate<Record> {
  public static final PointRecordFilter FILTER = new PointRecordFilter();

  private PointRecordFilter() {
  }

  @Override
  public boolean test(final Record object) {
    final Geometry geometry = object.getGeometry();
    return geometry instanceof Point;
  }

  @Override
  public String toString() {
    return "Point";
  }

}
