package com.revolsys.data.record.filter;

import com.revolsys.data.record.Record;
import com.revolsys.filter.Filter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class PointRecordFilter implements Filter<Record> {
  public static final PointRecordFilter FILTER = new PointRecordFilter();

  private PointRecordFilter() {
  }

  @Override
  public boolean accept(final Record object) {
    final Geometry geometry = object.getGeometry();
    return geometry instanceof Point;
  }

  @Override
  public String toString() {
    return "Point";
  }

}
