/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.revolsys.gis.esri.gdb.file.capi.swig;

public enum ShapeType {
  shapeNull(0), shapePoint(1), shapePointM(21), shapePointZM(11), shapePointZ(9), shapeMultipoint(
    8), shapeMultipointM(28), shapeMultipointZM(18), shapeMultipointZ(20), shapePolyline(
    3), shapePolylineM(23), shapePolylineZM(13), shapePolylineZ(10), shapePolygon(
    5), shapePolygonM(25), shapePolygonZM(15), shapePolygonZ(19), shapeMultiPatchM(
    31), shapeMultiPatch(32), shapeGeneralPolyline(50), shapeGeneralPolygon(51), shapeGeneralPoint(
    52), shapeGeneralMultipoint(53), shapeGeneralMultiPatch(54);

  private static class SwigNext {
    private static int next = 0;
  }

  public static ShapeType swigToEnum(final int swigValue) {
    final ShapeType[] swigValues = ShapeType.class.getEnumConstants();
    if (swigValue < swigValues.length && swigValue >= 0
      && swigValues[swigValue].swigValue == swigValue) {
      return swigValues[swigValue];
    }
    for (final ShapeType swigEnum : swigValues) {
      if (swigEnum.swigValue == swigValue) {
        return swigEnum;
      }
    }
    throw new IllegalArgumentException("No enum " + ShapeType.class
      + " with value " + swigValue);
  }

  private final int swigValue;

  @SuppressWarnings("unused")
  private ShapeType() {
    this.swigValue = SwigNext.next++;
  }

  @SuppressWarnings("unused")
  private ShapeType(final int swigValue) {
    this.swigValue = swigValue;
    SwigNext.next = swigValue + 1;
  }

  @SuppressWarnings("unused")
  private ShapeType(final ShapeType swigEnum) {
    this.swigValue = swigEnum.swigValue;
    SwigNext.next = this.swigValue + 1;
  }

  public final int swigValue() {
    return this.swigValue;
  }
}
