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
    return dataObject.getRecordDefinition().getFieldName(index);
  }

  @Override
  public Object getValue() {
    return dataObject.getValue(index);
  }

  @Override
  public Object setValue(final Object value) {
    dataObject.setValue(index, value);
    return value;
  }
}
