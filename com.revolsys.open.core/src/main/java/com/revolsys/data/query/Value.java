package com.revolsys.data.query;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.data.codes.CodeTable;
import com.revolsys.data.codes.CodeTableProperty;
import com.revolsys.data.equals.Equals;
import com.revolsys.data.identifier.Identifier;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.types.DataType;
import com.revolsys.jdbc.field.JdbcFieldDefinition;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.DateUtil;

public class Value extends QueryValue {
  private FieldDefinition field;

  private Object displayValue;

  private JdbcFieldDefinition jdbcField;

  private Object queryValue;

  public Value(final FieldDefinition field, final Object value) {
    this.queryValue = value;
    this.displayValue = value;
    setField(field);

  }

  public Value(final Object value) {
    this(JdbcFieldDefinition.createField(value), value);
  }

  @Override
  public int appendParameters(final int index, final PreparedStatement statement) {
    try {
      Object sqlValue = this.queryValue;
      if (sqlValue instanceof Identifier) {
        final Identifier identifier = (Identifier)sqlValue;
        sqlValue = identifier.getValue(0);
      }
      return this.jdbcField.setPreparedStatementValue(statement, index, sqlValue);
    } catch (final SQLException e) {
      throw new RuntimeException("Unable to set value: " + this.queryValue, e);
    }
  }

  @Override
  public void appendSql(final StringBuffer buffer) {
    buffer.append('?');
  }

  @Override
  public Value clone() {
    return (Value)super.clone();
  }

  public void convert(final DataType dataType) {
    if (this.queryValue != null) {
      final Object newValue = StringConverterRegistry.toObject(dataType, this.queryValue);
      final Class<?> typeClass = dataType.getJavaClass();
      if (newValue == null || !typeClass.isAssignableFrom(newValue.getClass())) {
        throw new IllegalArgumentException(this.queryValue + " is not a valid " + typeClass);
      } else {
        this.queryValue = newValue;
      }
    }
  }

  public void convert(final FieldDefinition field) {
    if (field instanceof JdbcFieldDefinition) {
      this.jdbcField = (JdbcFieldDefinition)field;
    }
    convert(field.getType());
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Value) {
      final Value value = (Value)obj;
      return Equals.equal(value.getValue(), this.getValue());
    } else {
      return false;
    }
  }

  public Object getDisplayValue() {
    return this.displayValue;
  }

  public JdbcFieldDefinition getJdbcField() {
    return this.jdbcField;
  }

  public Object getQueryValue() {
    return this.queryValue;
  }

  @Override
  public String getStringValue(final Map<String, Object> record) {
    final Object value = getValue(record);
    if (this.field == null) {
      return StringConverterRegistry.toString(value);
    } else {
      final Class<?> typeClass = this.field.getTypeClass();
      return StringConverterRegistry.toString(typeClass, value);
    }
  }

  public Object getValue() {
    return this.queryValue;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(final Map<String, Object> record) {
    return (V)this.queryValue;
  }

  public void setField(final FieldDefinition field) {
    this.field = field;
    if (field == null) {

    } else {
      if (field instanceof JdbcFieldDefinition) {
        this.jdbcField = (JdbcFieldDefinition)field;
      } else {
        this.jdbcField = JdbcFieldDefinition.createField(this.queryValue);
      }

      CodeTable codeTable = null;
      if (field != null) {
        final RecordDefinition metaData = field.getRecordDefinition();
        if (metaData != null) {
          final String fieldName = field.getName();
          codeTable = metaData.getCodeTableByFieldName(fieldName);
          if (codeTable instanceof CodeTableProperty) {
            final CodeTableProperty codeTableProperty = (CodeTableProperty)codeTable;
            if (codeTableProperty.getRecordDefinition() == metaData) {
              codeTable = null;
            }
          }
          if (codeTable == null) {
            convert(field);
          } else {
            final Object id = codeTable.getId(this.queryValue);
            if (id == null) {
              this.displayValue = this.queryValue;
            } else {
              this.queryValue = id;
              final List<Object> values = codeTable.getValues(id);
              if (values.size() == 1) {
                this.displayValue = values.get(0);
              } else {
                this.displayValue = CollectionUtil.toString(":", values);
              }
            }
          }
        }
      }
    }
  }

  public void setValue(final Object value) {
    this.queryValue = value;
  }

  @Override
  public String toFormattedString() {
    return toString();
  }

  @Override
  public String toString() {
    if (this.displayValue instanceof Number) {
      return StringConverterRegistry.toString(this.displayValue);
    } else if (this.displayValue instanceof Date) {
      final Date date = (Date)this.displayValue;
      final String stringValue = DateUtil.format("yyyy-MM-dd ", date);
      return "{d '" + stringValue + "'}";
    } else if (this.displayValue instanceof Time) {
      final Time time = (Time)this.displayValue;
      final String stringValue = DateUtil.format("HH:mm:ss", time);
      return "{t '" + stringValue + "'}";
    } else if (this.displayValue instanceof Timestamp) {
      final Timestamp time = (Timestamp)this.displayValue;
      final String stringValue = DateUtil.format("yyyy-MM-dd HH:mm:ss.S", time);
      return "{ts '" + stringValue + "'}";
    } else if (this.displayValue instanceof java.util.Date) {
      final java.util.Date time = (java.util.Date)this.displayValue;
      final String stringValue = DateUtil.format("yyyy-MM-dd HH:mm:ss.S", time);
      return "{ts '" + stringValue + "'}";
    } else {
      final String string = StringConverterRegistry.toString(this.displayValue);
      return "'" + string.replaceAll("'", "''") + "'";
    }
  }

}
