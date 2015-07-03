package com.revolsys.data.record;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.revolsys.data.equals.EqualsInstance;
import com.revolsys.data.record.schema.RecordDefinition;

/**
 * The ArrayRecord is an implementation of {@link Record} which uses an
 * array of Objects as the storage for the field values.
 */
public class ArrayRecord extends BaseRecord {
  /** Serialization version */
  private static final long serialVersionUID = 1L;

  /** The object's field values. */
  private Object[] values;

  /**
   * Construct a new ArrayRecord as a deep clone of the field values.
   * Objects can only be cloned if they have a publicly accessible
   * {@link #clone()} method.
   *
   * @param record The object to clone.
   */
  public ArrayRecord(final Record record) {
    this(record.getRecordDefinition(), record);
  }

  /**
   * Construct a new empty ArrayRecord using the recordDefinition.
   *
   * @param recordDefinition The recordDefinition defining the object type.
   */
  public ArrayRecord(final RecordDefinition recordDefinition) {
    this(recordDefinition, null);
  }

  public ArrayRecord(final RecordDefinition recordDefinition,
    final Map<String, ? extends Object> values) {
    super(recordDefinition);
    if (recordDefinition == null) {
      this.values = new Object[0];
    } else {
      final int fieldCount = recordDefinition.getFieldCount();
      this.values = new Object[fieldCount];
      final Map<String, Object> defaultValues = recordDefinition.getDefaultValues();
      setValuesByPath(defaultValues);
      setValues(values);
    }
    setState(RecordState.New);
  }

  /**
   * Create a clone of the object.
   *
   * @return The cloned object.
   */
  @Override
  public ArrayRecord clone() {
    final ArrayRecord clone = (ArrayRecord)super.clone();
    clone.values = this.values.clone();
    return clone;
  }

  /**
   * Get the value of the field with the specified index.
   *
   * @param index The index of the field.
   * @return The field value.
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T extends Object> T getValue(final int index) {
    if (index < 0) {
      return null;
    } else {
      return (T)this.values[index];
    }
  }

  /**
   * Get the values of all values.
   *
   * @return The field value.
   */
  @Override
  public List<Object> getValues() {
    return Arrays.asList(this.values);
  }

  @Override
  public int hashCode() {
    return this.values.hashCode();
  }

  /**
   * Set the value of the field with the specified name.
   *
   * @param index The index of the field. param value The field value.
   * @param value The new value.
   */
  @Override
  public void setValue(final int index, final Object value) {
    if (index >= 0) {
      final Object oldValue = this.values[index];
      if (!EqualsInstance.INSTANCE.equals(oldValue, value)) {
        updateState();
      }
      this.values[index] = value;
    }
  }
}
