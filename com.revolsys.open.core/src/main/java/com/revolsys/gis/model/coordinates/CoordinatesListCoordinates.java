package com.revolsys.gis.model.coordinates;

import com.revolsys.gis.model.coordinates.list.CoordinatesList;

public class CoordinatesListCoordinates extends AbstractCoordinates {
  private final CoordinatesList coordinates;

  private int index = 0;

  public CoordinatesListCoordinates(final CoordinatesList coordinates) {
    this.coordinates = coordinates;
  }

  public CoordinatesListCoordinates(final CoordinatesList coordinates, final int index) {
    this.coordinates = coordinates;
    this.index = index;
  }

  @Override
  public Coordinates cloneCoordinates() {
    return new DoubleCoordinates(this);
  }

  public int getIndex() {
    return this.index;
  }

  @Override
  public byte getNumAxis() {
    return this.coordinates.getNumAxis();
  }

  @Override
  public double getValue(final int index) {
    if (index >= 0 && index < this.coordinates.getNumAxis()) {
      return this.coordinates.getValue(this.index, index);
    } else {
      return 0;
    }
  }

  public void next() {
    this.index++;
  }

  public void setIndex(final int index) {
    this.index = index;
  }

  @Override
  public void setValue(final int index, final double value) {
    if (index >= 0 && index < this.coordinates.getNumAxis()) {
      this.coordinates.setValue(this.index, index, value);
    }
  }

  public int size() {
    return this.coordinates.size();
  }

  @Override
  public String toString() {
    final byte numAxis = getNumAxis();
    if (numAxis > 0) {
      final double x = this.coordinates.getX(this.index);
      final StringBuffer s = new StringBuffer(String.valueOf(x));
      final double y = this.coordinates.getY(this.index);
      s.append(',');
      s.append(y);

      for (int i = 2; i < numAxis; i++) {
        final Double ordinate = this.coordinates.getValue(this.index, i);
        s.append(',');
        s.append(ordinate);
      }
      return s.toString();
    } else {
      return "";
    }
  }

}
