package com.revolsys.gis.model.coordinates;

@SuppressWarnings("serial")
public class CoordinatesWithOrientation extends DoubleCoordinates {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private final double orientation;

  public CoordinatesWithOrientation(final Coordinates coordinates, final double orientation) {
    super(coordinates);
    this.orientation = orientation;
  }

  public double getOrientation() {
    return this.orientation;
  }
}
