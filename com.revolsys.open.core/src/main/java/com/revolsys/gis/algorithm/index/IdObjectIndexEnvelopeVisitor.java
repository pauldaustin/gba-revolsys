package com.revolsys.gis.algorithm.index;

import com.revolsys.collection.Visitor;
import com.vividsolutions.jts.geom.Envelope;

public final class IdObjectIndexEnvelopeVisitor<T> implements Visitor<Integer> {
  private final Envelope envelope;

  private final IdObjectIndex<T> index;

  private final Visitor<T> visitor;

  public IdObjectIndexEnvelopeVisitor(final IdObjectIndex<T> index, final Envelope envelope,
    final Visitor<T> visitor) {
    this.index = index;
    this.envelope = envelope;
    this.visitor = visitor;
  }

  @Override
  public boolean visit(final Integer id) {
    final T object = this.index.getObject(id);
    final Envelope e = this.index.getEnvelope(object);
    if (e.intersects(this.envelope)) {
      if (!this.visitor.visit(object)) {
        return false;
      }
    }
    return true;
  }
}
