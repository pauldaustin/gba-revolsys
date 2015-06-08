package com.revolsys.gis.model.coordinates.list;

import com.vividsolutions.jts.geom.CoordinateSequence;

public class CoordinateSequenceCoordinateList extends AbstractCoordinatesList {

  /**
   *
   */
  private static final long serialVersionUID = 872633273329727308L;

  private final CoordinateSequence coordinateSequence;

  public CoordinateSequenceCoordinateList(final CoordinateSequence coordinateSequence) {
    this.coordinateSequence = coordinateSequence;
  }

  @Override
  public AbstractCoordinatesList clone() {
    return new CoordinateSequenceCoordinateList(this.coordinateSequence);
  }

  @Override
  public byte getNumAxis() {
    return (byte)this.coordinateSequence.getDimension();
  }

  @Override
  public double getValue(final int index, final int axisIndex) {
    return this.coordinateSequence.getOrdinate(index, axisIndex);
  }

  @Override
  public void setValue(final int index, final int axisIndex, final double value) {
    this.coordinateSequence.setOrdinate(index, axisIndex, value);
  }

  @Override
  public int size() {
    return this.coordinateSequence.size();
  }

}
