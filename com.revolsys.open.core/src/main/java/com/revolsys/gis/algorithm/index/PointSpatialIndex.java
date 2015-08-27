package com.revolsys.gis.algorithm.index;

import java.util.List;

import java.util.function.Consumer;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.vividsolutions.jts.geom.Envelope;

public interface PointSpatialIndex<T> extends Iterable<T> {
  List<T> find(Envelope envelope);

  List<T> findAll();

  void put(Coordinates point, T object);

  boolean remove(Coordinates point, T object);

  void visit(final Envelope envelope, final Consumer<T> visitor);

  void visit(final Consumer<T> visitor);
}
