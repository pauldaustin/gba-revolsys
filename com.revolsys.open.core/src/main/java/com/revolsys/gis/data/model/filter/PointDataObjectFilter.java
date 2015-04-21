package com.revolsys.gis.data.model.filter;

import com.revolsys.data.record.Record;
import com.revolsys.filter.Filter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class PointDataObjectFilter implements Filter<Record> {
  public static final PointDataObjectFilter FILTER = new PointDataObjectFilter();

  private PointDataObjectFilter() {
  }

  @Override
  public boolean accept(final Record object) {
    final Geometry geometry = object.getGeometryValue();
    return geometry instanceof Point;
  }

  @Override
  public String toString() {
    return "Point";
  }

}
