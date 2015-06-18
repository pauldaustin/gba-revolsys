/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.40
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.gdal.gdal;

public final class XMLNodeType {
  public final static XMLNodeType CXT_Element = new XMLNodeType("CXT_Element",
    gdalJNI.CXT_Element_get());

  public final static XMLNodeType CXT_Text = new XMLNodeType("CXT_Text", gdalJNI.CXT_Text_get());

  public final static XMLNodeType CXT_Attribute = new XMLNodeType("CXT_Attribute",
    gdalJNI.CXT_Attribute_get());

  public final static XMLNodeType CXT_Comment = new XMLNodeType("CXT_Comment",
    gdalJNI.CXT_Comment_get());

  public final static XMLNodeType CXT_Literal = new XMLNodeType("CXT_Literal",
    gdalJNI.CXT_Literal_get());

  private static XMLNodeType[] swigValues = {
    CXT_Element, CXT_Text, CXT_Attribute, CXT_Comment, CXT_Literal
  };

  private static int swigNext = 0;

  public static XMLNodeType swigToEnum(final int swigValue) {
    if (swigValue < swigValues.length && swigValue >= 0
      && swigValues[swigValue].swigValue == swigValue) {
      return swigValues[swigValue];
    }
    for (final XMLNodeType swigValue2 : swigValues) {
      if (swigValue2.swigValue == swigValue) {
        return swigValue2;
      }
    }
    throw new IllegalArgumentException("No enum " + XMLNodeType.class + " with value " + swigValue);
  }

  private final int swigValue;

  private final String swigName;

  private XMLNodeType(final String swigName) {
    this.swigName = swigName;
    this.swigValue = swigNext++;
  }

  private XMLNodeType(final String swigName, final int swigValue) {
    this.swigName = swigName;
    this.swigValue = swigValue;
    swigNext = swigValue + 1;
  }

  private XMLNodeType(final String swigName, final XMLNodeType swigEnum) {
    this.swigName = swigName;
    this.swigValue = swigEnum.swigValue;
    swigNext = this.swigValue + 1;
  }

  public final int swigValue() {
    return this.swigValue;
  }

  @Override
  public String toString() {
    return this.swigName;
  }
}
