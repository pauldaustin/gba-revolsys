package com.revolsys.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.revolsys.collection.iterator.AbstractIterator;
import java.util.function.Predicate;

public class FilterIterator<T> extends AbstractIterator<T> {

  private Predicate<T> predicate;

  private Iterator<T> iterator;

  public FilterIterator(final Predicate<T> filter, final Iterator<T> iterator) {
    this.predicate = filter;
    this.iterator = iterator;
  }

  @Override
  protected void doClose() {
    super.doClose();
    if (this.iterator instanceof AbstractIterator) {
      final AbstractIterator<T> abstractIterator = (AbstractIterator<T>)this.iterator;
      abstractIterator.close();
    }
    this.predicate = null;
    this.iterator = null;
  }

  protected Predicate<T> getFilter() {
    return this.predicate;
  }

  protected Iterator<T> getIterator() {
    return this.iterator;
  }

  @Override
  protected T getNext() throws NoSuchElementException {
    while (this.iterator != null && this.iterator.hasNext()) {
      final T value = this.iterator.next();
      if (this.predicate == null || this.predicate.test(value)) {
        return value;
      }
    }
    throw new NoSuchElementException();
  }
}
