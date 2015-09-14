package com.revolsys.record;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.revolsys.record.schema.RecordDefinition;

public abstract class AbstractRecord extends AbstractMap<String, Object>
  implements Record, Cloneable, Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * Create a clone of the record.
   *
   * @return The cloned record.
   */
  @Override
  public AbstractRecord clone() {
    try {
      final AbstractRecord newRecord = (AbstractRecord)super.clone();
      newRecord.setState(RecordState.New);
      return newRecord;
    } catch (final CloneNotSupportedException e) {
      throw new RuntimeException("Unable to clone", e);
    }
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    final RecordDefinition recordDefinition = getRecordDefinition();
    final Set<Entry<String, Object>> entries = new LinkedHashSet<Entry<String, Object>>();
    for (int i = 0; i < recordDefinition.getFieldCount(); i++) {
      entries.add(new RecordMapEntry(this, i));
    }
    return entries;
  }

  @Override
  public boolean equals(final Object o) {
    return this == o;
  }

  @Override
  public Object get(final Object key) {
    if (key instanceof String) {
      final String name = (String)key;
      return getValue(name);
    } else {
      return null;
    }
  }

  @Override
  public Object put(final String key, final Object value) {
    final Object oldValue = getValue(key);
    setValue(key, value);
    return oldValue;
  }

  @Override
  public void putAll(final Map<? extends String, ? extends Object> values) {
    setValues(values);
  }

  /**
   * Return a String representation of the Object. There is no guarantee as to
   * the format of this string.
   *
   * @return The string value.
   */
  @Override
  public String toString() {
    final RecordDefinition recordDefinition = getRecordDefinition();
    final StringBuffer s = new StringBuffer();
    s.append(recordDefinition.getPath()).append("(\n");
    for (int i = 0; i < recordDefinition.getFieldCount(); i++) {
      final Object value = getValue(i);
      if (value != null) {
        final String fieldName = recordDefinition.getFieldName(i);
        s.append(fieldName).append('=').append(value).append('\n');
      }
    }
    s.append(')');
    return s.toString();
  }

  @SuppressWarnings("incomplete-switch")
  protected void updateState() {
    switch (getState()) {
      case Persisted:
        setState(RecordState.Modified);
      break;
      case Deleted:
        throw new IllegalStateException("Cannot modify a record which has been deleted");
    }
  }
}
