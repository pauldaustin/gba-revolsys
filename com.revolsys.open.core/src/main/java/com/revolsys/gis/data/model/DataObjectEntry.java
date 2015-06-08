package com.revolsys.gis.data.model;

import java.util.Map.Entry;

import com.revolsys.data.record.Record;

public class DataObjectEntry implements Entry<String, Object> {

  private final Record dataObject;

  private final int index;

  public DataObjectEntry(final Record dataObject, final int index) {
    this.dataObject = dataObject;
    this.index = index;
  }

  @Override
  public String getKey() {
    return this.dataObject.getRecordDefinition().getFieldName(this.index);
  }

  @Override
  public Object getValue() {
    return this.dataObject.getValue(this.index);
  }

  @Override
  public Object setValue(final Object value) {
    this.dataObject.setValue(this.index, value);
    return value;
  }
}
