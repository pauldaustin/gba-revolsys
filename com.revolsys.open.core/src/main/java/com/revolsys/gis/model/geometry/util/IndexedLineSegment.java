package com.revolsys.gis.model.geometry.util;

import com.revolsys.gis.model.geometry.LineSegment;
import com.revolsys.jts.geom.GeometryFactory;

public class IndexedLineSegment extends LineSegment {

  private int[] index;

  public IndexedLineSegment(LineSegment line, int... index) {
    super(line);
    this.index = index;
  }

  public IndexedLineSegment(GeometryFactory geometryFactory, LineSegment line,
    int... index) {
    super(geometryFactory, line);
    this.index = index;
  }

  public int[] getIndex() {
    return index;
  }
}
