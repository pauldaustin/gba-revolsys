package com.revolsys.gis.data.model;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordMapEntry;

public class RecordMap extends AbstractMap<String, Object> {
  private Record object;

  private LinkedHashSet<Entry<String, Object>> entries;

  public RecordMap() {
  }

  public RecordMap(final Record object) {
    this.object = object;
  }

  @Override
  public void clear() {
    throw new IllegalArgumentException("Cannot clear a data object map");
  }

  @Override
  public boolean containsKey(final Object name) {
    return this.object.getRecordDefinition().hasField(name.toString());
  }

  @Override
  public boolean containsValue(final Object value) {
    if (value != null) {
      for (int i = 0; i < size(); i++) {
        final Object objectValue = this.object.getValue(i);
        if (objectValue != null) {
          if (objectValue == value || objectValue.equals(value)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    if (this.entries == null) {
      this.entries = new LinkedHashSet<Entry<String, Object>>();
      for (int i = 0; i < size(); i++) {
        final RecordMapEntry entry = new RecordMapEntry(this.object, i);
        this.entries.add(entry);
      }
    }
    return this.entries;
  }

  @Override
  public Object get(final Object key) {
    return this.object.getValue(key.toString());
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public Set<String> keySet() {
    return new LinkedHashSet<String>(this.object.getRecordDefinition().getFieldNames());
  }

  @Override
  public Object put(final String key, final Object value) {
    this.object.setValue(key, value);
    return value;
  }

  @Override
  public void putAll(final Map<? extends String, ? extends Object> values) {
    for (final Entry<? extends String, ? extends Object> entry : values.entrySet()) {
      final String key = entry.getKey();
      final Object value = entry.getValue();
      put(key, value);
    }
  }

  @Override
  public Object remove(final Object key) {
    final Object value = get(key);
    this.object.setValue(key.toString(), null);
    return value;
  }

  public void setObject(final Record object) {
    this.object = object;
  }

  @Override
  public int size() {
    return this.object.getRecordDefinition().getFieldCount();
  }

  @Override
  public Collection<Object> values() {
    return this.object.getValues();
  }

}
