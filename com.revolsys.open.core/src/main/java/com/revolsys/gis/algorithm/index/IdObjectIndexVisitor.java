package com.revolsys.gis.algorithm.index;

import java.util.function.Consumer;

public final class IdObjectIndexVisitor<T> implements Consumer<Integer> {
  private final IdObjectIndex<T> index;

  private final Consumer<T> consumer;

  public IdObjectIndexVisitor(final IdObjectIndex<T> index, final Consumer<T> visitor) {
    this.index = index;
    this.consumer = visitor;
  }

  @Override
  public void accept(final Integer id) {
    final T object = this.index.getObject(id);
    this.consumer.accept(object);
  }
}
