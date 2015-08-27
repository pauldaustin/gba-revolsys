package com.revolsys.gis.wms.capabilities;

import com.vividsolutions.jts.geom.Envelope;

public class BoundingBox {
  private Envelope envelope;

  private double resX;

  private double resY;

  private String srs;

  public Envelope getEnvelope() {
    return this.envelope;
  }

  public double getResX() {
    return this.resX;
  }

  public double getResY() {
    return this.resY;
  }

  public String getSrs() {
    return this.srs;
  }

  public void setEnvelope(final Envelope envelope) {
    this.envelope = envelope;
  }

  public void setResX(final double resX) {
    this.resX = resX;
  }

  public void setResY(final double resY) {
    this.resY = resY;
  }

  public void setSrs(final String srs) {
    this.srs = srs;
  }

}
