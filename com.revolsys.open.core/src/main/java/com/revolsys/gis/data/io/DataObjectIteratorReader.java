package com.revolsys.gis.data.io;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.io.RecordReader;
import com.revolsys.data.record.schema.RecordDefinition;

public class DataObjectIteratorReader extends IteratorReader<Record>
  implements RecordReader {
  public DataObjectIteratorReader(final DataObjectIterator iterator) {
    super(iterator);
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    final DataObjectIterator iterator = (DataObjectIterator)iterator();
    iterator.hasNext();
    return iterator.getMetaData();
  }

  @Override
  public String toString() {
    return "Reader=" + iterator().toString();
  }
}
