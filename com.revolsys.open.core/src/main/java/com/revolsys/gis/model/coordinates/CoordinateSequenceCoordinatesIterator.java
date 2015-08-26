package com.revolsys.gis.model.coordinates;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.vividsolutions.jts.geom.CoordinateSequence;

public class CoordinateSequenceCoordinatesIterator extends AbstractCoordinates
  implements Iterator<Coordinates>, Iterable<Coordinates> {
  private final CoordinateSequence coordinates;

  private int index = 0;

  public CoordinateSequenceCoordinatesIterator(final CoordinateSequence coordinates) {
    this.coordinates = coordinates;
  }

  public CoordinateSequenceCoordinatesIterator(final CoordinateSequence coordinates,
    final int index) {
    this.coordinates = coordinates;
    this.index = index;
  }

  @Override
  public CoordinateSequenceCoordinatesIterator cloneCoordinates() {
    return new CoordinateSequenceCoordinatesIterator(this.coordinates, this.index);
  }

  public int getIndex() {
    return this.index;
  }

  @Override
  public byte getNumAxis() {
    return (byte)this.coordinates.getDimension();
  }

  @Override
  public double getValue(final int axisIndex) {
    if (axisIndex >= 0 && axisIndex < getNumAxis()) {
      return this.coordinates.getOrdinate(this.index, axisIndex);
    } else {
      return 0;
    }
  }

  public double getValue(final int relativeIndex, final int axisIndex) {
    if (axisIndex >= 0 && axisIndex < getNumAxis()) {
      return this.coordinates.getOrdinate(this.index + relativeIndex, axisIndex);
    } else {
      return 0;
    }
  }

  @Override
  public boolean hasNext() {
    return this.index < this.coordinates.size() - 1;
  }

  @Override
  public Iterator<Coordinates> iterator() {
    return this;
  }

  @Override
  public Coordinates next() {
    if (hasNext()) {
      this.index++;
      return this;
    } else {
      throw new NoSuchElementException();
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();

  }

  public void setIndex(final int index) {
    this.index = index;
  }

  @Override
  public void setValue(final int axisIndex, final double value) {
    if (axisIndex >= 0 && axisIndex < getNumAxis()) {
      this.coordinates.setOrdinate(this.index, axisIndex, value);
    }
  }

  public void setValue(final int relativeIndex, final int axisIndex, final double value) {
    if (axisIndex >= 0 && axisIndex < getNumAxis()) {
      this.coordinates.setOrdinate(this.index + relativeIndex, axisIndex, value);
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
