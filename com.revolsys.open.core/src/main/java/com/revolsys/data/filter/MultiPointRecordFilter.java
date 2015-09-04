package com.revolsys.data.filter;

import java.util.function.Predicate;

import com.revolsys.data.record.Record;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;

public class MultiPointRecordFilter implements Predicate<Record> {
  public static final MultiPointRecordFilter FILTER = new MultiPointRecordFilter();

  private MultiPointRecordFilter() {
  }

  @Override
  public boolean test(final Record object) {
    final Geometry geometry = object.getGeometry();
    return geometry instanceof MultiPoint;
  }

  @Override
  public String toString() {
    return "MultiPoint";
  }

}
