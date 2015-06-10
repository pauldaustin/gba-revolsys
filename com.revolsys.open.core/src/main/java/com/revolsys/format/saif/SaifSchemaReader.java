/*
 * $URL:https://secure.revolsys.com/svn/open.revolsys.com/GIS/trunk/src/main/java/com/revolsys/gis/format/saif/io/SaifSchemaReader.java $
 * $Author:paul.austin@revolsys.com $
 * $Date:2007-06-09 09:28:28 -0700 (Sat, 09 Jun 2007) $
 * $Revision:265 $

 * Copyright 2004-2005 Revolution Systems Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.revolsys.format.saif;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.core.io.Resource;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.property.FieldProperties;
import com.revolsys.data.record.property.RecordDefinitionProperty;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionFactory;
import com.revolsys.data.record.schema.RecordDefinitionFactoryImpl;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.types.CollectionDataType;
import com.revolsys.data.types.DataType;
import com.revolsys.data.types.DataTypes;
import com.revolsys.data.types.EnumerationDataType;
import com.revolsys.data.types.SimpleDataType;
import com.revolsys.format.saif.util.CsnIterator;

public class SaifSchemaReader {

  private static final Map<String, DataType> nameTypeMap = new HashMap<String, DataType>();

  private static final String SPATIAL_OBJECT = "/SpatialObject";

  private static final String TEXT_OR_SYMBOL_OBJECT = "/TextOrSymbolObject";

  static {
    addType("/Boolean", DataTypes.BOOLEAN);
    addType("/Numeric", DataTypes.DECIMAL);
    addType("/Integer", DataTypes.INTEGER);
    addType("/Integer8", DataTypes.BYTE);
    addType("/Integer16", DataTypes.SHORT);
    addType("/Integer32", DataTypes.INT);
    addType("/Integer64", DataTypes.LONG);
    addType("/Integer8Unsigned", DataTypes.INTEGER);
    addType("/Integer16Unsigned", DataTypes.INTEGER);
    addType("/Integer32Unsigned", DataTypes.INTEGER);
    addType("/Integer64Unsigned", DataTypes.INTEGER);
    addType("/Real", DataTypes.DECIMAL);
    addType("/Real32", DataTypes.FLOAT);
    addType("/Real64", DataTypes.DOUBLE);
    addType("/Real80", DataTypes.DECIMAL);
    addType("/List", DataTypes.LIST);
    addType("/Set", DataTypes.SET);
    addType("/AggregateType", new SimpleDataType("AggregateType", Object.class));
    addType("/PrimitiveType", new SimpleDataType("PrimitiveType", Object.class));
    addType("/Enumeration", new SimpleDataType("Enumeration", Object.class));

  }

  private static void addType(final String typePath, final DataType dataType) {
    nameTypeMap.put(String.valueOf(typePath), dataType);
  }

  private List<RecordDefinitionProperty> commonMetaDataProperties = new ArrayList<RecordDefinitionProperty>();

  private RecordDefinitionImpl currentClass;

  private final Set<RecordDefinition> currentSuperClasses = new LinkedHashSet<RecordDefinition>();

  private RecordDefinitionFactoryImpl schema;

  private void addExportedObjects() {
    final RecordDefinitionImpl exportedObjectHandle = new RecordDefinitionImpl(
        "ExportedObjectHandle");
    this.schema.addMetaData(exportedObjectHandle);
    exportedObjectHandle.addField("referenceID", DataTypes.STRING, true);
    exportedObjectHandle.addField("type", DataTypes.STRING, true);
    exportedObjectHandle.addField("offset", DataTypes.INTEGER, true);
    exportedObjectHandle.addField("sharable", DataTypes.BOOLEAN, true);
  }

  public void addSuperClass(final RecordDefinitionImpl currentClass,
    final RecordDefinition superClass) {
    currentClass.addSuperClass(superClass);
    for (final String name : superClass.getFieldNames()) {
      final FieldDefinition attribute = superClass.getField(name);
      currentClass.addField(attribute.clone());
    }
    for (final Entry<String, Object> defaultValue : superClass.getDefaultValues().entrySet()) {
      final String name = defaultValue.getKey();
      final Object value = defaultValue.getValue();
      if (!currentClass.hasField(name)) {
        currentClass.addDefaultValue(name, value);
      }
    }
    final String idAttributeName = superClass.getIdFieldName();
    if (idAttributeName != null) {
      currentClass.setIdFieldName(idAttributeName);

    }
    String geometryAttributeName = superClass.getGeometryFieldName();
    final String path = currentClass.getPath();
    if (path.equals("/TRIM/TrimText")) {
      geometryAttributeName = "textOrSymbol";
    }
    if (geometryAttributeName != null) {
      currentClass.setGeometryFieldName(geometryAttributeName);
    }
  }

  public void attributes(final RecordDefinition type, final CsnIterator iterator)
      throws IOException {
    while (iterator.getNextEventType() == CsnIterator.ATTRIBUTE_NAME
        || iterator.getNextEventType() == CsnIterator.OPTIONAL_ATTRIBUTE) {
      boolean required = true;
      switch (iterator.next()) {
        case CsnIterator.OPTIONAL_ATTRIBUTE:
          required = false;
          iterator.next();
        case CsnIterator.ATTRIBUTE_NAME:
          final String attributeName = iterator.getStringValue();
          switch (iterator.next()) {
            case CsnIterator.ATTRIBUTE_TYPE:
              final String typePath = iterator.getPathValue();
              DataType dataType = nameTypeMap.get(typePath);
              if (typePath.equals(SPATIAL_OBJECT) || typePath.equals(TEXT_OR_SYMBOL_OBJECT)) {
                dataType = DataTypes.GEOMETRY;
                this.currentClass.setGeometryFieldIndex(this.currentClass.getFieldCount());
              } else if (dataType == null) {
                dataType = new SimpleDataType(typePath, Record.class);
              }

              this.currentClass.addField(attributeName, dataType, required);

              break;
            case CsnIterator.COLLECTION_ATTRIBUTE:
              final String collectionType = iterator.getPathValue();
              if (iterator.next() == CsnIterator.CLASS_NAME) {
                final String contentTypeName = iterator.getPathValue();
                final DataType collectionDataType = nameTypeMap.get(collectionType);
                DataType contentDataType = nameTypeMap.get(contentTypeName);
                if (contentDataType == null) {
                  contentDataType = DataTypes.DATA_OBJECT;
                }
                this.currentClass.addField(
                  attributeName,
                  new CollectionDataType(collectionDataType.getName(),
                    collectionDataType.getJavaClass(), contentDataType), required);
              } else {
                throw new IllegalStateException("Expecting attribute type");
              }
              break;
            case CsnIterator.STRING_ATTRIBUTE:
              int length = Integer.MAX_VALUE;
              if (iterator.getEventType() == CsnIterator.STRING_ATTRIBUTE_LENGTH) {
                length = iterator.getIntegerValue();
              }
              this.currentClass.addField(attributeName, DataTypes.STRING, length, required);
              break;
            default:
              throw new IllegalStateException("Unknown event type: " + iterator.getEventType());
          }
          break;
        default:
          break;
      }
    }
  }

  public void classAttributes(final RecordDefinition type, final CsnIterator iterator)
      throws IOException {
  }

  public void comments(final RecordDefinition type, final CsnIterator iterator) throws IOException {
    if (iterator.next() == CsnIterator.VALUE) {
      iterator.getStringValue();
    }
  }

  public void defaults(final RecordDefinition type, final CsnIterator iterator) throws IOException {
    while (iterator.getNextEventType() == CsnIterator.ATTRIBUTE_PATH) {
      iterator.next();
      final String attributeName = iterator.getStringValue();
      if (iterator.next() == CsnIterator.VALUE) {
        final Object value = iterator.getValue();
        this.currentClass.addDefaultValue(attributeName, value);
      } else {
        throw new IllegalStateException("Expecting a value");
      }
    }
  }

  public List<RecordDefinitionProperty> getCommonMetaDataProperties() {
    return this.commonMetaDataProperties;
  }

  private Object getDefinition(final CsnIterator iterator) throws IOException {
    while (iterator.next() != CsnIterator.END_DEFINITION) {
      switch (iterator.getEventType()) {
        case CsnIterator.CLASS_NAME:
          final String superClassName = iterator.getPathValue();
          if (superClassName.equals("/Enumeration")) {
            final DataType enumeration = processEnumeration(iterator);
            nameTypeMap.put(enumeration.getName(), enumeration);
            return enumeration;
          }
          final RecordDefinition superClass = this.schema.getRecordDefinition(superClassName);
          if (superClass == null) {
            throw new IllegalStateException("Cannot find super class '" + superClassName + "'");

          }
          this.currentSuperClasses.add(superClass);
          break;
        case CsnIterator.COMPONENT_NAME:
          final String componentName = iterator.getStringValue();
          try {
            final Method method = getClass().getMethod(componentName, new Class[] {
              RecordDefinition.class, CsnIterator.class
            });
            method.invoke(this, new Object[] {
              this.currentClass, iterator
            });
          } catch (final SecurityException e) {
            throw new IllegalStateException("Unknown component '" + componentName + "'");
          } catch (final NoSuchMethodException e) {
            throw new IllegalStateException("Unknown component '" + componentName + "'");
          } catch (final IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
          } catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
              throw (RuntimeException)cause;
            } else if (cause instanceof Error) {
              throw (Error)cause;
            } else if (cause instanceof IOException) {
              throw (IOException)cause;
            } else {
              throw new RuntimeException(cause.getMessage(), cause);
            }
          }
        default:
          break;
      }
    }
    return this.currentClass;
  }

  private RecordDefinitionFactory loadSchema(final CsnIterator iterator) throws IOException {
    if (this.schema == null) {
      this.schema = new RecordDefinitionFactoryImpl();

      this.schema.addMetaData(new RecordDefinitionImpl("/AggregateType"));
      this.schema.addMetaData(new RecordDefinitionImpl("/PrimitiveType"));

      addExportedObjects();
    }
    while (iterator.next() != CsnIterator.END_DOCUMENT) {
      this.currentSuperClasses.clear();
      this.currentClass = null;
      final Object definition = getDefinition(iterator);
      if (definition instanceof RecordDefinition) {
        final RecordDefinitionImpl metaData = (RecordDefinitionImpl)definition;
        setMetaDataProperties(metaData);
        metaData.setRecordDefinitionFactory(this.schema);
        this.schema.addMetaData(metaData);
      }
    }
    return this.schema;
  }

  public RecordDefinitionFactory loadSchema(final File file) throws IOException {
    final CsnIterator iterator = new CsnIterator(file);
    return loadSchema(iterator);
  }

  public RecordDefinitionFactory loadSchema(final Resource resource) throws IOException {
    return loadSchema(new CsnIterator(resource.getFilename(), resource.getInputStream()));

  }

  public RecordDefinitionFactory loadSchema(final String fileName, final InputStream in)
      throws IOException {
    return loadSchema(new CsnIterator(fileName, in));
  }

  public RecordDefinitionFactory loadSchemas(final List<Resource> resources) throws IOException {
    for (final Resource resource : resources) {
      if (resource.exists()) {
        loadSchema(resource);
      }
    }
    return this.schema;
  }

  private DataType processEnumeration(final CsnIterator iterator) throws IOException {
    String name = null;
    final Set<String> allowedValues = new TreeSet<String>();
    while (iterator.getNextEventType() == CsnIterator.COMPONENT_NAME) {
      iterator.next();
      final String componentName = iterator.getStringValue();
      if (componentName.equals("subclass")) {
        if (iterator.next() == CsnIterator.CLASS_NAME) {
          name = iterator.getPathValue();
        } else {
          throw new IllegalArgumentException("Expecting an enumeration class name");
        }
      } else if (componentName.equals("values")) {
        while (iterator.getNextEventType() == CsnIterator.TAG_NAME) {
          iterator.next();
          final String tagName = iterator.getStringValue();
          allowedValues.add(tagName);
        }
      } else if (!componentName.equals("comments")) {
        throw new IllegalArgumentException("Unknown component " + componentName
          + " for enumberation " + name);
      }

    }
    return new EnumerationDataType(name, String.class, allowedValues);
  }

  public void restricted(final RecordDefinition type, final CsnIterator iterator)
      throws IOException {
    while (iterator.getNextEventType() == CsnIterator.ATTRIBUTE_PATH) {
      iterator.next();
      String attributeName = iterator.getStringValue();
      boolean hasMore = true;
      final List<String> typePaths = new ArrayList<String>();
      final List<Object> values = new ArrayList<Object>();
      while (hasMore) {
        switch (iterator.getNextEventType()) {
          case CsnIterator.CLASS_NAME:
            iterator.next();
            final String typePath = iterator.getPathValue();
            typePaths.add(typePath);
            break;
          case CsnIterator.FORCE_TYPE:
            iterator.next();
            if (iterator.next() == CsnIterator.CLASS_NAME) {
              typePaths.add(iterator.getPathValue());
            } else {
              throw new IllegalStateException("Expecting a class name");
            }
            break;
          case CsnIterator.EXCLUDE_TYPE:
            iterator.next();
            if (iterator.next() == CsnIterator.CLASS_NAME) {
              typePaths.add(iterator.getPathValue());
            } else {
              throw new IllegalStateException("Expecting a class name");
            }
            break;
          case CsnIterator.VALUE:
            iterator.next();
            values.add(iterator.getValue());
            break;
          default:
            hasMore = false;
            break;
        }
      }
      attributeName = attributeName.replaceFirst("position.geometry", "position");
      final int dotIndex = attributeName.indexOf('.');
      if (dotIndex == -1) {
        final FieldDefinition attribute = type.getField(attributeName);
        if (attribute != null) {
          if (!typePaths.isEmpty()) {
            attribute.setProperty(FieldProperties.ALLOWED_TYPE_NAMES, typePaths);
          }
          if (!values.isEmpty()) {
            attribute.setProperty(FieldProperties.ALLOWED_VALUES, values);
          }
        }
      } else {
        final String key = attributeName.substring(0, dotIndex);
        final String subKey = attributeName.substring(dotIndex + 1);
        final FieldDefinition attribute = type.getField(key);
        if (attribute != null) {
          if (!typePaths.isEmpty()) {
            Map<String, List<String>> allowedValues = attribute.getProperty(FieldProperties.ATTRIBUTE_ALLOWED_TYPE_NAMES);
            if (allowedValues == null) {
              allowedValues = new HashMap<String, List<String>>();
              attribute.setProperty(FieldProperties.ATTRIBUTE_ALLOWED_TYPE_NAMES, allowedValues);
            }
            allowedValues.put(subKey, typePaths);
          }
          if (!values.isEmpty()) {
            Map<String, List<Object>> allowedValues = attribute.getProperty(FieldProperties.ATTRIBUTE_ALLOWED_VALUES);
            if (allowedValues == null) {
              allowedValues = new HashMap<String, List<Object>>();
              attribute.setProperty(FieldProperties.ATTRIBUTE_ALLOWED_VALUES, allowedValues);
            }
            allowedValues.put(subKey, values);
          }
        }
      }

    }
  }

  public void setCommonMetaDataProperties(
    final List<RecordDefinitionProperty> commonMetaDataProperties) {
    this.commonMetaDataProperties = commonMetaDataProperties;
  }

  private void setMetaDataProperties(final RecordDefinitionImpl metaData) {
    for (final RecordDefinitionProperty property : this.commonMetaDataProperties) {
      final RecordDefinitionProperty clonedProperty = property.clone();
      clonedProperty.setRecordDefinition(metaData);
    }
  }

  public void subclass(final RecordDefinition type, final CsnIterator iterator) throws IOException {
    if (iterator.next() == CsnIterator.CLASS_NAME) {
      final String className = iterator.getPathValue();
      this.currentClass = new RecordDefinitionImpl(className);
      for (final RecordDefinition superClassDef : this.currentSuperClasses) {
        addSuperClass(this.currentClass, superClassDef);
      }
      // currentClass.setName(className);
      this.schema.addMetaData(this.currentClass);
    }
  }
}
