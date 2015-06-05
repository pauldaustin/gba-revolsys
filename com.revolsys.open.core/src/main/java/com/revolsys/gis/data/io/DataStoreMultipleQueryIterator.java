package com.revolsys.gis.data.io;

import java.util.NoSuchElementException;

import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.collection.iterator.AbstractMultipleIterator;
import com.revolsys.data.record.Record;

public class DataStoreMultipleQueryIterator extends
  AbstractMultipleIterator<Record> {

  private DataObjectStoreQueryReader reader;

  private int queryIndex = 0;

  public DataStoreMultipleQueryIterator(final DataObjectStoreQueryReader reader) {
    this.reader = reader;
  }

  @Override
  public void doClose() {
    super.doClose();
    reader = null;
  }

  @Override
  public AbstractIterator<Record> getNextIterator()
    throws NoSuchElementException {
    if (reader == null) {
      throw new NoSuchElementException();
    } else {
      final AbstractIterator<Record> iterator = reader.createQueryIterator(queryIndex);
      queryIndex++;
      return iterator;
    }
  }

}
