package com.revolsys.gis.algorithm.index;

import java.util.function.Consumer;

import com.vividsolutions.jts.geom.Envelope;

public final class IdObjectIndexEnvelopeVisitor<T> implements Consumer<Integer> {
  private final Envelope envelope;

  private final IdObjectIndex<T> index;

  private final Consumer<T> consumer;

  public IdObjectIndexEnvelopeVisitor(final IdObjectIndex<T> index, final Envelope envelope,
    final Consumer<T> visitor) {
    this.index = index;
    this.envelope = envelope;
    this.consumer = visitor;
  }

  @Override
  public void accept(final Integer id) {
    final T object = this.index.getObject(id);
    final Envelope e = this.index.getEnvelope(object);
    if (e.intersects(this.envelope)) {
      this.consumer.accept(object);
    }
  }
}
