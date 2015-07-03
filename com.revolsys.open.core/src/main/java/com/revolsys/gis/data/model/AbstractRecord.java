package com.revolsys.gis.data.model;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Set;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordState;
import com.revolsys.data.record.schema.RecordDefinition;

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
      entries.add(new RecordEntry(this, i));
    }
    return entries;
  }

  @Override
  public boolean equals(final Object o) {
    return this == o;
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
        String fieldName = recordDefinition.getFieldName(i);
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
