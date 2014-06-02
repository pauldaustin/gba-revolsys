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
package com.revolsys.jts.geom;

import com.revolsys.jts.geom.impl.PointDouble;

/**
 * A Bounding Container which is in the shape of an octagon.
 * The OctagonalEnvelope of a geometric object
 * is tight along the four extremal rectilineal parallels
 * and along the four extremal diagonal parallels.
 * Depending on the shape of the contained
 * geometry, the octagon may be degenerate to any extreme
 * (e.g. it may be a rectangle, a line, or a point).
 */
public class OctagonalEnvelope {

  private static double SQRT2 = Math.sqrt(2.0);

  private static double computeA(final double x, final double y) {
    return x + y;
  }

  private static double computeB(final double x, final double y) {
    return x - y;
  }

  // initialize in the null state
  private double minX = Double.NaN;

  private double maxX;

  private double minY;

  private double maxY;

  private double minA;

  private double maxA;

  private double minB;

  private double maxB;

  /**
   * Creates a new null bounding octagon
   */
  public OctagonalEnvelope() {
  }

  /**
   * Creates a new null bounding octagon bounding an {@link Envelope}
   */
  public OctagonalEnvelope(final BoundingBox env) {
    expandToInclude(env);
  }

  /**
   * Creates a new null bounding octagon bounding a {@link Geometry}
   */
  public OctagonalEnvelope(final Geometry geom) {
    expandToInclude(geom);
  }

  /**
   * Creates a new null bounding octagon bounding an {@link OctagonalEnvelope}
   * (the copy constructor).
   */
  public OctagonalEnvelope(final OctagonalEnvelope oct) {
    expandToInclude(oct);
  }

  /**
   * Creates a new null bounding octagon bounding a {@link Coordinates}
   */
  public OctagonalEnvelope(final Point p) {
    expandToInclude(p);
  }

  /**
   * Creates a new null bounding octagon bounding a pair of {@link Coordinates}s
   */
  public OctagonalEnvelope(final Point p0, final Point p1) {
    expandToInclude(p0);
    expandToInclude(p1);
  }

  public boolean contains(final OctagonalEnvelope other) {
    if (isNull() || other.isNull()) {
      return false;
    }

    return other.minX >= minX && other.maxX <= maxX && other.minY >= minY
      && other.maxY <= maxY && other.minA >= minA && other.maxA <= maxA
      && other.minB >= minB && other.maxB <= maxB;
  }

  public void expandBy(final double distance) {
    if (isNull()) {
      return;
    }

    final double diagonalDistance = SQRT2 * distance;

    minX -= distance;
    maxX += distance;
    minY -= distance;
    maxY += distance;
    minA -= diagonalDistance;
    maxA += diagonalDistance;
    minB -= diagonalDistance;
    maxB += diagonalDistance;

    if (!isValid()) {
      setToNull();
    }
  }

  public OctagonalEnvelope expandToInclude(final BoundingBox env) {
    expandToInclude(env.getMinX(), env.getMinY());
    expandToInclude(env.getMinX(), env.getMaxY());
    expandToInclude(env.getMaxX(), env.getMinY());
    expandToInclude(env.getMaxX(), env.getMaxY());
    return this;
  }

  public OctagonalEnvelope expandToInclude(final double x, final double y) {
    final double A = computeA(x, y);
    final double B = computeB(x, y);

    if (isNull()) {
      minX = x;
      maxX = x;
      minY = y;
      maxY = y;
      minA = A;
      maxA = A;
      minB = B;
      maxB = B;
    } else {
      if (x < minX) {
        minX = x;
      }
      if (x > maxX) {
        maxX = x;
      }
      if (y < minY) {
        minY = y;
      }
      if (y > maxY) {
        maxY = y;
      }
      if (A < minA) {
        minA = A;
      }
      if (A > maxA) {
        maxA = A;
      }
      if (B < minB) {
        minB = B;
      }
      if (B > maxB) {
        maxB = B;
      }
    }
    return this;
  }

  public void expandToInclude(final Geometry geometry) {
    for (final Point point : geometry.getGeometries(Point.class)) {
      expandToInclude(point);
    }
    for (final LineString line : geometry.getGeometryComponents(LineString.class)) {
      expandToInclude((LineString)line);
    }
  }

  public OctagonalEnvelope expandToInclude(final OctagonalEnvelope oct) {
    if (oct.isNull()) {
      return this;
    }

    if (isNull()) {
      minX = oct.minX;
      maxX = oct.maxX;
      minY = oct.minY;
      maxY = oct.maxY;
      minA = oct.minA;
      maxA = oct.maxA;
      minB = oct.minB;
      maxB = oct.maxB;
      return this;
    }
    if (oct.minX < minX) {
      minX = oct.minX;
    }
    if (oct.maxX > maxX) {
      maxX = oct.maxX;
    }
    if (oct.minY < minY) {
      minY = oct.minY;
    }
    if (oct.maxY > maxY) {
      maxY = oct.maxY;
    }
    if (oct.minA < minA) {
      minA = oct.minA;
    }
    if (oct.maxA > maxA) {
      maxA = oct.maxA;
    }
    if (oct.minB < minB) {
      minB = oct.minB;
    }
    if (oct.maxB > maxB) {
      maxB = oct.maxB;
    }
    return this;
  }

  public OctagonalEnvelope expandToInclude(final Point p) {
    expandToInclude(p.getX(), p.getY());
    return this;
  }

  public OctagonalEnvelope expandToInclude(final LineString seq) {
    for (int i = 0; i < seq.getVertexCount(); i++) {
      final double x = seq.getX(i);
      final double y = seq.getY(i);
      expandToInclude(x, y);
    }
    return this;
  }

  public double getMaxA() {
    return maxA;
  }

  public double getMaxB() {
    return maxB;
  }

  public double getMaxX() {
    return maxX;
  }

  public double getMaxY() {
    return maxY;
  }

  public double getMinA() {
    return minA;
  }

  public double getMinB() {
    return minB;
  }

  public double getMinX() {
    return minX;
  }

  public double getMinY() {
    return minY;
  }

  public boolean intersects(final OctagonalEnvelope other) {
    if (isNull() || other.isNull()) {
      return false;
    }

    if (minX > other.maxX) {
      return false;
    }
    if (maxX < other.minX) {
      return false;
    }
    if (minY > other.maxY) {
      return false;
    }
    if (maxY < other.minY) {
      return false;
    }
    if (minA > other.maxA) {
      return false;
    }
    if (maxA < other.minA) {
      return false;
    }
    if (minB > other.maxB) {
      return false;
    }
    if (maxB < other.minB) {
      return false;
    }
    return true;
  }

  public boolean intersects(final Point p) {
    if (minX > p.getX()) {
      return false;
    }
    if (maxX < p.getX()) {
      return false;
    }
    if (minY > p.getY()) {
      return false;
    }
    if (maxY < p.getY()) {
      return false;
    }

    final double A = computeA(p.getX(), p.getY());
    final double B = computeB(p.getX(), p.getY());
    if (minA > A) {
      return false;
    }
    if (maxA < A) {
      return false;
    }
    if (minB > B) {
      return false;
    }
    if (maxB < B) {
      return false;
    }
    return true;
  }

  public boolean isNull() {
    return Double.isNaN(minX);
  }

  /**
   * Tests if the extremal values for this octagon are valid.
   *
   * @return <code>true</code> if this object has valid values
   */
  private boolean isValid() {
    if (isNull()) {
      return true;
    }
    return minX <= maxX && minY <= maxY && minA <= maxA && minB <= maxB;
  }

  /**
   *  Sets the value of this object to the null value
   */
  public void setToNull() {
    minX = Double.NaN;
  }

  public Geometry toGeometry(final GeometryFactory geometryFactory) {
    if (isNull()) {
      return geometryFactory.point((LineString)null);
    }

    final Point px00 = new PointDouble(geometryFactory.makePrecise(0, minX),
      geometryFactory.makePrecise(1, minA - minX));
    final Point px01 = new PointDouble(geometryFactory.makePrecise(0, minX),
      geometryFactory.makePrecise(1, minX - minB));

    final Point px10 = new PointDouble(geometryFactory.makePrecise(0, maxX),
      geometryFactory.makePrecise(1, maxX - maxB));
    final Point px11 = new PointDouble(geometryFactory.makePrecise(0, maxX),
      geometryFactory.makePrecise(1, maxA - maxX));

    final Point py00 = new PointDouble(geometryFactory.makePrecise(0, minA
      - minY), geometryFactory.makePrecise(1, minY));
    final Point py01 = new PointDouble(geometryFactory.makePrecise(0, minY
      + maxB), geometryFactory.makePrecise(1, minY));

    final Point py10 = new PointDouble(geometryFactory.makePrecise(0, maxY
      + minB), geometryFactory.makePrecise(1, maxY));
    final Point py11 = new PointDouble(geometryFactory.makePrecise(0, maxA
      - maxY), geometryFactory.makePrecise(1, maxY));

    final CoordinateList coordList = new CoordinateList();
    coordList.add(px00, false);
    coordList.add(px01, false);
    coordList.add(py10, false);
    coordList.add(py11, false);
    coordList.add(px11, false);
    coordList.add(px10, false);
    coordList.add(py01, false);
    coordList.add(py00, false);

    if (coordList.size() == 1) {
      return geometryFactory.point(px00);
    }
    if (coordList.size() == 2) {
      return geometryFactory.lineString(coordList);
    }
    // must be a polygon, so add closing point
    coordList.add(px00, false);
    return geometryFactory.polygon(geometryFactory.linearRing(coordList));
  }
}
