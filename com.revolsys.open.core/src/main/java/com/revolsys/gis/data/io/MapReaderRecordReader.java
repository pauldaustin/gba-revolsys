package com.revolsys.gis.data.io;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.datatype.DataType;
import com.revolsys.io.AbstractReader;
import com.revolsys.io.Reader;
import com.revolsys.record.ArrayRecord;
import com.revolsys.record.Record;
import com.revolsys.record.io.RecordReader;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;

public class MapReaderRecordReader extends AbstractReader<Record>
  implements RecordReader, Iterator<Record> {

  private Iterator<Map<String, Object>> mapIterator;

  private final Reader<Map<String, Object>> mapReader;

  private final RecordDefinition recordDefinition;

  private boolean open;

  public MapReaderRecordReader(final RecordDefinition recordDefinition,
    final Reader<Map<String, Object>> mapReader) {
    this.recordDefinition = recordDefinition;
    this.mapReader = mapReader;
  }

  @Override
  public void close() {
    this.mapReader.close();
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    return this.recordDefinition;
  }

  @Override
  public boolean hasNext() {
    if (!this.open) {
      open();
    }
    return this.mapIterator.hasNext();
  }

  @Override
  public Iterator<Record> iterator() {
    return this;
  }

  @Override
  public Record next() {
    if (hasNext()) {
      final Map<String, Object> source = this.mapIterator.next();
      final Record target = new ArrayRecord(this.recordDefinition);
      for (final FieldDefinition attribute : this.recordDefinition.getFields()) {
        final String name = attribute.getName();
        final Object value = source.get(name);
        if (value != null) {
          final DataType dataType = this.recordDefinition.getFieldType(name);
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
    this.mapIterator = this.mapReader.iterator();
  }

  @Override
  public void remove() {
    this.mapIterator.remove();
  }
}