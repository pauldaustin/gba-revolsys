package com.revolsys.gis.model.schema;

import java.util.LinkedHashMap;
import java.util.Map;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.gis.converter.ObjectConverter;
import com.revolsys.gis.data.model.RecordDefinition;

public class SchemaMapper {
  private final Map<FieldDefinition, FieldDefinition> attributeMapping = new LinkedHashMap<FieldDefinition, FieldDefinition>();

  private final Map<RecordDefinition, ObjectConverter> typeConverter = new LinkedHashMap<RecordDefinition, ObjectConverter>();

  private final Map<RecordDefinition, RecordDefinition> typeMapping = new LinkedHashMap<RecordDefinition, RecordDefinition>();

  public SchemaMapper() {
  }

  /**
   * Add a forward and reverse mapping from one DataObjectMetaData to another.
   * 
   * @param from The type.
   * @param to The mapped type.
   */
  public void addAttributeMapping(final FieldDefinition from, final FieldDefinition to) {
    attributeMapping.put(from, to);
    attributeMapping.put(to, from);
  }

  /**
   * Add a forward and reverse mapping from one Attribute to another.
   * 
   * @param from The attribute.
   * @param to The mapped type.
   */
  public void addAttributeMapping(final RecordDefinition fromClass,
    final String fromName, final RecordDefinition toClass, final String toName) {
    final FieldDefinition fromAttribute = fromClass.getAttribute(fromName);
    final FieldDefinition toAttribute = toClass.getAttribute(toName);
    addAttributeMapping(fromAttribute, toAttribute);
  }

  /**
   * Add an object converter for the specified type.
   * 
   * @param from The type.
   * @param converter The converter.
   */
  public void addTypeConverter(final RecordDefinition type,
    final ObjectConverter converter) {
    typeConverter.put(type, converter);
  }

  /**
   * Add a forward and reverse mapping from one DataObjectMetaData to another.
   * 
   * @param from The type.
   * @param to The mapped type.
   */
  public void addTypeMapping(final RecordDefinition from,
    final RecordDefinition to) {
    typeMapping.put(from, to);
    typeMapping.put(to, from);
  }

  public Record convert(final Record object) {
    final RecordDefinition type = object.getMetaData();
    final RecordDefinition newType = getClassMapping(type);
    if (newType != null) {
      final Record newObject = newType.createDataObject();
      for (int i = 0; i < type.getAttributeCount(); i++) {
        final FieldDefinition attribute = type.getAttribute(i);
        final FieldDefinition newAttribute = getAttributeMapping(attribute);
        if (newAttribute != null) {
          final Object value = object.getValue(i);
          newObject.setValue(newAttribute.getName(), value);
        }
      }
      return newObject;
    } else {
      return object;
    }

  }

  /**
   * Get the Attribute that the specified attribute maps to.
   * 
   * @param attribute The attribute to map.
   * @return The mapped attribute.
   */
  public FieldDefinition getAttributeMapping(final FieldDefinition attribute) {
    return attributeMapping.get(attribute);
  }

  /**
   * Get the DataObjectMetaData that the specified class maps to.
   * 
   * @param type The class to map.
   * @return The mapped class.
   */
  public RecordDefinition getClassMapping(final RecordDefinition type) {
    return typeMapping.get(type);
  }

  /**
   * Get the converter that can convert objects of the specified type.
   * 
   * @param type The type to convert.
   * @return The converter
   */
  public ObjectConverter getTypeConverter(final RecordDefinition type) {
    return typeConverter.get(type);
  }

  /**
   * Get the DataObjectMetaData that the specified type maps to.
   * 
   * @param type The type to map.
   * @return The mapped type.
   */
  public RecordDefinition getTypeMapping(final RecordDefinition type) {
    return typeMapping.get(type);
  }

  /**
   * Check there is a converter for the type.
   * 
   * @param type The type to convert.
   * @return The converter
   */
  public boolean hasTypeConverter(final RecordDefinition type) {
    return typeConverter.containsKey(type);
  }
}
