package com.revolsys.gis.model.coordinates.comparator;

import java.util.Comparator;

import com.vividsolutions.jts.geom.Geometry;

public class GeometryDistanceComparator implements Comparator<Geometry> {

  private final boolean invert;

  private final Geometry geometry;

  public GeometryDistanceComparator(final Geometry geometry) {
    this.geometry = geometry;
    this.invert = false;
  }

  public GeometryDistanceComparator(final Geometry geometry, final boolean invert) {
    this.geometry = geometry;
    this.invert = invert;
  }

  @Override
  public int compare(final Geometry geometry1, final Geometry geometry2) {
    int compare;
    final double distance1 = geometry1.distance(this.geometry);
    final double distance2 = geometry2.distance(this.geometry);
    if (distance1 == distance2) {
      compare = geometry1.compareTo(geometry2);
    } else if (distance1 < distance2) {
      compare = -1;
    } else {
      compare = 1;
    }

    if (this.invert) {
      return -compare;
    } else {
      return compare;
    }
  }

  public boolean isInvert() {
    return this.invert;
  }

}
