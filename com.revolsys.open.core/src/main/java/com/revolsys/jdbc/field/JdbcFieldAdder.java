package com.revolsys.jdbc.field;

import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.record.schema.RecordStoreSchema;
import com.revolsys.data.types.DataType;
import com.revolsys.jdbc.io.AbstractJdbcRecordStore;

public class JdbcFieldAdder {
  public static String GEOMETRY_FACTORY = "geometryFactory";

  public static String NUM_AXIS = "numAxis";

  public static String GEOMETRY_TYPE = "geometryType";

  public static final String COLUMN_PROPERTIES = "columnProperties";

  public static final String TABLE_PROPERTIES = "tableProperties";

  public static Map<String, Map<String, Map<String, Object>>> getColumnProperties(
    final RecordStoreSchema schema) {
    synchronized (schema) {
      Map<String, Map<String, Map<String, Object>>> columnProperties = schema.getProperty(COLUMN_PROPERTIES);
      if (columnProperties == null) {
        columnProperties = new HashMap<String, Map<String, Map<String, Object>>>();
        schema.setProperty(COLUMN_PROPERTIES, columnProperties);
      }
      return columnProperties;
    }
  }

  @SuppressWarnings("unchecked")
  public static <V> V getColumnProperty(final RecordStoreSchema schema, final String typePath,
    final String columnName, final String propertyName) {
    final Map<String, Map<String, Object>> columnsProperties = getTypeColumnProperties(schema,
      typePath);
    final Map<String, Object> properties = columnsProperties.get(columnName);
    if (properties != null) {
      final Object value = properties.get(propertyName);
      return (V)value;
    }
    return null;
  }

  public static double getDoubleColumnProperty(final RecordStoreSchema schema,
    final String typePath, final String columnName, final String propertyName) {
    final Object value = getColumnProperty(schema, typePath, columnName, propertyName);
    if (value instanceof Number) {
      final Number number = (Number)value;
      return number.doubleValue();
    } else {
      return 11;
    }
  }

  public static int getIntegerColumnProperty(final RecordStoreSchema schema, final String typePath,
    final String columnName, final String propertyName) {
    final Object value = getColumnProperty(schema, typePath, columnName, propertyName);
    if (value instanceof Number) {
      final Number number = (Number)value;
      return number.intValue();
    } else {
      return -1;
    }
  }

  public static Map<String, Map<String, Object>> getTableProperties(final RecordStoreSchema schema) {
    synchronized (schema) {
      Map<String, Map<String, Object>> tableProperties = schema.getProperty(TABLE_PROPERTIES);
      if (tableProperties == null) {
        tableProperties = new HashMap<String, Map<String, Object>>();
        schema.setProperty(TABLE_PROPERTIES, tableProperties);
      }
      return tableProperties;
    }
  }

  public static Map<String, Object> getTableProperties(final RecordStoreSchema schema,
    final String typePath) {
    final Map<String, Map<String, Object>> tableProperties = getTableProperties(schema);
    synchronized (tableProperties) {
      Map<String, Object> properties = tableProperties.get(typePath);
      if (properties == null) {
        properties = new HashMap<String, Object>();
        tableProperties.put(typePath, properties);
      }
      return properties;
    }
  }

  @SuppressWarnings("unchecked")
  public static <V> V getTableProperty(final RecordStoreSchema schema, final String typePath,
    final String propertyName) {
    final Map<String, Object> properties = getTableProperties(schema, typePath);
    final Object value = properties.get(propertyName);
    return (V)value;
  }

  public static Map<String, Map<String, Object>> getTypeColumnProperties(
    final RecordStoreSchema schema, final String typePath) {
    final Map<String, Map<String, Map<String, Object>>> esriColumnProperties = getColumnProperties(schema);
    final Map<String, Map<String, Object>> typeColumnProperties = esriColumnProperties.get(typePath);
    if (typeColumnProperties == null) {
      return Collections.emptyMap();
    } else {
      return typeColumnProperties;
    }
  }

  public static void setColumnProperty(final RecordStoreSchema schema, final String typePath,
    final String columnName, final String propertyName, final Object propertyValue) {
    final Map<String, Map<String, Map<String, Object>>> tableColumnProperties = getColumnProperties(schema);
    synchronized (tableColumnProperties) {

      Map<String, Map<String, Object>> typeColumnMap = tableColumnProperties.get(typePath);
      if (typeColumnMap == null) {
        typeColumnMap = new HashMap<String, Map<String, Object>>();
        tableColumnProperties.put(typePath, typeColumnMap);
      }
      Map<String, Object> columnProperties = typeColumnMap.get(columnName);
      if (columnProperties == null) {
        columnProperties = new HashMap<String, Object>();
        typeColumnMap.put(columnName, columnProperties);
      }
      columnProperties.put(propertyName, propertyValue);
    }
  }

  public static void setTableProperty(final RecordStoreSchema schema, final String typePath,
    final String propertyName, final Object value) {
    final Map<String, Object> properties = getTableProperties(schema, typePath);
    properties.put(propertyName, value);
  }

  private DataType dataType;

  public JdbcFieldAdder() {
  }

  public JdbcFieldAdder(final DataType dataType) {
    this.dataType = dataType;
  }

  public FieldDefinition addField(final AbstractJdbcRecordStore recordStore,
    final RecordDefinitionImpl recordDefinition, final String name, final String dataType,
    final int sqlType, final int length, final int scale, final boolean required,
    final String description) {
    JdbcFieldDefinition attribute;
    if (dataType.equals("oid")) {
      attribute = new JdbcBlobFieldDefinition(name, sqlType, length, required, description, null);
    } else {
      switch (sqlType) {
        case Types.CHAR:
        case Types.CLOB:
        case Types.LONGVARCHAR:
        case Types.VARCHAR:
          final Map<String, Object> properties = recordStore.getProperties();
          attribute = new JdbcStringFieldDefinition(name, sqlType, length, required, description,
            properties);
        break;
        case Types.BIGINT:
          attribute = new JdbcLongFieldDefinition(name, sqlType, length, required, description,
            null);
        break;
        case Types.INTEGER:
          attribute = new JdbcIntegerFieldDefinition(name, sqlType, length, required, description,
            null);
        break;
        case Types.SMALLINT:
          attribute = new JdbcShortFieldDefinition(name, sqlType, length, required, description,
            null);
        break;
        case Types.TINYINT:
          attribute = new JdbcByteFieldDefinition(name, sqlType, length, required, description,
            null);
        break;
        case Types.DOUBLE:
          attribute = new JdbcDoubleFieldDefinition(name, sqlType, length, required, description,
            null);
        break;
        case Types.REAL:
          attribute = new JdbcFloatFieldDefinition(name, sqlType, length, required, description,
            null);
        break;
        case Types.DECIMAL:
        case Types.NUMERIC:
        case Types.FLOAT:
          if (scale > 0) {
            attribute = new JdbcBigDecimalFieldDefinition(name, sqlType, length, scale, required,
              description, null);
          } else if (length == 131089 || length == 0) {
            attribute = new JdbcBigDecimalFieldDefinition(name, sqlType, -1, -1, required,
              description, null);
          } else {
            if (length <= 2) {
              attribute = new JdbcByteFieldDefinition(name, sqlType, length, required, description,
                null);
            } else if (length <= 4) {
              attribute = new JdbcShortFieldDefinition(name, sqlType, length, required,
                description, null);
            } else if (length <= 9) {
              attribute = new JdbcIntegerFieldDefinition(name, sqlType, length, required,
                description, null);
            } else if (length <= 18) {
              attribute = new JdbcLongFieldDefinition(name, sqlType, length, required, description,
                null);
            } else {
              attribute = new JdbcBigIntegerFieldDefinition(name, sqlType, length, required,
                description, null);
            }
          }
        break;
        case Types.DATE:
          attribute = new JdbcDateFieldDefinition(name, sqlType, required, description, null);
        break;
        case Types.TIMESTAMP:
          attribute = new JdbcTimestampFieldDefinition(name, sqlType, required, description, null);
        break;
        case Types.BIT:
          attribute = new JdbcBooleanFieldDefinition(name, sqlType, length, required, description,
            null);
        break;
        case Types.BLOB:
          attribute = new JdbcBlobFieldDefinition(name, sqlType, length, required, description,
            null);
        break;
        default:
          attribute = new JdbcFieldDefinition(name, this.dataType, sqlType, length, scale,
            required, description, null);
        break;
      }
    }
    recordDefinition.addField(attribute);
    return attribute;
  }

  public void initialize(final RecordStoreSchema schema) {
  }
}
