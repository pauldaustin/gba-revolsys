package com.revolsys.jdbc.field;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.springframework.asm.Type;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.types.DataType;
import com.revolsys.data.types.DataTypes;

public class JdbcFieldDefinition extends FieldDefinition {

  public static JdbcFieldDefinition createField(final Object value) {
    if (value == null) {
      return new JdbcFieldDefinition(null, DataTypes.OBJECT, Types.OTHER, 0, 0,
        false, null, null);
    } else if (value instanceof CharSequence) {
      return new JdbcStringFieldDefinition(null, Types.CHAR, -1, false, null, null);
    } else if (value instanceof BigInteger) {
      return new JdbcLongFieldDefinition(null, Types.BIGINT, -1, false, null, null);
    } else if (value instanceof Long) {
      return new JdbcLongFieldDefinition(null, Types.BIGINT, -1, false, null, null);
    } else if (value instanceof Integer) {
      return new JdbcIntegerFieldDefinition(null, Types.INTEGER, -1, false, null,
        null);
    } else if (value instanceof Short) {
      return new JdbcShortFieldDefinition(null, Types.SMALLINT, -1, false, null, null);
    } else if (value instanceof Byte) {
      return new JdbcByteFieldDefinition(null, Types.TINYINT, -1, false, null, null);
    } else if (value instanceof Double) {
      return new JdbcDoubleFieldDefinition(null, Type.DOUBLE, -1, false, null, null);
    } else if (value instanceof Float) {
      return new JdbcFloatFieldDefinition(null, Types.FLOAT, -1, false, null, null);
    } else if (value instanceof BigDecimal) {
      return new JdbcBigDecimalFieldDefinition(null, Types.NUMERIC, -1, -1, false,
        null, null);
    } else if (value instanceof Date) {
      return new JdbcDateFieldDefinition(null, -1, false, null, null);
    } else if (value instanceof java.util.Date) {
      return new JdbcTimestampFieldDefinition(null, -1, false, null, null);
    } else if (value instanceof Boolean) {
      return new JdbcBooleanFieldDefinition(null, Types.BIT, -1, false, null, null);
    } else {
      return new JdbcFieldDefinition();
    }
  }

  private int sqlType;

  private JdbcFieldDefinition() {
  }

  public JdbcFieldDefinition(final String name, final DataType type,
    final int sqlType, final int length, final int scale,
    final boolean required, final String description,
    final Map<String, Object> properties) {
    super(name, type, length, scale, required, description, properties);
    this.sqlType = sqlType;
  }

  public void addColumnName(final StringBuffer sql, final String tablePrefix) {
    if (tablePrefix != null) {
      sql.append(tablePrefix);
      sql.append(".");
    }
    sql.append(getName());
  }

  public void addInsertStatementPlaceHolder(final StringBuffer sql,
    final boolean generateKeys) {
    addStatementPlaceHolder(sql);
  }

  public void addSelectStatementPlaceHolder(final StringBuffer sql) {
    addStatementPlaceHolder(sql);
  }

  public void addStatementPlaceHolder(final StringBuffer sql) {
    sql.append('?');
  }

  @Override
  public JdbcFieldDefinition clone() {
    return new JdbcFieldDefinition(getName(), getType(), getSqlType(), getLength(),
      getScale(), isRequired(), getDescription(), getProperties());
  }

  public int getSqlType() {
    return this.sqlType;
  }

  public int setAttributeValueFromResultSet(final ResultSet resultSet,
    final int columnIndex, final Record object) throws SQLException {
    final Object value = resultSet.getObject(columnIndex);
    object.setValue(getIndex(), value);
    return columnIndex + 1;
  }

  public int setInsertPreparedStatementValue(final PreparedStatement statement,
    final int parameterIndex, final Record object) throws SQLException {
    final String name = getName();
    final Object value = object.getValue(name);
    return setPreparedStatementValue(statement, parameterIndex, value);
  }

  public int setPreparedStatementValue(final PreparedStatement statement,
    final int parameterIndex, final Object value) throws SQLException {
    statement.setObject(parameterIndex, value);
    return parameterIndex + 1;
  }

  public void setSqlType(final int sqlType) {
    this.sqlType = sqlType;
  }
}
