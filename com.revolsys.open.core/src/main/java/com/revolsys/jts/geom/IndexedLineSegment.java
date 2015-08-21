package com.revolsys.jts.geom;

public class IndexedLineSegment extends LineSegment {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private final int[] index;

  public IndexedLineSegment(final GeometryFactory geometryFactory, final LineSegment line,
    final int... index) {
    super(geometryFactory, line);
    this.index = index;
  }

  public IndexedLineSegment(final LineSegment line, final int... index) {
    super(line);
    this.index = index;
  }

  public int[] getIndex() {
    return this.index;
  }
}
