package com.revolsys.gis.esri.gdb.xml.parser;

import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.cs.epsg.EpsgCoordinateSystems;
import com.revolsys.gis.model.coordinates.CoordinatesPrecisionModel;
import com.revolsys.gis.model.coordinates.SimpleCoordinatesPrecisionModel;

public class SpatialReference {
  private String wkt;

  private double xOrigin;

  private double yOrigin;

  private double xYScale;

  private double zOrigin;

  private double zScale;

  private double mOrigin;

  private double mScale;

  private double xYTolerance;

  private double zTolerance;

  private double mTolerance;

  private boolean highPrecision;

  private double leftLongitude;

  private int wKID;

  private int latestWKID;

  public SpatialReference() {
  }

  private CoordinateSystem coordinateSystem;

  private GeometryFactory geometryFactory;

  public CoordinateSystem getCoordinateSystem() {
    if (coordinateSystem == null) {
      coordinateSystem = EpsgCoordinateSystems.getCoordinateSystem(latestWKID);
      if (coordinateSystem == null) {
        coordinateSystem = EpsgCoordinateSystems.getCoordinateSystem(wKID);
      }
    }
    return coordinateSystem;
  }

  public GeometryFactory getGeometryFactory() {
    if (geometryFactory == null) {
      final CoordinateSystem coordinateSystem = getCoordinateSystem();
      if (coordinateSystem != null) {
        final CoordinatesPrecisionModel precisionModel;
        if (xYScale == 1.1258999068426238E13) {
          precisionModel = new SimpleCoordinatesPrecisionModel(0, zScale);
        } else {
          precisionModel = new SimpleCoordinatesPrecisionModel(xYScale, zScale);
        }
        geometryFactory = new GeometryFactory(coordinateSystem, precisionModel);
      }
    }
    return geometryFactory;
  }

  public int getLatestWKID() {
    return latestWKID;
  }

  public double getLeftLongitude() {
    return leftLongitude;
  }

  public double getMOrigin() {
    return mOrigin;
  }

  public double getMScale() {
    return mScale;
  }

  public double getMTolerance() {
    return mTolerance;
  }

  public int getWKID() {
    return wKID;
  }

  public String getWKT() {
    return wkt;
  }

  public double getXOrigin() {
    return xOrigin;
  }

  public double getXYScale() {
    return xYScale;
  }

  public double getXYTolerance() {
    return xYTolerance;
  }

  public double getYOrigin() {
    return yOrigin;
  }

  public double getZOrigin() {
    return zOrigin;
  }

  public double getZScale() {
    return zScale;
  }

  public double getZTolerance() {
    return zTolerance;
  }

  public boolean isHighPrecision() {
    return highPrecision;
  }

  public void setHighPrecision(final boolean highPrecision) {
    this.highPrecision = highPrecision;
  }

  public void setLatestWKID(final int latestWKID) {
    this.latestWKID = latestWKID;
  }

  public void setLeftLongitude(final double leftLongitude) {
    this.leftLongitude = leftLongitude;
  }

  public void setMOrigin(final double mOrigin) {
    this.mOrigin = mOrigin;
  }

  public void setMScale(final double mScale) {
    this.mScale = mScale;
  }

  public void setMTolerance(final double mTolerance) {
    this.mTolerance = mTolerance;
  }

  public void setWKID(final int wkid) {
    this.wKID = wkid;
  }

  public void setWKT(final String wkt) {
    this.wkt = wkt;
  }

  public void setXOrigin(final double xOrigin) {
    this.xOrigin = xOrigin;
  }

  public void setXYScale(final double xYScale) {
    this.xYScale = xYScale;
  }

  public void setXYTolerance(final double xYTolerance) {
    this.xYTolerance = xYTolerance;
  }

  public void setYOrigin(final double yOrigin) {
    this.yOrigin = yOrigin;
  }

  public void setZOrigin(final double zOrigin) {
    this.zOrigin = zOrigin;
  }

  public void setZScale(final double zScale) {
    this.zScale = zScale;
  }

  public void setZTolerance(final double zTolerance) {
    this.zTolerance = zTolerance;
  }

  @Override
  public String toString() {
    return wkt;
  }
}
