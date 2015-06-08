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
    final RecordDefinition metaData = this.object.getRecordDefinition();
    return metaData.getFieldName(this.index);
  }

  @Override
  public Object getValue() {
    return this.object.getValue(this.index);
  }

  @Override
  public Object setValue(final Object value) {
    this.object.setValue(this.index, value);
    return value;
  }
}
