package com.revolsys.data.equals;

import java.util.Collection;
import java.util.Map;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;

public class RecordEquals implements Equals<Record> {
  public static final String EXCLUDE_GEOMETRY = RecordEquals.class.getName() + ".excludeGeometry";

  public static final String EXCLUDE_ID = RecordEquals.class.getName() + ".excludeId";

  public static boolean equalAttributes(final Collection<String> excludedAttributes,
    final Record object1, final Record object2, final Collection<String> attributeNames) {
    for (final String attributeName : attributeNames) {
      if (!equals(excludedAttributes, object1, object2, attributeName)) {
        return false;
      }
    }
    return true;
  }

  public static boolean equalAttributes(final Record object1, final Map<String, Object> values2) {
    if (object1 == null) {
      return values2 == null;
    } else if (values2 == null) {
      return false;
    } else {
      for (final String attributeName : object1.getRecordDefinition().getFieldNames()) {
        if (!MapEquals.equals(object1, values2, attributeName)) {
          return false;
        }
      }
      return true;
    }
  }

  public static boolean equalAttributes(final Record object1, final Record object2,
    final Collection<String> attributeNames) {
    for (final String attributeName : attributeNames) {
      if (!equals(object1, object2, attributeName)) {
        return false;
      }
    }
    return true;
  }

  public static boolean equals(final Collection<String> excludedAttributes, final Record object1,
    final Record object2, final String attributeName) {
    final RecordDefinition metaData = object1.getRecordDefinition();
    if (excludedAttributes.contains(attributeName)) {
      return true;
    } else if (excludedAttributes.contains(EXCLUDE_ID)
      && attributeName.equals(metaData.getIdFieldName())) {
      return true;
    } else if (excludedAttributes.contains(EXCLUDE_GEOMETRY)
      && attributeName.equals(metaData.getGeometryFieldName())) {
      return true;
    } else {
      final Object value1 = object1.getValue(attributeName);
      final Object value2 = object2.getValue(attributeName);
      return EqualsInstance.INSTANCE.equals(value1, value2);
    }
  }

  public static boolean equals(final Record object1, final Record object2,
    final String attributeName) {
    final Object value1 = object1.getValue(attributeName);
    final Object value2 = object2.getValue(attributeName);
    return EqualsInstance.INSTANCE.equals(value1, value2);
  }

  public static boolean isAttributeIgnored(final RecordDefinition metaData,
    final Collection<String> excludedAttributes, final String attributeName) {
    if (excludedAttributes.contains(attributeName)) {
      return true;
    } else if (excludedAttributes.contains(EXCLUDE_ID)
      && attributeName.equals(metaData.getIdFieldName())) {
      return true;
    } else if (excludedAttributes.contains(EXCLUDE_GEOMETRY)
      && attributeName.equals(metaData.getGeometryFieldName())) {
      return true;
    } else {
      return false;
    }
  }

  public static boolean isFieldIgnored(final RecordDefinition recordDefinition,
    final Collection<String> excludedAttributes, final String fieldName) {
    if (excludedAttributes.contains(fieldName)) {
      return true;
    } else if (excludedAttributes.contains(EXCLUDE_ID)
      && fieldName.equals(recordDefinition.getIdFieldName())) {
      return true;
    } else if (excludedAttributes.contains(EXCLUDE_GEOMETRY)
      && fieldName.equals(recordDefinition.getGeometryFieldName())) {
      return true;
    } else {
      return false;
    }
  }

  private EqualsRegistry equalsRegistry;

  @Override
  public boolean equals(final Record object1, final Record object2,
    final Collection<String> excludedAttributes) {
    if (object1 != null && object2 != null) {
      final RecordDefinition metaData1 = object1.getRecordDefinition();
      final RecordDefinition metaData2 = object2.getRecordDefinition();
      if (metaData1.getPath().equals(metaData2.getPath())) {
        if (metaData1.getFieldCount() == metaData2.getFieldCount()) {
          final int idIndex = metaData1.getIdFieldIndex();
          final int geometryIndex = metaData1.getGeometryFieldIndex();
          final int objectIdIndex = metaData1.getFieldIndex("OBJECTID");
          for (int i = 0; i < metaData1.getFieldCount(); i++) {
            final String name = metaData1.getFieldName(i);
            if (excludedAttributes.contains(name)) {
            } else if (i == idIndex && excludedAttributes.contains(EXCLUDE_ID)) {
            } else if (i == geometryIndex && excludedAttributes.contains(EXCLUDE_GEOMETRY)) {
            } else if (i == objectIdIndex && excludedAttributes.contains(EXCLUDE_GEOMETRY)) {
            } else {
              final Object value1 = object1.getValue(i);
              final Object value2 = object2.getValue(i);
              if (!this.equalsRegistry.equals(value1, value2, excludedAttributes)) {
                return false;
              }
            }
          }
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void setEqualsRegistry(final EqualsRegistry equalsRegistry) {
    this.equalsRegistry = equalsRegistry;
  }
}
