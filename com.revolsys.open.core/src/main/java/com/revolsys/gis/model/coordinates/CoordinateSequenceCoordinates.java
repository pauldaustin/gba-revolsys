package com.revolsys.gis.model.coordinates;

import com.vividsolutions.jts.geom.CoordinateSequence;

public class CoordinateSequenceCoordinates extends AbstractCoordinates {
  private final CoordinateSequence coordinates;

  private int index = 0;

  public CoordinateSequenceCoordinates(final CoordinateSequence coordinates) {
    this.coordinates = coordinates;
  }

  public CoordinateSequenceCoordinates(final CoordinateSequence coordinates, final int index) {
    this.coordinates = coordinates;
    this.index = index;
  }

  @Override
  public CoordinateSequenceCoordinates cloneCoordinates() {
    return new CoordinateSequenceCoordinates(this.coordinates, this.index);
  }

  public int getIndex() {
    return this.index;
  }

  @Override
  public byte getNumAxis() {
    return (byte)this.coordinates.getDimension();
  }

  @Override
  public double getValue(final int index) {
    if (index >= 0 && index < getNumAxis()) {
      return this.coordinates.getOrdinate(this.index, index);
    } else {
      return 0;
    }
  }

  public void setIndex(final int index) {
    this.index = index;
  }

  @Override
  public void setValue(final int index, final double value) {
    if (index >= 0 && index < getNumAxis()) {
      this.coordinates.setOrdinate(this.index, index, value);
    }
  }

  public int size() {
    return this.coordinates.size();
  }

  @Override
  public String toString() {
    return this.coordinates.getCoordinate(this.index).toString();
  }

}
