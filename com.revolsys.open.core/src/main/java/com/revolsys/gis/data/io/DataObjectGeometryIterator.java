package com.revolsys.gis.data.io;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.revolsys.data.record.Record;
import com.revolsys.io.DelegatingObjectWithProperties;
import com.vividsolutions.jts.geom.Geometry;

public class DataObjectGeometryIterator extends DelegatingObjectWithProperties implements
Iterator<Geometry> {
  private Iterator<Record> iterator;

  public DataObjectGeometryIterator(final Iterator<Record> iterator) {
    super(iterator);
    this.iterator = iterator;
  }

  @Override
  public void close() {
    super.close();
    this.iterator = null;
  }

  @Override
  public boolean hasNext() {
    return this.iterator.hasNext();
  }

  @Override
  public Geometry next() {
    if (this.iterator.hasNext()) {
      final Record dataObject = this.iterator.next();
      return dataObject.getGeometryValue();
    } else {
      throw new NoSuchElementException();
    }
  }

  @Override
  public void remove() {
    this.iterator.remove();
  }
}
