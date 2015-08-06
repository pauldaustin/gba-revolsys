package com.revolsys.gis.data.io;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.data.record.ArrayRecord;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.io.RecordReader;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.types.DataType;
import com.revolsys.io.AbstractReader;
import com.revolsys.io.Reader;

public class RecordDefinitionConvertRecordReader extends AbstractReader<Record> implements
  RecordReader, Iterator<Record> {

  private final RecordDefinition metaData;

  private final Reader<Record> reader;

  private boolean open;

  private Iterator<Record> iterator;

  public RecordDefinitionConvertRecordReader(final RecordDefinition metaData,
    final Reader<Record> reader) {
    this.metaData = metaData;
    this.reader = reader;
  }

  @Override
  public void close() {
    this.reader.close();
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    return this.metaData;
  }

  @Override
  public boolean hasNext() {
    if (!this.open) {
      open();
    }
    return this.iterator.hasNext();
  }

  @Override
  public Iterator<Record> iterator() {
    return this;
  }

  @Override
  public Record next() {
    if (hasNext()) {
      final Record source = this.iterator.next();
      final Record target = new ArrayRecord(this.metaData);
      for (final FieldDefinition attribute : this.metaData.getFields()) {
        final String name = attribute.getName();
        final Object value = source.getValue(name);
        if (value != null) {
          final DataType dataType = this.metaData.getFieldType(name);
          final Object convertedValue = StringConverterRegistry.toObject(dataType, value);
          target.setValue(name, convertedValue);
        }
      }
      return target;
    } else {
      throw new NoSuchElementException();
    }
  }

  @Override
  public void open() {
    this.open = true;
    this.iterator = this.reader.iterator();
  }

  @Override
  public void remove() {
    this.iterator.remove();
  }
}
