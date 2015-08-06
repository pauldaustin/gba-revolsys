package com.revolsys.gis.parallel;

import com.revolsys.data.record.Record;
import com.revolsys.gis.jts.JtsGeometryUtil;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.process.BaseInOutProcess;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

public class ClipGeometryProcess extends BaseInOutProcess<Record, Record> {

  private Polygon clipPolygon;

  /**
   * @return the clipPolygon
   */
  public Polygon getClipPolygon() {
    return this.clipPolygon;
  }

  @Override
  protected void process(final Channel<Record> in, final Channel<Record> out, final Record object) {
    final Geometry geometry = object.getGeometry();
    if (geometry != null) {
      final Geometry intersection = geometry.intersection(this.clipPolygon);
      if (!intersection.isEmpty() && intersection.getClass() == geometry.getClass()) {
        if (intersection instanceof LineString) {
          final LineString lineString = (LineString)intersection;
          final Coordinate c0 = lineString.getCoordinateN(0);
          if (Double.isNaN(c0.z)) {
            JtsGeometryUtil.addElevation(c0, (LineString)geometry);
          }
          final Coordinate cN = lineString.getCoordinateN(lineString.getNumPoints() - 1);
          if (Double.isNaN(cN.z)) {
            JtsGeometryUtil.addElevation(cN, (LineString)geometry);
          }
        }
        JtsGeometryUtil.copyUserData(geometry, intersection);

        object.setGeometryValue(intersection);
        out.write(object);
      }
    } else {
      out.write(object);
    }
  }

  /**
   * @param clipPolygon the clipPolygon to set
   */
  public void setClipPolygon(final Polygon clipPolygon) {
    this.clipPolygon = clipPolygon;
  }

}
