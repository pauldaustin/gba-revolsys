package com.revolsys.gis.cs.projection;

import com.revolsys.gis.cs.Datum;
import com.revolsys.gis.cs.GeographicCoordinateSystem;
import com.revolsys.gis.cs.ProjectedCoordinateSystem;
import com.revolsys.gis.cs.ProjectionParameterNames;
import com.revolsys.gis.cs.Spheroid;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.vividsolutions.jts.algorithm.Angle;

public class Mercator1SPSpherical implements CoordinatesProjection {
  /** The central origin. */
  private final double lambda0;

  private final double r;

  private final double x0;

  private final double y0;

  public Mercator1SPSpherical(final ProjectedCoordinateSystem cs) {
    final GeographicCoordinateSystem geographicCS = cs.getGeographicCoordinateSystem();
    final Datum datum = geographicCS.getDatum();
    final double centralMeridian = cs
      .getDoubleParameter(ProjectionParameterNames.LONGITUDE_OF_CENTER);

    final Spheroid spheroid = datum.getSpheroid();
    this.x0 = cs.getDoubleParameter(ProjectionParameterNames.FALSE_EASTING);
    this.y0 = cs.getDoubleParameter(ProjectionParameterNames.FALSE_NORTHING);
    this.lambda0 = Math.toRadians(centralMeridian);
    this.r = spheroid.getSemiMinorAxis();

  }

  @Override
  public void inverse(final Coordinates from, final Coordinates to) {
    final double x = from.getX() - this.x0;
    final double y = from.getY() - this.y0;

    final double lambda = x / this.r + this.lambda0;

    final double phi = Angle.PI_OVER_2 - 2 * Math.atan(Math.pow(Math.E, -y / this.r));

    to.setValue(0, lambda);
    to.setValue(1, phi);
    for (int i = 2; i < from.getNumAxis() && i < to.getNumAxis(); i++) {
      final double ordinate = from.getValue(i);
      to.setValue(i, ordinate);
    }
  }

  @Override
  public void project(final Coordinates from, final Coordinates to) {
    final double lambda = from.getX();
    final double phi = from.getY();

    final double x = this.r * (lambda - this.lambda0);

    final double y = this.r * Math.log(Math.tan(Angle.PI_OVER_4 + phi / 2));

    to.setValue(0, this.x0 + x);
    to.setValue(1, this.y0 + y);

    for (int i = 2; i < from.getNumAxis() && i < to.getNumAxis(); i++) {
      final double ordinate = from.getValue(i);
      to.setValue(i, ordinate);
    }
  }

}
