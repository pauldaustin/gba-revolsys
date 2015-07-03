package com.revolsys.gis.data.model;

import java.io.Serializable;
import java.util.AbstractMap;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordState;
import com.revolsys.data.record.schema.FieldDefinition;
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

  public FieldDefinition getFieldDefinition(final int fieldIndex) {
    final RecordDefinition recordDefinition = getRecordDefinition();
    return recordDefinition.getField(fieldIndex);
  }

  @Override
  public void validateField(final int fieldIndex) {
    final FieldDefinition field = getFieldDefinition(fieldIndex);
    if (field != null) {
      final Object value = getValue(fieldIndex);
      field.validate(this, value);
    }
  }

}
