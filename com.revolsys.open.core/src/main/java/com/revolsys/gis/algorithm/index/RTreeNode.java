package com.revolsys.gis.algorithm.index;

import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Envelope;

public abstract class RTreeNode<T> extends Envelope {

  /**
   *
   */
  private static final long serialVersionUID = -8110404083135361671L;

  public RTreeNode() {
  }

  public abstract void forEach(Envelope envelope, Consumer<T> visitor);

  public abstract void forEach(Envelope envelope, Predicate<T> filter, Consumer<T> visitor);

  public abstract void forEachNode(Consumer<T> visitor);

  public abstract boolean remove(LinkedList<RTreeNode<T>> path, Envelope envelope, T object);

  @Override
  public String toString() {
    return new BoundingBox(GeometryFactory.getFactory(), this).toPolygon(1).toString();
  }

  protected abstract void updateEnvelope();
}
