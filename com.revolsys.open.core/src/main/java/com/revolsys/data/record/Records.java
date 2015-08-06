package com.revolsys.data.record;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.revolsys.converter.string.StringConverter;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.data.codes.CodeTable;
import com.revolsys.data.equals.Equals;
import com.revolsys.data.equals.EqualsInstance;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.types.DataType;
import com.revolsys.data.types.DataTypes;
import com.revolsys.gis.jts.JtsGeometryUtil;
import com.revolsys.util.JavaBeanUtil;
import com.revolsys.util.Property;
import com.vividsolutions.jts.geom.Geometry;

public final class Records {

  public static Record copy(final RecordDefinition recordDefinition, final Record record) {
    final Record copy = new ArrayRecord(recordDefinition);
    copy.setValues(record);
    return copy;
  }

  /**
   * Create a copy of the data record replacing the geometry with the new
   * geometry. If the existing geometry on the record has user data it will be
   * cloned to the new geometry.
   *
   * @param record The record to copy.
   * @param geometry The new geometry.
   * @return The copied record.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Record> T copy(final T record, final Geometry geometry) {
    final Geometry oldGeometry = record.getGeometry();
    final T newObject = (T)record.clone();
    newObject.setGeometryValue(geometry);
    JtsGeometryUtil.copyUserData(oldGeometry, geometry);
    return newObject;
  }

  public static RecordDefinition createGeometryMetaData() {
    final FieldDefinition geometryAttribute = new FieldDefinition("geometry", DataTypes.GEOMETRY,
      true);
    return new RecordDefinitionImpl("Feature", geometryAttribute);
  }

  public static <D extends Record> List<D> filter(final Collection<D> records,
    final Geometry geometry, final double maxDistance) {
    final List<D> results = new ArrayList<D>();
    for (final D record : records) {
      final Geometry recordGeometry = record.getGeometry();
      final double distance = recordGeometry.distance(geometry);
      if (distance < maxDistance) {
        results.add(record);
      }
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public static <T> T getAttributeByPath(final Record record, final String path) {
    final RecordDefinition recordDefinition = record.getRecordDefinition();

    final String[] propertyPath = path.split("\\.");
    Object propertyValue = record;
    for (int i = 0; i < propertyPath.length && propertyValue != null; i++) {
      final String propertyName = propertyPath[i];
      if (propertyValue instanceof Record) {
        final Record recordValue = (Record)propertyValue;

        if (recordValue.hasField(propertyName)) {
          propertyValue = recordValue.getValue(propertyName);
          if (propertyValue == null) {
            return null;
          } else if (i + 1 < propertyPath.length) {
            final CodeTable codeTable = recordDefinition.getCodeTableByFieldName(propertyName);
            if (codeTable != null) {
              propertyValue = codeTable.getMap(propertyValue);
            }
          }
        } else {
          return null;
        }
      } else if (propertyValue instanceof Geometry) {
        final Geometry geometry = (Geometry)propertyValue;
        propertyValue = JtsGeometryUtil.getGeometryProperty(geometry, propertyName);
      } else if (propertyValue instanceof Map) {
        final Map<String, Object> map = (Map<String, Object>)propertyValue;
        propertyValue = map.get(propertyName);
        if (propertyValue == null) {
          return null;
        } else if (i + 1 < propertyPath.length) {
          final CodeTable codeTable = recordDefinition.getCodeTableByFieldName(propertyName);
          if (codeTable != null) {
            propertyValue = codeTable.getMap(propertyValue);
          }
        }
      } else {
        try {
          propertyValue = JavaBeanUtil.getProperty(propertyValue, propertyName);
        } catch (final IllegalArgumentException e) {
          throw new IllegalArgumentException("Path does not exist " + path, e);
        }
      }
    }
    return (T)propertyValue;
  }

  public static boolean getBoolean(final Record record, final String attributeName) {
    if (record == null) {
      return false;
    } else {
      final Object value = record.getValue(attributeName);
      if (value == null) {
        return false;
      } else if (value instanceof Boolean) {
        final Boolean booleanValue = (Boolean)value;
        return booleanValue;
      } else if (value instanceof Number) {
        final Number number = (Number)value;
        return number.intValue() == 1;
      } else {
        final String stringValue = value.toString();
        if (stringValue.equals("1") || Boolean.parseBoolean(stringValue)) {
          return true;
        } else {
          return false;
        }
      }
    }
  }

  public static Double getDouble(final Record record, final int attributeIndex) {
    final Number value = record.getValue(attributeIndex);
    if (value == null) {
      return null;
    } else if (value instanceof Double) {
      return (Double)value;
    } else {
      return value.doubleValue();
    }
  }

  public static Double getDouble(final Record record, final String attributeName) {
    final Number value = record.getValue(attributeName);
    if (value == null) {
      return null;
    } else if (value instanceof Double) {
      return (Double)value;
    } else {
      return value.doubleValue();
    }
  }

  public static Integer getInteger(final Record record, final String attributeName) {
    if (record == null) {
      return null;
    } else {
      final Number value = record.getValue(attributeName);
      if (value == null) {
        return null;
      } else if (value instanceof Integer) {
        return (Integer)value;
      } else {
        return value.intValue();
      }
    }
  }

  public static Integer getInteger(final Record record, final String attributeName,
    final Integer defaultValue) {
    if (record == null) {
      return null;
    } else {
      final Number value = record.getValue(attributeName);
      if (value == null) {
        return defaultValue;
      } else if (value instanceof Integer) {
        return (Integer)value;
      } else {
        return value.intValue();
      }
    }
  }

  public static Long getLong(final Record record, final String attributeName) {
    final Number value = record.getValue(attributeName);
    if (value == null) {
      return null;
    } else if (value instanceof Long) {
      return (Long)value;
    } else {
      return value.longValue();
    }
  }

  public static Record getObject(final RecordDefinition recordDefinition,
    final Map<String, Object> values) {
    final Record record = new ArrayRecord(recordDefinition);
    for (final Entry<String, Object> entry : values.entrySet()) {
      final String name = entry.getKey();
      final FieldDefinition attribute = recordDefinition.getField(name);
      if (attribute != null) {
        final Object value = entry.getValue();
        if (value != null) {
          final DataType dataType = attribute.getType();
          @SuppressWarnings("unchecked")
          final Class<Object> dataTypeClass = (Class<Object>)dataType.getJavaClass();
          if (dataTypeClass.isAssignableFrom(value.getClass())) {
            record.setValue(name, value);
          } else {
            final StringConverter<Object> converter = StringConverterRegistry.getInstance()
              .getConverter(dataTypeClass);
            if (converter == null) {
              record.setValue(name, value);
            } else {
              final Object convertedValue = converter.toObject(value);
              record.setValue(name, convertedValue);
            }
          }
        }
      }
    }
    return record;
  }

  public static List<Record> getObjects(final RecordDefinition recordDefinition,
    final Collection<? extends Map<String, Object>> list) {
    final List<Record> records = new ArrayList<Record>();
    for (final Map<String, Object> map : list) {
      final Record record = getObject(recordDefinition, map);
      records.add(record);
    }
    return records;
  }

  public static void mergeValue(final Map<String, Object> record, final Record record1,
    final Record record2, final String fieldName, final String separator) {
    final String value1 = record1.getString(fieldName);
    final String value2 = record2.getString(fieldName);
    Object value;
    if (!Property.hasValue(value1)) {
      value = value2;
    } else if (!Property.hasValue(value2)) {
      value = value1;
    } else if (Equals.equal(value1, value2)) {
      value = value1;
    } else {
      value = value1 + separator + value2;
    }
    record.put(fieldName, value);
  }

  public static void setValues(final Record target, final Record source,
    final Collection<String> attributesNames, final Collection<String> ignoreAttributeNames) {
    for (final String attributeName : attributesNames) {
      if (!ignoreAttributeNames.contains(attributeName)) {
        final Object oldValue = target.getValue(attributeName);
        Object newValue = source.getValue(attributeName);
        if (!EqualsInstance.INSTANCE.equals(oldValue, newValue)) {
          newValue = JavaBeanUtil.clone(newValue);
          target.setValue(attributeName, newValue);
        }
      }
    }
  }

  private Records() {
  }

}
