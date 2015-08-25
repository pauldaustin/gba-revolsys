package com.revolsys.data.io;

import java.util.Iterator;
import java.util.Map;

import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.io.AbstractReader;
import com.revolsys.properties.ObjectWithProperties;

public class IteratorReader<T> extends AbstractReader<T> {

  private Iterator<T> iterator;

  private ObjectWithProperties object;

  public IteratorReader() {
  }

  public IteratorReader(final Iterator<T> iterator) {
    this.iterator = iterator;
    if (iterator instanceof ObjectWithProperties) {
      this.object = (ObjectWithProperties)iterator;
    }
  }

  @Override
  public void close() {
    try {
      if (this.iterator instanceof AbstractIterator) {
        final AbstractIterator<T> i = (AbstractIterator<T>)this.iterator;
        i.close();
      }
    } finally {
      this.iterator = null;
    }
  }

  @Override
  public Map<String, Object> getProperties() {
    if (this.object == null) {
      return super.getProperties();
    } else {
      return this.object.getProperties();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <C> C getProperty(final String name) {
    if (this.object == null) {
      return (C)super.getProperty(name);
    } else {
      return (C)this.object.getProperty(name);
    }
  }

  @Override
  public Iterator<T> iterator() {
    return this.iterator;
  }

  @Override
  public void open() {
    this.iterator.hasNext();
  }

  protected void setIterator(final Iterator<T> iterator) {
    this.iterator = iterator;
  }

  @Override
  public void setProperty(final String name, final Object value) {
    if (this.object == null) {
      super.setProperty(name, value);
    } else {
      this.object.setProperty(name, value);
    }
  }
}
