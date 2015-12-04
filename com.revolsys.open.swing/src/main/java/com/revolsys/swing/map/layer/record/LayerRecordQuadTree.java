package com.revolsys.swing.map.layer.record;

import com.revolsys.geometry.algorithm.index.RecordQuadTree;
import com.revolsys.geometry.model.GeometryFactory;

public class LayerRecordQuadTree extends RecordQuadTree<LayerRecord> {
  private static final long serialVersionUID = 1L;

  public LayerRecordQuadTree() {
  }

  public LayerRecordQuadTree(final GeometryFactory geometryFactory) {
    super(geometryFactory);
  }

  public LayerRecordQuadTree(final GeometryFactory geometryFactory,
    final Iterable<? extends LayerRecord> records) {
    super(geometryFactory, records);
  }

  public LayerRecordQuadTree(final Iterable<? extends LayerRecord> records) {
    super(records);
  }

  @Override
  protected boolean equalsItem(final LayerRecord record1, final LayerRecord record2) {
    if (record1 == null || record2 == null) {
      return record1 == record2;
    } else {
      return record1.isSame(record2);
    }
  }
}
