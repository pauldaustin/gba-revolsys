/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.5
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.revolsys.gis.esri.gdb.file.capi.swig;

public class Row {
  protected static long getCPtr(final Row obj) {
    return obj == null ? 0 : obj.swigCPtr;
  }

  protected boolean swigCMemOwn;

  private long swigCPtr;

  public Row() {
    this(EsriFileGdbJNI.new_Row(), true);
  }

  protected Row(final long cPtr, final boolean cMemoryOwn) {
    this.swigCMemOwn = cMemoryOwn;
    this.swigCPtr = cPtr;
  }

  public synchronized void delete() {
    if (this.swigCPtr != 0) {
      if (this.swigCMemOwn) {
        this.swigCMemOwn = false;
        EsriFileGdbJNI.delete_Row(this.swigCPtr);
      }
      this.swigCPtr = 0;
    }
  }

  @Override
  protected void finalize() {
    delete();
  }

  public long getDate(final String name) {
    return EsriFileGdbJNI.Row_getDate(this.swigCPtr, this, name);
  }

  public double getDouble(final String name) {
    return EsriFileGdbJNI.Row_getDouble(this.swigCPtr, this, name);
  }

  public int GetFieldInformation(final FieldInfo fieldInfo) {
    return EsriFileGdbJNI.Row_GetFieldInformation(this.swigCPtr, this,
      FieldInfo.getCPtr(fieldInfo), fieldInfo);
  }

  public VectorOfFieldDef getFields() {
    return new VectorOfFieldDef(EsriFileGdbJNI.Row_getFields(this.swigCPtr,
      this), true);
  }

  public float getFloat(final String name) {
    return EsriFileGdbJNI.Row_getFloat(this.swigCPtr, this, name);
  }

  public byte[] getGeometry() {
    return EsriFileGdbJNI.Row_getGeometry(this.swigCPtr, this);
  }

  public Guid getGlobalId() {
    return new Guid(EsriFileGdbJNI.Row_getGlobalId(this.swigCPtr, this), true);
  }

  public Guid getGuid(final String name) {
    return new Guid(EsriFileGdbJNI.Row_getGuid(this.swigCPtr, this, name), true);
  }

  public int getInteger(final String name) {
    return EsriFileGdbJNI.Row_getInteger(this.swigCPtr, this, name);
  }

  public int getOid() {
    return EsriFileGdbJNI.Row_getOid(this.swigCPtr, this);
  }

  public short getShort(final String name) {
    return EsriFileGdbJNI.Row_getShort(this.swigCPtr, this, name);
  }

  public String getString(final String name) {
    return EsriFileGdbJNI.Row_getString(this.swigCPtr, this, name);
  }

  public String getXML(final String name) {
    return EsriFileGdbJNI.Row_getXML(this.swigCPtr, this, name);
  }

  public boolean isNull(final String name) {
    return EsriFileGdbJNI.Row_isNull(this.swigCPtr, this, name);
  }

  public void setDate(final String name, final long date) {
    EsriFileGdbJNI.Row_setDate(this.swigCPtr, this, name, date);
  }

  public void setDouble(final String name, final double value) {
    EsriFileGdbJNI.Row_setDouble(this.swigCPtr, this, name, value);
  }

  public void setFloat(final String name, final double value) {
    EsriFileGdbJNI.Row_setFloat(this.swigCPtr, this, name, value);
  }

  public void setGeometry(final byte[] byteArray) {
    EsriFileGdbJNI.Row_setGeometry(this.swigCPtr, this, byteArray);
  }

  public void setGuid(final String name, final Guid value) {
    EsriFileGdbJNI.Row_setGuid(this.swigCPtr, this, name, Guid.getCPtr(value),
      value);
  }

  public void setInteger(final String name, final int value) {
    EsriFileGdbJNI.Row_setInteger(this.swigCPtr, this, name, value);
  }

  public void setNull(final String name) {
    EsriFileGdbJNI.Row_setNull(this.swigCPtr, this, name);
  }

  public void setShort(final String name, final short value) {
    EsriFileGdbJNI.Row_setShort(this.swigCPtr, this, name, value);
  }

  public void setString(final String name, final String value) {
    EsriFileGdbJNI.Row_setString(this.swigCPtr, this, name, value);
  }

  public void setXML(final String name, final String value) {
    EsriFileGdbJNI.Row_setXML(this.swigCPtr, this, name, value);
  }

}
