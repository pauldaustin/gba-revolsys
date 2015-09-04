package com.revolsys.data.record.filter;

import java.util.function.Predicate;

import com.revolsys.data.record.Record;
import com.vividsolutions.jts.geom.Geometry;

public class GeometryValueFilter implements Predicate<Record> {
  private final Geometry geometry;

  public GeometryValueFilter(final Geometry geometry) {
    this.geometry = geometry;
  }

  public GeometryValueFilter(final Record object) {
    this(object.<Geometry> getGeometry());
  }

  @Override
  public boolean test(final Record object) {
    final Geometry value = object.getGeometry();
    if (value == this.geometry) {
      return true;
    } else if (value != null && this.geometry != null) {
      return value.equals(this.geometry);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return " geometry == " + this.geometry;
  }

}
