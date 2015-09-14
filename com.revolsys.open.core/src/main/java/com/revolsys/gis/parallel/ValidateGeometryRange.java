package com.revolsys.gis.parallel;

import org.apache.log4j.Logger;

import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.process.BaseInOutProcess;
import com.revolsys.record.Record;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ValidateGeometryRange extends BaseInOutProcess<Record, Record> {
  private static final Logger LOG = Logger.getLogger(ValidateGeometryRange.class);

  private double maxX = Double.MAX_VALUE;

  private double maxY = Double.MAX_VALUE;

  private double maxZ = Double.MAX_VALUE;

  private double minX = -Double.MAX_VALUE;

  private double minY = -Double.MAX_VALUE;

  private double minZ = -Double.MAX_VALUE;

  /**
   * @return the maxX
   */
  public double getMaxX() {
    return this.maxX;
  }

  /**
   * @return the maxY
   */
  public double getMaxY() {
    return this.maxY;
  }

  /**
   * @return the maxZ
   */
  public double getMaxZ() {
    return this.maxZ;
  }

  /**
   * @return the minX
   */
  public double getMinX() {
    return this.minX;
  }

  /**
   * @return the minY
   */
  public double getMinY() {
    return this.minY;
  }

  /**
   * @return the minZ
   */
  public double getMinZ() {
    return this.minZ;
  }

  private boolean isValid(final double min, final double max, final double value) {
    return value >= min && value <= max;
  }

  private boolean isValid(final String type, final Coordinate coordinate) {
    if (!isValid(this.minX, this.maxY, coordinate.x) || !isValid(this.minY, this.maxY, coordinate.y)
      || !isValid(this.minZ, this.maxZ, coordinate.z)) {
      LOG.warn(type + " has invalid coordinate at " + coordinate);
      return false;
    } else {
      return true;
    }

  }

  private boolean isValid(final String type, final CoordinateSequence coordinates) {
    boolean valid = true;
    for (int j = 0; j < coordinates.size(); j++) {
      final Coordinate coordinate = coordinates.getCoordinate(j);
      if (!isValid(type, coordinate)) {
        valid = false;
      }
    }
    return valid;
  }

  private boolean isValid(final String type, final Geometry geometry) {
    boolean valid = true;
    for (int i = 0; i < geometry.getNumGeometries(); i++) {
      final Geometry subGeometry = geometry.getGeometryN(i);
      if (subGeometry instanceof Point) {
        final Coordinate coordinate = geometry.getCoordinate();
        if (!isValid(type, coordinate)) {
          valid = false;
        }
      } else if (subGeometry instanceof LineString) {
        final LineString line = (LineString)subGeometry;
        valid = isValid(type, line);

      } else if (subGeometry instanceof Polygon) {
        final Polygon polygon = (Polygon)subGeometry;

        if (!isValid(type, polygon.getExteriorRing())) {
          valid = false;
        }
        for (int k = 0; k < polygon.getNumInteriorRing(); k++) {
          final LineString ring = polygon.getInteriorRingN(k);
          if (!isValid(type, ring)) {
            valid = false;
          }
        }
      }
    }
    return valid;

  }

  private boolean isValid(final String type, final LineString line) {
    final CoordinateSequence coordinates = line.getCoordinateSequence();
    if (!isValid(type, coordinates)) {
      return false;
    }
    return true;
  }

  @Override
  protected void process(final Channel<Record> in, final Channel<Record> out, final Record object) {
    // TODO Auto-generated method stub
    final Geometry geometry = object.getGeometry();
    isValid(object.getRecordDefinition().getPath().toString(), geometry);
    out.write(object);
  }

  /**
   * @param maxX the maxX to set
   */
  public void setMaxX(final double maxX) {
    this.maxX = maxX;
  }

  /**
   * @param maxY the maxY to set
   */
  public void setMaxY(final double maxY) {
    this.maxY = maxY;
  }

  /**
   * @param maxZ the maxZ to set
   */
  public void setMaxZ(final double maxZ) {
    this.maxZ = maxZ;
  }

  /**
   * @param minX the minX to set
   */
  public void setMinX(final double minX) {
    this.minX = minX;
  }

  /**
   * @param minY the minY to set
   */
  public void setMinY(final double minY) {
    this.minY = minY;
  }

  /**
   * @param minZ the minZ to set
   */
  public void setMinZ(final double minZ) {
    this.minZ = minZ;
  }

}
