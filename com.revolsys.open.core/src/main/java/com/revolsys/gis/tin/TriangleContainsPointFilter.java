package com.revolsys.gis.tin;

import com.revolsys.gis.model.coordinates.Coordinates;
import java.util.function.Predicate;

public class TriangleContainsPointFilter implements Predicate<Triangle> {
  private final Coordinates point;

  public TriangleContainsPointFilter(final Coordinates point) {
    this.point = point;
  }

  @Override
  public boolean test(final Triangle triangle) {
    return triangle.contains(this.point);
  }
}
