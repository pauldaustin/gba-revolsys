package com.revolsys.gis.data.model;

import java.util.Map.Entry;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;

public class RecordEntry implements Entry<String, Object> {

  private final Record record;

  private final int index;

  public RecordEntry(final Record record, final int index) {
    this.record = record;
    this.index = index;
  }

  @Override
  public String getKey() {
    final RecordDefinition recordDefinition = this.record.getRecordDefinition();
    return recordDefinition.getFieldName(this.index);
  }

  @Override
  public Object getValue() {
    return this.record.getValue(this.index);
  }

  @Override
  public Object setValue(final Object value) {
    this.record.setValue(this.index, value);
    return value;
  }
}
