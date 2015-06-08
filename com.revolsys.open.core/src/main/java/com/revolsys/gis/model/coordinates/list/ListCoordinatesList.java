package com.revolsys.gis.model.coordinates.list;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.CoordinatesUtil;
import com.revolsys.gis.model.coordinates.DoubleCoordinates;
import com.vividsolutions.jts.geom.Point;

public class ListCoordinatesList extends AbstractCoordinatesList {

  private static final long serialVersionUID = 0L;

  private List<Coordinates> coordinates = new ArrayList<Coordinates>();

  private final byte numAxis;

  public ListCoordinatesList(final CoordinatesList coordinatesList) {
    this(coordinatesList.getNumAxis(), coordinatesList);
  }

  public ListCoordinatesList(final int numAxis) {
    this.numAxis = (byte)numAxis;
  }

  public ListCoordinatesList(final int numAxis, final Coordinates... coordinates) {
    this.numAxis = (byte)numAxis;
    for (final Coordinates coordinate : coordinates) {
      add(coordinate);
    }
  }

  public ListCoordinatesList(final int numAxis, final CoordinatesList coordinatesList) {
    this(numAxis);
    coordinatesList.copy(0, this, 0, numAxis, coordinatesList.size());
  }

  public ListCoordinatesList(final int numAxis, final List<Coordinates> coordinates) {
    this(numAxis);
    for (final Coordinates coordinate : coordinates) {
      add(coordinate);
    }
  }

  public void add(final Coordinates point) {
    this.coordinates.add(new DoubleCoordinates(point, this.numAxis));
  }

  public void add(final int index, final Coordinates point) {
    this.coordinates.add(index, point);
  }

  public void add(final Point point) {
    add(CoordinatesUtil.get(point));
  }

  public void clear() {
    this.coordinates.clear();
  }

  @Override
  public ListCoordinatesList clone() {
    final ListCoordinatesList clone = (ListCoordinatesList)super.clone();
    clone.coordinates = new ArrayList<Coordinates>(this.coordinates);
    return clone;
  }

  @Override
  public byte getNumAxis() {
    return this.numAxis;
  }

  @Override
  public double getValue(final int index, final int axisIndex) {
    final byte numAxis = getNumAxis();
    if (axisIndex < numAxis && index < size()) {
      return this.coordinates.get(index).getValue(axisIndex);
    } else {
      return Double.NaN;
    }
  }

  public void remove(final int index) {
    this.coordinates.remove(index);
  }

  @Override
  public void setValue(final int index, final int axisIndex, final double value) {
    final byte numAxis = getNumAxis();
    if (axisIndex < numAxis) {
      if (index <= size()) {
        for (int i = this.coordinates.size(); i < index + 1; i++) {
          add(new DoubleCoordinates(numAxis));
        }
      }
      this.coordinates.get(index).setValue(axisIndex, value);
    }
  }

  @Override
  public int size() {
    return this.coordinates.size();
  }
}
