package com.revolsys.gis.algorithm.index;

import java.util.List;

import java.util.function.Predicate;
import com.revolsys.visitor.CreateListVisitor;
import com.vividsolutions.jts.geom.Envelope;

public abstract class AbstractSpatialIndex<T> implements EnvelopeSpatialIndex<T> {

  @Override
  public List<T> find(final Envelope envelope) {
    final CreateListVisitor<T> visitor = new CreateListVisitor<T>();
    visit(envelope, visitor);
    return visitor.getList();
  }

  @Override
  public List<T> find(final Envelope envelope, final Predicate<T> filter) {
    final CreateListVisitor<T> visitor = new CreateListVisitor<T>();
    visit(envelope, filter, visitor);
    return visitor.getList();
  }

  @Override
  public List<T> findAll() {
    final CreateListVisitor<T> visitor = new CreateListVisitor<T>();
    visit(visitor);
    return visitor.getList();
  }
}
