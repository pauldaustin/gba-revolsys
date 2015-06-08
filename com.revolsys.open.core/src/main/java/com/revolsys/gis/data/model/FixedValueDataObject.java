package com.revolsys.gis.data.model;

import java.util.Arrays;
import java.util.List;

import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;

public class FixedValueDataObject extends BaseRecord {
  private static final long serialVersionUID = 1L;

  private static final RecordDefinition META_DATA = new RecordDefinitionImpl();

  private final Object value;

  public FixedValueDataObject(final Object value) {
    this(META_DATA, value);
  }

  public FixedValueDataObject(final RecordDefinition metaData, final Object value) {
    super(metaData);
    this.value = value;
  }

  @Override
  public FixedValueDataObject clone() {
    final FixedValueDataObject clone = (FixedValueDataObject)super.clone();
    return clone;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getValue(final CharSequence name) {
    return (T)this.value;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Object> T getValue(final int index) {
    if (index < 0) {
      return null;
    } else {
      return (T)this.value;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getValueByPath(final CharSequence path) {
    return (T)this.value;
  }

  @Override
  public List<Object> getValues() {
    return Arrays.asList(this.value);
  }

  @Override
  public int hashCode() {
    return this.value.hashCode();
  }

  @Override
  public void setValue(final int index, final Object value) {

  }
}
