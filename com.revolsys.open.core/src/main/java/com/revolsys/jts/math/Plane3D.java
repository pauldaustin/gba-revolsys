/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */

package com.revolsys.jts.math;

import com.revolsys.jts.geom.Point;

/**
 * Models a plane in 3-dimensional Cartesian space.
 *
 * @author mdavis
 *
 */
public class Plane3D {

  /**
   * Enums for the 3 coordinate planes
   */
  public static final int XY_PLANE = 1;
  public static final int YZ_PLANE = 2;
  public static final int XZ_PLANE = 3;

  private final Vector3D normal;
  private final Point basePt;

  public Plane3D(final Vector3D normal, final Point basePt)
  {
    this.normal = normal;
    this.basePt = basePt;
  }

  /**
   * Computes the axis plane that this plane lies closest to.
   * <p>
   * Geometries lying in this plane undergo least distortion
   * (and have maximum area)
   * when projected to the closest axis plane.
   * This provides optimal conditioning for
   * computing a Point-in-Polygon test.
   *
   * @return the index of the closest axis plane.
   */
  public int closestAxisPlane() {
    final double xmag = Math.abs(this.normal.getX());
    final double ymag = Math.abs(this.normal.getY());
    final double zmag = Math.abs(this.normal.getZ());
    if (xmag > ymag) {
      if (xmag > zmag) {
        return YZ_PLANE;
      } else {
        return XY_PLANE;
      }
    }
    // y >= x
    else if (zmag > ymag) {
      return XY_PLANE;
    }
    // y >= z
    return XZ_PLANE;
  }

  /**
   * Computes the oriented distance from a point to the plane.
   * The distance is:
   * <ul>
   * <li><b>positive</b> if the point lies above the plane (relative to the plane normal)
   * <li><b>zero</b> if the point is on the plane
   * <li><b>negative</b> if the point lies below the plane (relative to the plane normal)
   * </ul>
   *
   * @param p the point to compute the distance for
   * @return the oriented distance to the plane
   */
  public double orientedDistance(final Point p) {
    final Vector3D pb = new Vector3D(p, this.basePt);
    final double pbdDotNormal = pb.dot(this.normal);
    if (Double.isNaN(pbdDotNormal)) {
      throw new IllegalArgumentException("3D Point has NaN ordinate");
    }
    final double d = pbdDotNormal / this.normal.length();
    return d;
  }

}