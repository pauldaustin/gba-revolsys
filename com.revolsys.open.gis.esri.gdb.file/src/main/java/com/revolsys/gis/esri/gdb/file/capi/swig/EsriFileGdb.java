/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.revolsys.gis.esri.gdb.file.capi.swig;

public class EsriFileGdb {
  public static int CloseGeodatabase(final Geodatabase geodatabase) {
    return EsriFileGdbJNI.CloseGeodatabase(Geodatabase.getCPtr(geodatabase),
      geodatabase);
  }

  public static int closeGeodatabase2(final Geodatabase geodatabase) {
    return EsriFileGdbJNI.closeGeodatabase2(Geodatabase.getCPtr(geodatabase),
      geodatabase);
  }

  public static Geodatabase createGeodatabase(final String path) {
    final long cPtr = EsriFileGdbJNI.createGeodatabase(path);
    return cPtr == 0 ? null : new Geodatabase(cPtr, true);
  }

  public static int createGeodatabase2(final String path,
    final Geodatabase geodatabase) {
    return EsriFileGdbJNI.createGeodatabase2(path,
      Geodatabase.getCPtr(geodatabase), geodatabase);
  }

  public static int DeleteGeodatabase(final String path) {
    return EsriFileGdbJNI.DeleteGeodatabase(path);
  }

  public static int deleteGeodatabase2(final String path) {
    return EsriFileGdbJNI.deleteGeodatabase2(path);
  }

  public static String getSpatialReferenceWkt(final int srid) {
    return EsriFileGdbJNI.getSpatialReferenceWkt(srid);
  }

  public static Geodatabase openGeodatabase(final String path) {
    final long cPtr = EsriFileGdbJNI.openGeodatabase(path);
    return cPtr == 0 ? null : new Geodatabase(cPtr, true);
  }

  public static int openGeodatabase2(final String path,
    final Geodatabase geodatabase) {
    return EsriFileGdbJNI.openGeodatabase2(path,
      Geodatabase.getCPtr(geodatabase), geodatabase);
  }

  public static void setMaxOpenFiles(final int maxOpenFiles) {
    EsriFileGdbJNI.setMaxOpenFiles(maxOpenFiles);
  }

}
