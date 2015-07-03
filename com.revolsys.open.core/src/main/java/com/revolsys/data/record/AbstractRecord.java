package com.revolsys.data.record;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractRecord extends AbstractMap<String, Object>
  implements Record, Cloneable {

  /**
   * Create a clone of the object.
   *
   * @return The cloned object.
   */
  @Override
  public AbstractRecord clone() {
    try {
      final AbstractRecord newObject = (AbstractRecord)super.clone();
      newObject.setState(RecordState.New);
      return newObject;
    } catch (final CloneNotSupportedException e) {
      throw new RuntimeException("Unable to clone", e);
    }
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    final Set<Entry<String, Object>> entries = new LinkedHashSet<Entry<String, Object>>();
    for (int i = 0; i < this.getRecordDefinition().getFieldCount(); i++) {
      entries.add(new RecordMapEntry(this, i));
    }
    return entries;
  }

  @Override
  public boolean equals(final Object o) {
    return this == o;
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
    final StringBuilder s = new StringBuilder();
    s.append(this.getRecordDefinition().getPath()).append("(\n");
    for (int i = 0; i < this.getRecordDefinition().getFieldCount(); i++) {
      final Object value = getValue(i);
      if (value != null) {
        s.append(this.getRecordDefinition().getFieldName(i)).append('=').append(value).append('\n');
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
        throw new IllegalStateException("Cannot modify an object which has been deleted");
    }
  }

}
