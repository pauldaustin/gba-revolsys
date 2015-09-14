package com.revolsys.record.io;

import com.revolsys.io.IteratorReader;
import com.revolsys.record.Record;
import com.revolsys.record.schema.RecordDefinition;

public class RecordIteratorReader extends IteratorReader<Record>implements RecordReader {
  public RecordIteratorReader(final RecordIterator iterator) {
    super(iterator);
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    final RecordIterator iterator = (RecordIterator)iterator();
    iterator.hasNext();
    return iterator.getRecordDefinition();
  }

  @Override
  public String toString() {
    return "Reader=" + iterator().toString();
  }
}
