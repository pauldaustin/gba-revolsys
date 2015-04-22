package com.revolsys.gis.data.model;

import java.util.Map.Entry;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;

public class DataObjectMapEntry implements Entry<String, Object> {
  private final Record object;

  private final int index;

  public DataObjectMapEntry(final Record object, final int index) {
    this.object = object;
    this.index = index;
  }

  @Override
  public String getKey() {
    final RecordDefinition metaData = object.getRecordDefinition();
    return metaData.getAttributeName(index);
  }

  @Override
  public Object getValue() {
    return object.getValue(index);
  }

  @Override
  public Object setValue(final Object value) {
    object.setValue(index, value);
    return value;
  }
}
