package com.revolsys.data.record.filter;

import com.revolsys.data.record.Record;
import com.revolsys.filter.Filter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class LineStringRecordFilter implements Filter<Record> {

  public static final LineStringRecordFilter FILTER = new LineStringRecordFilter();

  private LineStringRecordFilter() {
  }

  @Override
  public boolean accept(final Record object) {
    final Geometry geometry = object.getGeometryValue();
    return geometry instanceof LineString;
  }

  @Override
  public String toString() {
    return "LineString";
  }

}
