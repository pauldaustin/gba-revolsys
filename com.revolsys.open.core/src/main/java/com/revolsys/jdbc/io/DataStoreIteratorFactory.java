package com.revolsys.jdbc.io;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.util.Property;

public class DataStoreIteratorFactory {

  private Reference<Object> factory;

  private String methodName;

  public DataStoreIteratorFactory() {
  }

  public DataStoreIteratorFactory(final Object factory, final String methodName) {
    this.factory = new WeakReference<Object>(factory);
    this.methodName = methodName;
  }

  public AbstractIterator<Record> createIterator(final RecordStore dataStore, final Query query,
    final Map<String, Object> properties) {
    final Object factory = this.factory.get();
    if (factory != null && StringUtils.hasText(this.methodName)) {
      return Property.invoke(factory, this.methodName, dataStore, query, properties);
    } else {
      throw new UnsupportedOperationException("Creating query iterators not supported");
    }
  }

}
