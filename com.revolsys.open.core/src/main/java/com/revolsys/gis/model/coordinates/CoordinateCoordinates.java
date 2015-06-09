package com.revolsys.gis.model.coordinates;

import com.vividsolutions.jts.geom.Coordinate;

public class CoordinateCoordinates extends AbstractCoordinates {
  private Coordinate coordinate;

  public CoordinateCoordinates(final Coordinate coordinate) {
    this.coordinate = coordinate;
  }

  @Override
  public CoordinateCoordinates cloneCoordinates() {
    final Coordinate newCoordinate = new Coordinate(this.coordinate);
    return new CoordinateCoordinates(newCoordinate);
  }

  public Coordinate getCoordinate() {
    return this.coordinate;
  }

  @Override
  public byte getNumAxis() {
    if (Double.isNaN(this.coordinate.z)) {
      return 2;
    } else {
      return 3;
    }
  }

  @Override
  public double getValue(final int index) {
    switch (index) {
      case 0:
        return this.coordinate.x;
      case 1:
        return this.coordinate.y;
      case 2:
        return this.coordinate.z;
      default:
        return Double.NaN;
    }
  }

  public void setCoordinate(final Coordinate coordinate) {
    this.coordinate = coordinate;
  }

  @Override
  public void setValue(final int index, final double value) {
    switch (index) {
      case 0:
        this.coordinate.x = value;
        break;
      case 1:
        this.coordinate.y = value;
        break;
      case 2:
        this.coordinate.z = value;
        break;
      default:
        break;
    }
  }

  @Override
  public String toString() {
    return this.coordinate.toString();
  }
}
