/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.5
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.revolsys.gis.esri.gdb.file.capi.swig;

public class VectorOfWString {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected VectorOfWString(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(VectorOfWString obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        EsriFileGdbJNI.delete_VectorOfWString(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public VectorOfWString() {
    this(EsriFileGdbJNI.new_VectorOfWString__SWIG_0(), true);
  }

  public VectorOfWString(long n) {
    this(EsriFileGdbJNI.new_VectorOfWString__SWIG_1(n), true);
  }

  public long size() {
    return EsriFileGdbJNI.VectorOfWString_size(swigCPtr, this);
  }

  public long capacity() {
    return EsriFileGdbJNI.VectorOfWString_capacity(swigCPtr, this);
  }

  public void reserve(long n) {
    EsriFileGdbJNI.VectorOfWString_reserve(swigCPtr, this, n);
  }

  public boolean isEmpty() {
    return EsriFileGdbJNI.VectorOfWString_isEmpty(swigCPtr, this);
  }

  public void clear() {
    EsriFileGdbJNI.VectorOfWString_clear(swigCPtr, this);
  }

  public void add(String x) {
    EsriFileGdbJNI.VectorOfWString_add(swigCPtr, this, x);
  }

  public String get(int i) {
    return EsriFileGdbJNI.VectorOfWString_get(swigCPtr, this, i);
  }

  public void set(int i, String val) {
    EsriFileGdbJNI.VectorOfWString_set(swigCPtr, this, i, val);
  }

}
