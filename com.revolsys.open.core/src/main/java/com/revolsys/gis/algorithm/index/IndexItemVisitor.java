package com.revolsys.gis.algorithm.index;

import java.util.function.Consumer;

import com.revolsys.data.record.Record;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.ItemVisitor;

/**
 * A {@link ItemVisitor} implementation which uses a {@link Consumer} to visit
 * each item.
 *
 * @author Paul Austin
 * @param <T> The type of item to visit.
 */
public class IndexItemVisitor implements ItemVisitor {
  private final Envelope envelope;

  private final Consumer<Record> consumer;

  public IndexItemVisitor(final Envelope envelope, final Consumer<Record> visitor) {
    this.envelope = envelope;
    this.consumer = visitor;
  }

  @Override
  public void visitItem(final Object item) {
    final Record object = (Record)item;
    final Envelope envelope = object.getGeometry().getEnvelopeInternal();
    if (envelope.intersects(this.envelope)) {
      this.consumer.accept(object);
    }
  }
}
