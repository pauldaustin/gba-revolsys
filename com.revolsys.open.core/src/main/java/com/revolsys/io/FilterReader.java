package com.revolsys.io;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.PreDestroy;

import com.revolsys.collection.FilterIterator;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.io.RecordReader;
import com.revolsys.data.record.schema.RecordDefinition;
import java.util.function.Predicate;

public class FilterReader extends AbstractReader<Record>implements RecordReader {

  private Predicate<Record> predicate;

  private RecordReader reader;

  public FilterReader(final Predicate<Record> filter, final RecordReader reader) {
    this.predicate = filter;
    this.reader = reader;
  }

  @Override
  @PreDestroy
  public void close() {
    super.close();
    if (this.reader != null) {
      this.reader.close();
    }
    this.predicate = null;
    this.reader = null;
  }

  protected Predicate<Record> getFilter() {
    return this.predicate;
  }

  @Override
  public Map<String, Object> getProperties() {
    return this.reader.getProperties();
  }

  protected RecordReader getReader() {
    return this.reader;
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    return this.reader.getRecordDefinition();
  }

  @Override
  public Iterator<Record> iterator() {
    final Iterator<Record> iterator = this.reader.iterator();
    return new FilterIterator<Record>(this.predicate, iterator);
  }

  @Override
  public void open() {
    this.reader.open();
  }

}
