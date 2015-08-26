package com.revolsys.gis.jts;

import java.util.function.Consumer;

import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Polygon;

public class LineStringMitredBuffer implements Consumer<LineSegment> {
  private Polygon buffer;

  private final double distance;

  public LineStringMitredBuffer(final double distance) {
    this.distance = distance;
  }

  @Override
  public void accept(final LineSegment segment) {
    final Polygon segmentBuffer = JtsGeometryUtil.getMitredBuffer(segment, this.distance);
    if (this.buffer == null) {
      this.buffer = segmentBuffer;
    } else {
      this.buffer = (Polygon)this.buffer.union(segmentBuffer);
    }
  }

  /**
   * @return the buffer
   */
  public Polygon getBuffer() {
    return this.buffer;
  }

}
