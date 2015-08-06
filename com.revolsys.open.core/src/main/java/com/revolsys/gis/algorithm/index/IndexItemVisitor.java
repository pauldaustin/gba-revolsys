package com.revolsys.gis.algorithm.index;

import com.revolsys.collection.Visitor;
import com.revolsys.data.record.Record;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.ItemVisitor;

/**
 * A {@link ItemVisitor} implementation which uses a {@link Visitor} to visit
 * each item.
 *
 * @author Paul Austin
 * @param <T> The type of item to visit.
 */
public class IndexItemVisitor implements ItemVisitor {
  private final Visitor<Record> visitor;

  private final Envelope envelope;

  public IndexItemVisitor(final Envelope envelope, final Visitor<Record> visitor) {
    this.envelope = envelope;
    this.visitor = visitor;
  }

  @Override
  public void visitItem(final Object item) {
    final Record object = (Record)item;
    final Envelope envelope = object.getGeometry().getEnvelopeInternal();
    if (envelope.intersects(this.envelope)) {
      this.visitor.visit(object);
    }
  }
}
