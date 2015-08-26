package com.revolsys.gis.graph;

import java.util.function.Consumer;

public abstract class AbstractItemVisitor<T>
  implements Consumer<T>, com.vividsolutions.jts.index.ItemVisitor {
  @Override
  @SuppressWarnings("unchecked")
  public void visitItem(final Object item) {
    accept((T)item);
  }

}
