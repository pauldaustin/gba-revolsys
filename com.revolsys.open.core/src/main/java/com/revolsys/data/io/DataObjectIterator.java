package com.revolsys.data.io;

import java.util.Iterator;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;

public interface DataObjectIterator extends Iterator<Record> {
  public RecordDefinition getMetaData();
}
