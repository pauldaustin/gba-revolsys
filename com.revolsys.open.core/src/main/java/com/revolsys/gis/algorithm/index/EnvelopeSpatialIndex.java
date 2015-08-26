package com.revolsys.gis.algorithm.index;

import java.util.List;

import java.util.function.Consumer;
import java.util.function.Predicate;
import com.vividsolutions.jts.geom.Envelope;

public interface EnvelopeSpatialIndex<T> {
  List<T> find(Envelope envelope);

  List<T> find(Envelope envelope, Predicate<T> filter);

  List<T> findAll();

  void put(Envelope envelope, T object);

  boolean remove(Envelope envelope, T object);

  void visit(Envelope envelope, Predicate<T> filter, Consumer<T> visitor);

  void visit(final Envelope envelope, final Consumer<T> visitor);

  void visit(final Consumer<T> visitor);
}
