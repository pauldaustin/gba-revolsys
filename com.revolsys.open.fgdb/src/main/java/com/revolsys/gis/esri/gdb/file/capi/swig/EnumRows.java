/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.5
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.revolsys.gis.esri.gdb.file.capi.swig;

public class EnumRows {
  protected static long getCPtr(EnumRows obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }
  protected boolean swigCMemOwn;

  private long swigCPtr;

  public EnumRows() {
    this(EsriFileGdbJNI.new_EnumRows(), true);
  }

   protected EnumRows(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  public void Close() {
    EsriFileGdbJNI.EnumRows_Close(swigCPtr, this);
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        EsriFileGdbJNI.delete_EnumRows(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  protected void finalize() {
   }

  public Row next() {
    long cPtr = EsriFileGdbJNI.EnumRows_next(swigCPtr, this);
    return (cPtr == 0) ? null : new Row(cPtr, true);
  }

}
