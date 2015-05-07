/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.5
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.revolsys.gis.esri.gdb.file.capi.swig;

public class Row {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected Row(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(Row obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        EsriFileGdbJNI.delete_Row(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public Row() {
    this(EsriFileGdbJNI.new_Row(), true);
  }

  public boolean isNull(String name) {
    return EsriFileGdbJNI.Row_isNull(swigCPtr, this, name);
  }

  public void setNull(String name) {
    EsriFileGdbJNI.Row_setNull(swigCPtr, this, name);
  }

  public long getDate(String name) {
    return EsriFileGdbJNI.Row_getDate(swigCPtr, this, name);
  }

  public void setDate(String name, long date) {
    EsriFileGdbJNI.Row_setDate(swigCPtr, this, name, date);
  }

  public double getDouble(String name) {
    return EsriFileGdbJNI.Row_getDouble(swigCPtr, this, name);
  }

  public void setDouble(String name, double value) {
    EsriFileGdbJNI.Row_setDouble(swigCPtr, this, name, value);
  }

  public float getFloat(String name) {
    return EsriFileGdbJNI.Row_getFloat(swigCPtr, this, name);
  }

  public void setFloat(String name, double value) {
    EsriFileGdbJNI.Row_setFloat(swigCPtr, this, name, value);
  }

  public Guid getGuid(String name) {
    return new Guid(EsriFileGdbJNI.Row_getGuid(swigCPtr, this, name), true);
  }

  public Guid getGlobalId() {
    return new Guid(EsriFileGdbJNI.Row_getGlobalId(swigCPtr, this), true);
  }

  public void setGuid(String name, Guid value) {
    EsriFileGdbJNI.Row_setGuid(swigCPtr, this, name, Guid.getCPtr(value), value);
  }

  public int getOid() {
    return EsriFileGdbJNI.Row_getOid(swigCPtr, this);
  }

  public short getShort(String name) {
    return EsriFileGdbJNI.Row_getShort(swigCPtr, this, name);
  }

  public void setShort(String name, short value) {
    EsriFileGdbJNI.Row_setShort(swigCPtr, this, name, value);
  }

  public int getInteger(String name) {
    return EsriFileGdbJNI.Row_getInteger(swigCPtr, this, name);
  }

  public void setInteger(String name, int value) {
    EsriFileGdbJNI.Row_setInteger(swigCPtr, this, name, value);
  }

  public String getString(String name) {
    return EsriFileGdbJNI.Row_getString(swigCPtr, this, name);
  }

  public void setString(String name, String value) {
    EsriFileGdbJNI.Row_setString(swigCPtr, this, name, value);
  }

  public String getXML(String name) {
    return EsriFileGdbJNI.Row_getXML(swigCPtr, this, name);
  }

  public void setXML(String name, String value) {
    EsriFileGdbJNI.Row_setXML(swigCPtr, this, name, value);
  }

  public byte[] getGeometry() {
  return EsriFileGdbJNI.Row_getGeometry(swigCPtr, this);
}

  public void setGeometry(byte[] byteArray) {
    EsriFileGdbJNI.Row_setGeometry(swigCPtr, this, byteArray);
  }

}