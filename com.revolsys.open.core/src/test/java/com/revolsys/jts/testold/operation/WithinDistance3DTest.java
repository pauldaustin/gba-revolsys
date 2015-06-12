package com.revolsys.jts.testold.operation;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.io.ParseException;
import com.revolsys.jts.io.WKTReader;
import com.revolsys.jts.operation.distance3d.Distance3DOp;

public class WithinDistance3DTest extends TestCase {
  static GeometryFactory geomFact = GeometryFactory.floating3();

  static WKTReader rdr = new WKTReader();

  public static void main(final String args[]) {
    TestRunner.run(WithinDistance3DTest.class);
  }

  String polyHoleFlat = "POLYGON ((100 200 0, 200 200 0, 200 100 0, 100 100 0, 100 200 0), (120 180 0, 180 180 0, 180 120 0, 120 120 0, 120 180 0))";

  public WithinDistance3DTest(final String name) {
    super(name);
  }

  private void checkWithinDistance(final Geometry g1, final Geometry g2, final double distance,
    final boolean expectedResult) {
    final boolean isWithinDist = Distance3DOp.isWithinDistance(g1, g2, distance);
    assertEquals(expectedResult, isWithinDist);
  }

  private void checkWithinDistance(final String wkt1, final String wkt2, final double distance) {
    checkWithinDistance(wkt1, wkt2, distance, true);
  }

  private void checkWithinDistance(final String wkt1, final String wkt2, final double distance,
    final boolean expectedResult) {
    Geometry g1;
    Geometry g2;
    try {
      g1 = rdr.read(wkt1);
    } catch (final ParseException e) {
      throw new RuntimeException(e.toString());
    }
    try {
      g2 = rdr.read(wkt2);
    } catch (final ParseException e) {
      throw new RuntimeException(e.toString());
    }
    // check both orders for arguments
    checkWithinDistance(g1, g2, distance, expectedResult);
    checkWithinDistance(g2, g1, distance, expectedResult);
  }

  public void testCrossSegments() {
    checkWithinDistance("LINESTRING (0 0 0, 10 10 0 )", "LINESTRING (10 0 1, 0 10 1 )", 1);
    checkWithinDistance("LINESTRING (0 0 0, 20 20 0 )", "LINESTRING (10 0 1, 0 10 1 )", 1);
    checkWithinDistance("LINESTRING (20 10 20, 10 20 10 )", "LINESTRING (10 10 20, 20 20 10 )", 0);
  }

  public void testCrossSegmentsFlat() {
    checkWithinDistance("LINESTRING (0 0 0, 10 10 0 )", "LINESTRING (10 0 0, 0 10 0 )", 0);
    checkWithinDistance("LINESTRING (0 0 10, 30 10 10 )", "LINESTRING (10 0 10, 0 10 10 )", 0);
  }

  public void testEmpty() {
    checkWithinDistance("POINT EMPTY", "POINT EMPTY", 0);
    checkWithinDistance("LINESTRING EMPTY", "POINT (0 0 0)", 1, true);
  }

  public void testLineLine() {
    checkWithinDistance("LINESTRING (0 1 2, 1 1 1, 1 0 2 )",
      "LINESTRING (0 0 0.1, .5 .5 0, 1 1 0, 1.5 1.5 0, 2 2 0 )", 1);
    checkWithinDistance("LINESTRING (10 10 20, 20 20 30, 20 20 1, 30 30 5 )",
      "LINESTRING (1 80 10, 0 39 5, 39 0 5, 80 1 20)", 0.7071067811865476);
  }

  public void testLinePolygonFlat() {
    // line inside
    checkWithinDistance("LINESTRING (150 150 0, 160 160 0)",
      "POLYGON ((100 200 0, 200 200 0, 200 100 0, 100 100 0, 100 200 0))", 0);
    // line outside
    checkWithinDistance("LINESTRING (200 250 0, 260 260 0)",
      "POLYGON ((100 200 0, 200 200 0, 200 100 0, 100 100 0, 100 200 0))", 50);
    // line touching
    checkWithinDistance("LINESTRING (200 200 0, 260 260 0)",
      "POLYGON ((100 200 0, 200 200 0, 200 100 0, 100 100 0, 100 200 0))", 0);
  }

  public void testLinePolygonHoleFlat() {
    // line crossing hole
    checkWithinDistance("LINESTRING (150 150 10, 150 150 -10)", this.polyHoleFlat, 20, false);
    // line crossing interior
    checkWithinDistance("LINESTRING (110 110 10, 110 110 -10)", this.polyHoleFlat, 0);
  }

  public void testLinePolygonSimple() {
    // line crossing inside
    checkWithinDistance("LINESTRING (150 150 10, 150 150 -10)",
      "POLYGON ((100 200 0, 200 200 0, 200 100 0, 100 100 0, 100 200 0))", 0);
    // vertical line above
    checkWithinDistance("LINESTRING (200 200 10, 260 260 100)",
      "POLYGON ((100 200 0, 200 200 0, 200 100 0, 100 100 0, 100 200 0))", 10);
    // vertical line touching
    checkWithinDistance("LINESTRING (200 200 0, 260 260 100)",
      "POLYGON ((100 200 0, 200 200 0, 200 100 0, 100 100 0, 100 200 0))", 0);
  }

  public void testMultiLineString() {
    checkWithinDistance(
      "MULTILINESTRING ((0 0 0, 10 10 10), (0 0 100, 25 25 25, 40 40 50), (100 100 100, 100 101 102))",
      "MULTILINESTRING ((100 100 99, 100 100 99), (100 102 102, 200 200 20), (25 100 33, 25 100 35))",
      1);
  }

  public void testMultiPoint() {
    checkWithinDistance("MULTIPOINT ((0 0 0), (0 0 100), (100 100 100))",
      "MULTIPOINT ((100 100 99), (50 50 50), (25 100 33))", 1);
  }

  public void testMultiPolygon() {
    checkWithinDistance(
      // Polygons parallel to XZ plane
      "MULTIPOLYGON ( ((120 120 -10, 120 120 100, 180 120 100, 180 120 -10, 120 120 -10)), ((120 200 -10, 120 200 190, 180 200 190, 180 200 -10, 120 200 -10)) )",
      // Polygons parallel to XY plane
      "MULTIPOLYGON ( ((100 200 200, 200 200 200, 200 100 200, 100 100 200, 100 200 200)), ((100 200 210, 200 200 210, 200 100 210, 100 100 210, 100 200 210)) )",
      10);
  }

  public void testParallelSegments() {
    checkWithinDistance("LINESTRING (0 0 0, 1 0 0 )", "LINESTRING (0 0 1, 1 0 1 )", 1);
    checkWithinDistance("LINESTRING (10 10 0, 20 10 0 )", "LINESTRING (10 20 10, 20 20 10 )",
      14.142135623730951);
    checkWithinDistance("LINESTRING (10 10 0, 20 20 0 )", "LINESTRING (10 20 10, 20 30 10 )",
      12.24744871391589);
    // = distance from LINESTRING (10 10 0, 20 20 0 ) to POINT(10 20 10)
    // = hypotenuse(7.0710678118654755, 10)
  }

  public void testParallelSegmentsFlat() {
    checkWithinDistance("LINESTRING (10 10 0, 20 20 0 )", "LINESTRING (10 20 0, 20 30 0 )",
      7.0710678118654755);
  }

  public void testPointPoint() {
    checkWithinDistance("POINT (0 0 0 )", "POINT (0 0 1 )", 1);
    checkWithinDistance("POINT (0 0 0 )", "POINT (0 0 1 )", 0.5, false);
    checkWithinDistance("POINT (10 10 1 )", "POINT (11 11 2 )", 1.733);
    checkWithinDistance("POINT (10 10 0 )", "POINT (10 20 10 )", 14.143);
  }

  public void testPointPolygon() {
    // point above poly
    checkWithinDistance("POINT (150 150 10)",
      "POLYGON ((100 200 0, 200 200 0, 200 100 0, 100 100 0, 100 200 0))", 10);
    // point below poly
    checkWithinDistance("POINT (150 150 -10)",
      "POLYGON ((100 200 0, 200 200 0, 200 100 0, 100 100 0, 100 200 0))", 10);
    // point right of poly in YZ plane
    checkWithinDistance("POINT (10 150 150)",
      "POLYGON ((0 100 200, 0 200 200, 0 200 100, 0 100 100, 0 100 200))", 10);
  }

  public void testPointPolygonFlat() {
    // inside
    checkWithinDistance("POINT (150 150 0)",
      "POLYGON ((100 200 0, 200 200 0, 200 100 0, 100 100 0, 100 200 0))", 0);
    // outside
    checkWithinDistance("POINT (250 250 0)",
      "POLYGON ((100 200 0, 200 200 0, 200 100 0, 100 100 0, 100 200 0))", 70.71067811865476);
    // on
    checkWithinDistance("POINT (200 200 0)",
      "POLYGON ((100 200 0, 200 200 0, 200 100 0, 100 100 0, 100 200 0))", 0);
  }

  // ==========================================================
  // Convenience methods
  // ==========================================================

  public void testPointPolygonHoleFlat() {
    // point above poly hole
    checkWithinDistance("POINT (130 130 10)", this.polyHoleFlat, 14.143);
    // point below poly hole
    checkWithinDistance("POINT (130 130 -10)", this.polyHoleFlat, 14.143);
    // point above poly
    checkWithinDistance("POINT (110 110 100)", this.polyHoleFlat, 100);
  }

  public void testPointSeg() {
    checkWithinDistance("LINESTRING (0 0 0, 10 10 10 )", "POINT (5 5 5 )", 0);
    checkWithinDistance("LINESTRING (10 10 10, 20 20 20 )", "POINT (11 11 10 )", 0.8, false);
  }

  public void testTSegmentsFlat() {
    checkWithinDistance("LINESTRING (10 10 0, 10 20 0 )", "LINESTRING (20 15 0, 25 15 0 )", 10);
  }

}