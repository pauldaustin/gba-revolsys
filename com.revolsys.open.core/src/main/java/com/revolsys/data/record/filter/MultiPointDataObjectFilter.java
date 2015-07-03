package com.revolsys.data.record.filter;

import com.revolsys.data.record.Record;
import com.revolsys.filter.Filter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;

public class MultiPointDataObjectFilter implements Filter<Record> {
  public static final MultiPointDataObjectFilter FILTER = new MultiPointDataObjectFilter();

  private MultiPointDataObjectFilter() {
  }

  @Override
  public boolean accept(final Record object) {
    final Geometry geometry = object.getGeometryValue();
    return geometry instanceof MultiPoint;
  }

  @Override
  public String toString() {
    return "MultiPoint";
  }

}
