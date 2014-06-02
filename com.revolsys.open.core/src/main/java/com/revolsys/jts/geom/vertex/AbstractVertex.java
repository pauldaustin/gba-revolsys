package com.revolsys.jts.geom.vertex;

import com.revolsys.gis.data.io.IteratorReader;
import com.revolsys.io.Reader;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.Envelope;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.impl.AbstractPoint;

public abstract class AbstractVertex extends AbstractPoint implements Vertex {

  protected final Geometry geometry;

  public AbstractVertex(final Geometry geometry) {
    this.geometry = geometry;
  }

  @Override
  public AbstractVertex clone() {
    return (AbstractVertex)super.clone();
  }

  @Override
  public int getAxisCount() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    return (byte)geometryFactory.getAxisCount();
  }

  @Override
  public BoundingBox getBoundingBox() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    return new Envelope(geometryFactory, this);
  }

  @Override
  public double getCoordinate(final int axisIndex) {
    return 0;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V extends Geometry> V getGeometry() {
    return (V)geometry;
  }

  @Override
  public GeometryFactory getGeometryFactory() {
    return geometry.getGeometryFactory();
  }

  @Override
  public Vertex getLineNext() {
    return null;
  }

  @Override
  public Vertex getLinePrevious() {
    return null;
  }

  @Override
  public int getPartIndex() {
    return -1;
  }

  @Override
  public int getRingIndex() {
    return -1;
  }

  @Override
  public int getSrid() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    return geometryFactory.getSrid();
  }

  @Override
  public abstract int[] getVertexId();

  @Override
  public int getVertexIndex() {
    final int[] vertexId = getVertexId();
    return vertexId[vertexId.length - 1];
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean isFrom() {
    return getVertexIndex() == 0;
  }

  @Override
  public boolean isTo() {
    return getVertexIndex() == getVertexCount() - 1;
  }

  @Override
  public Reader<Vertex> reader() {
    return new IteratorReader<Vertex>(this);
  }
}
