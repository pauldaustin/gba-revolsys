package com.revolsys.gis.data.io;

import java.util.Iterator;
import java.util.Map;
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

public class MapReaderRecordReader extends AbstractReader<Record>
  implements RecordReader, Iterator<Record> {

  private Iterator<Map<String, Object>> mapIterator;

  private final Reader<Map<String, Object>> mapReader;

  private final RecordDefinition metaData;

  private boolean open;

  public MapReaderRecordReader(final RecordDefinition metaData,
    final Reader<Map<String, Object>> mapReader) {
    this.metaData = metaData;
    this.mapReader = mapReader;
  }

  @Override
  public void close() {
    this.mapReader.close();
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
      final Record target = new ArrayRecord(this.metaData);
      for (final FieldDefinition attribute : this.metaData.getFields()) {
        final String name = attribute.getName();
        final Object value = source.get(name);
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
    this.mapIterator = this.mapReader.iterator();
  }

  @Override
  public void remove() {
    this.mapIterator.remove();
  }
}
