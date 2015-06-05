package com.revolsys.gis.data.io;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.io.RecordReader;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.types.DataType;
import com.revolsys.gis.data.model.ArrayRecord;
import com.revolsys.io.AbstractReader;
import com.revolsys.io.Reader;

public class MapReaderDataObjectReader extends AbstractReader<Record>
  implements RecordReader, Iterator<Record> {

  private final RecordDefinition metaData;

  private final Reader<Map<String, Object>> mapReader;

  private boolean open;

  private Iterator<Map<String, Object>> mapIterator;

  public MapReaderDataObjectReader(final RecordDefinition metaData,
    final Reader<Map<String, Object>> mapReader) {
    this.metaData = metaData;
    this.mapReader = mapReader;
  }

  @Override
  public void close() {
    mapReader.close();
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    return metaData;
  }

  @Override
  public boolean hasNext() {
    if (!open) {
      open();
    }
    return mapIterator.hasNext();
  }

  @Override
  public Iterator<Record> iterator() {
    return this;
  }

  @Override
  public Record next() {
    if (hasNext()) {
      final Map<String, Object> source = mapIterator.next();
      final Record target = new ArrayRecord(metaData);
      for (final FieldDefinition attribute : metaData.getFields()) {
        final String name = attribute.getName();
        final Object value = source.get(name);
        if (value != null) {
          final DataType dataType = metaData.getFieldType(name);
          final Object convertedValue = StringConverterRegistry.toObject(
            dataType, value);
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
    open = true;
    this.mapIterator = mapReader.iterator();
  }

  @Override
  public void remove() {
    mapIterator.remove();
  }
}
