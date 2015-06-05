package com.revolsys.data.record.schema;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;

import org.slf4j.LoggerFactory;

import com.revolsys.collection.WeakCache;
import com.revolsys.data.codes.CodeTable;
import com.revolsys.data.record.ArrayRecordFactory;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.property.FieldProperties;
import com.revolsys.data.record.property.RecordDefinitionProperty;
import com.revolsys.data.record.property.ValueRecordDefinitionProperty;
import com.revolsys.data.types.DataType;
import com.revolsys.gis.data.model.DataObjectMetaDataFactoryImpl;
import com.revolsys.io.AbstractObjectWithProperties;
import com.revolsys.io.Path;
import com.revolsys.io.map.InvokeMethodMapObjectFactory;
import com.revolsys.io.map.MapObjectFactory;
import com.revolsys.io.map.MapObjectFactoryRegistry;
import com.revolsys.io.map.MapSerializerUtil;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.util.CaseConverter;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.JavaBeanUtil;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.util.AssertionFailedException;

public class RecordDefinitionImpl extends AbstractObjectWithProperties implements RecordDefinition,
  Cloneable {
  private static final AtomicInteger INSTANCE_IDS = new AtomicInteger(0);

  private static final Map<Integer, RecordDefinitionImpl> METADATA_CACHE = new WeakCache<Integer, RecordDefinitionImpl>();

  public static final MapObjectFactory FACTORY = new InvokeMethodMapObjectFactory(
    "dataRecordDefinition", "Data Record Definition", RecordDefinitionImpl.class, "create");

  public static RecordDefinitionImpl create(final Map<String, Object> properties) {
    return new RecordDefinitionImpl(properties);
  }

  public static void destroy(final RecordDefinitionImpl... metaDataList) {
    for (final RecordDefinitionImpl metaData : metaDataList) {
      metaData.destroy();
    }
  }

  public static RecordDefinition getMetaData(final int instanceId) {
    return METADATA_CACHE.get(instanceId);
  }

  private final Map<String, Integer> fieldIdMap = new HashMap<String, Integer>();

  private final Map<String, FieldDefinition> fieldMap = new HashMap<String, FieldDefinition>();

  private final List<String> fieldNames = new ArrayList<String>();

  private final List<FieldDefinition> fields = new ArrayList<FieldDefinition>();

  private Map<String, CodeTable> codeTableByColumnMap = new HashMap<String, CodeTable>();

  private RecordFactory dataObjectFactory = new ArrayRecordFactory();

  private RecordDefinitionFactory recordDefinitionFactory;

  private Reference<RecordStore> dataStore;

  private Map<String, Object> defaultValues = new HashMap<String, Object>();

  /** The index of the primary geometry field. */
  private int geometryFieldIndex = -1;

  private final List<Integer> geometryAttributeIndexes = new ArrayList<Integer>();

  private final List<String> geometryAttributeNames = new ArrayList<String>();

  private final List<Integer> idFieldIndexes = new ArrayList<Integer>();

  private final List<String> idFieldNames = new ArrayList<String>();

  private final List<FieldDefinition> idFields = new ArrayList<FieldDefinition>();

  /** The index of the ID field. */
  private int idFieldIndex = -1;

  private final Integer instanceId = INSTANCE_IDS.getAndIncrement();

  /** The path of the data type. */
  private String path;

  private final Map<String, Collection<Object>> restrictions = new HashMap<String, Collection<Object>>();

  private RecordStoreSchema schema;

  private final List<RecordDefinition> superClasses = new ArrayList<RecordDefinition>();

  private String description;

  public RecordDefinitionImpl() {
  }

  @SuppressWarnings("unchecked")
  public RecordDefinitionImpl(final Map<String, Object> properties) {
    this(CollectionUtil.getString(properties, "path"));
    final List<Object> fields = (List<Object>)properties.get("fields");
    for (final Object object : fields) {
      if (object instanceof FieldDefinition) {
        final FieldDefinition field = (FieldDefinition)object;
        addField(field.clone());
      } else if (object instanceof Map) {
        final Map<String, Object> fieldProperties = (Map<String, Object>)object;
        final FieldDefinition field = FieldDefinition.create(fieldProperties);
        addField(field);
      }
    }
    final Map<String, Object> geometryFactoryDef = (Map<String, Object>)properties.get("geometryFactory");
    if (geometryFactoryDef != null) {
      final GeometryFactory geometryFactory = MapObjectFactoryRegistry.toObject(geometryFactoryDef);
      setGeometryFactory(geometryFactory);
    }
  }

  public RecordDefinitionImpl(final RecordDefinition metaData) {
    this(metaData.getPath(), metaData.getProperties(), metaData.getFields());
    setIdAttributeIndex(metaData.getIdFieldIndex());
    METADATA_CACHE.put(instanceId, this);
  }

  public RecordDefinitionImpl(final RecordStore dataObjectStore, final RecordStoreSchema schema,
    final RecordDefinition metaData) {
    this(metaData);
    dataStore = new WeakReference<RecordStore>(dataObjectStore);
    dataObjectFactory = dataObjectStore.getRecordFactory();
    this.schema = schema;
    METADATA_CACHE.put(instanceId, this);
  }

  public RecordDefinitionImpl(final RecordStore dataObjectStore, final RecordStoreSchema schema,
    final String typePath) {
    this(typePath);
    dataStore = new WeakReference<RecordStore>(dataObjectStore);
    dataObjectFactory = dataObjectStore.getRecordFactory();
    this.schema = schema;
    METADATA_CACHE.put(instanceId, this);
  }

  public RecordDefinitionImpl(final RecordStoreSchema schema, final String path,
    final Map<String, Object> properties, final List<FieldDefinition> fields) {
    this(path);
    // TODO implement schema support
    for (final FieldDefinition field : fields) {
      addField(field.clone());
    }
    cloneProperties(properties);
  }

  public RecordDefinitionImpl(final String name) {
    path = name;
    METADATA_CACHE.put(instanceId, this);
  }

  public RecordDefinitionImpl(final String name, final FieldDefinition... fields) {
    this(name, null, fields);
  }

  public RecordDefinitionImpl(final String name, final List<FieldDefinition> fields) {
    this(name, null, fields);
  }

  public RecordDefinitionImpl(final String name, final Map<String, Object> properties,
    final FieldDefinition... fields) {
    this(name, properties, Arrays.asList(fields));
  }

  public RecordDefinitionImpl(final String name, final Map<String, Object> properties,
    final List<FieldDefinition> fields) {
    path = name;
    for (final FieldDefinition field : fields) {
      addField(field.clone());
    }
    cloneProperties(properties);
    METADATA_CACHE.put(instanceId, this);
  }

  public FieldDefinition addAttribute(final String name, final DataType type, final int length,
    final int scale, final boolean required) {
    final FieldDefinition field = new FieldDefinition(name, type, length, scale, required);
    addField(field);
    return field;
  }

  public void addColumnCodeTable(final String column, final CodeTable codeTable) {
    codeTableByColumnMap.put(column, codeTable);
  }

  @Override
  public void addDefaultValue(final String fieldName, final Object defaultValue) {
    defaultValues.put(fieldName, defaultValue);
  }

  public void addField(final FieldDefinition field) {
    final int index = fieldNames.size();
    final String name = field.getName();
    String lowerName;
    if (name == null) {
      lowerName = null;
    } else {
      lowerName = name.toLowerCase();

    }
    fieldNames.add(name);
    fields.add(field);
    fieldMap.put(lowerName, field);
    fieldIdMap.put(lowerName, fieldIdMap.size());
    final DataType dataType = field.getType();
    if (dataType == null) {
      LoggerFactory.getLogger(getClass()).debug(field.toString());
    } else {
      final Class<?> dataClass = dataType.getJavaClass();
      if (Geometry.class.isAssignableFrom(dataClass)) {
        geometryAttributeIndexes.add(index);
        geometryAttributeNames.add(name);
        if (geometryFieldIndex == -1) {
          geometryFieldIndex = index;
        }
      }
    }
    field.setIndex(index);
    field.setMetaData(this);
  }

  /**
   * Adds an field with the given case-sensitive name.
   *
   * @throws AssertionFailedException if a second Geometry is being added
   */
  public FieldDefinition addField(final String fieldName, final DataType type) {
    return addField(fieldName, type, false);
  }

  public FieldDefinition addField(final String name, final DataType type, final boolean required) {
    final FieldDefinition field = new FieldDefinition(name, type, required);
    addField(field);
    return field;
  }

  public FieldDefinition addField(final String name, final DataType type, final int length,
    final boolean required) {
    final FieldDefinition field = new FieldDefinition(name, type, length, required);
    addField(field);
    return field;
  }

  public FieldDefinition addField(final String name, final DataType type, final int length,
    final int scale) {
    final FieldDefinition field = new FieldDefinition(name, type, length, scale, false);
    addField(field);
    return field;
  }

  public FieldDefinition addField(final String name, final DataType type, final int length,
    final int scale, final boolean required) {
    final FieldDefinition fieldDefinition = new FieldDefinition(name, type, length, scale, required);
    addField(fieldDefinition);
    return fieldDefinition;
  }

  public void addRestriction(final String fieldPath, final Collection<Object> values) {
    restrictions.put(fieldPath, values);
  }

  public void addSuperClass(final RecordDefinition superClass) {
    if (!superClasses.contains(superClass)) {
      superClasses.add(superClass);
    }
  }

  @Override
  public RecordDefinitionImpl clone() {
    final RecordDefinitionImpl clone = new RecordDefinitionImpl(path, getProperties(), fields);
    clone.setIdAttributeIndex(idFieldIndex);
    clone.setProperties(getProperties());
    return clone;
  }

  public void cloneProperties(final Map<String, Object> properties) {
    if (properties != null) {
      for (final Entry<String, Object> property : properties.entrySet()) {
        final String propertyName = property.getKey();
        if (property instanceof RecordDefinitionProperty) {
          RecordDefinitionProperty metaDataProperty = (RecordDefinitionProperty)property;
          metaDataProperty = metaDataProperty.clone();
          metaDataProperty.setRecordDefinition(this);
          setProperty(propertyName, metaDataProperty);
        } else {
          setProperty(propertyName, property);
        }
      }
    }
  }

  @Override
  public int compareTo(final RecordDefinition other) {
    final String otherPath = other.getPath();
    if (otherPath == path) {
      return 0;
    } else if (path == null) {
      return 1;
    } else if (otherPath == null) {
      return -1;
    } else {
      return path.compareTo(otherPath);
    }
  }

  @Override
  public Record createDataObject() {
    final RecordFactory dataObjectFactory = this.dataObjectFactory;
    if (dataObjectFactory == null) {
      return null;
    } else {
      return dataObjectFactory.createRecord(this);
    }
  }

  @Override
  public void delete(final Record dataObject) {
    final RecordStore dataStore = getRecordStore();
    if (dataStore == null) {
      throw new UnsupportedOperationException();
    } else {
      dataStore.delete(dataObject);
    }
  }

  @Override
  @PreDestroy
  public void destroy() {
    super.close();
    METADATA_CACHE.remove(instanceId);
    fieldIdMap.clear();
    fieldMap.clear();
    fieldNames.clear();
    fields.clear();
    codeTableByColumnMap.clear();
    dataObjectFactory = null;
    recordDefinitionFactory = new DataObjectMetaDataFactoryImpl();
    dataStore = null;
    defaultValues.clear();
    description = "";
    geometryFieldIndex = -1;
    geometryAttributeIndexes.clear();
    geometryAttributeNames.clear();
    restrictions.clear();
    schema = new RecordStoreSchema();
    superClasses.clear();
  }

  @Override
  public boolean equals(final Object other) {
    return other == this;
  }

  @Override
  public CodeTable getCodeTableByFieldName(final String column) {
    final RecordStore dataStore = getRecordStore();
    if (dataStore == null) {
      return null;
    } else {
      CodeTable codeTable = codeTableByColumnMap.get(column);
      if (codeTable == null && dataStore != null) {
        codeTable = dataStore.getCodeTableByFieldName(column);
      }
      return codeTable;
    }
  }

  @Override
  public Object getDefaultValue(final String fieldName) {
    return defaultValues.get(fieldName);
  }

  @Override
  public Map<String, Object> getDefaultValues() {
    return defaultValues;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public FieldDefinition getField(final CharSequence name) {
    if (name == null) {
      return null;
    } else {
      final String lowerName = name.toString().toLowerCase();
      return fieldMap.get(lowerName);
    }
  }

  @Override
  public FieldDefinition getField(final int i) {
    return fields.get(i);
  }

  @Override
  public Class<?> getFieldClass(final CharSequence name) {
    final DataType dataType = getFieldType(name);
    if (dataType == null) {
      return Object.class;
    } else {
      return dataType.getJavaClass();
    }
  }

  @Override
  public Class<?> getFieldClass(final int i) {
    final DataType dataType = getFieldType(i);
    if (dataType == null) {
      return Object.class;
    } else {
      return dataType.getJavaClass();
    }
  }

  @Override
  public int getFieldCount() {
    return fields.size();
  }

  @Override
  public int getFieldIndex(final CharSequence name) {
    if (name == null) {
      return -1;
    } else {
      final String lowerName = name.toString().toLowerCase();
      final Integer fieldId = fieldIdMap.get(lowerName);
      if (fieldId == null) {
        return -1;
      } else {
        return fieldId;
      }
    }
  }

  @Override
  public int getFieldLength(final int i) {
    try {
      final FieldDefinition field = fields.get(i);
      return field.getLength();
    } catch (final ArrayIndexOutOfBoundsException e) {
      throw e;
    }
  }

  @Override
  public String getFieldName(final int i) {
    try {
      if (i == -1) {
        return null;
      } else if (fields == null) {
        return null;
      } else {
        final FieldDefinition field = fields.get(i);
        return field.getName();
      }
    } catch (final ArrayIndexOutOfBoundsException e) {
      throw e;
    }
  }

  @Override
  public List<String> getFieldNames() {
    return new ArrayList<String>(fieldNames);
  }

  @Override
  public List<FieldDefinition> getFields() {
    return new ArrayList<FieldDefinition>(fields);
  }

  @Override
  public int getFieldScale(final int i) {
    final FieldDefinition field = fields.get(i);
    return field.getScale();
  }

  @Override
  public String getFieldTitle(final String fieldName) {
    final FieldDefinition field = getField(fieldName);
    if (field == null) {
      return CaseConverter.toCapitalizedWords(fieldName);
    } else {
      return field.getTitle();
    }
  }

  @Override
  public List<String> getFieldTitles() {
    final List<String> titles = new ArrayList<String>();
    for (final FieldDefinition field : getFields()) {
      titles.add(field.getTitle());
    }
    return titles;
  }

  @Override
  public DataType getFieldType(final CharSequence name) {
    final int index = getFieldIndex(name);
    if (index == -1) {
      return null;
    } else {
      return getFieldType(index);
    }
  }

  @Override
  public DataType getFieldType(final int i) {
    final FieldDefinition field = fields.get(i);
    return field.getType();
  }

  @Override
  public GeometryFactory getGeometryFactory() {
    final FieldDefinition geometryAttribute = getGeometryField();
    if (geometryAttribute == null) {
      return null;
    } else {
      final GeometryFactory geometryFactory = geometryAttribute.getProperty(FieldProperties.GEOMETRY_FACTORY);
      return geometryFactory;
    }
  }

  @Override
  public FieldDefinition getGeometryField() {
    if (geometryFieldIndex == -1) {
      return null;
    } else {
      return fields.get(geometryFieldIndex);
    }
  }

  @Override
  public int getGeometryFieldIndex() {
    return geometryFieldIndex;
  }

  @Override
  public List<Integer> getGeometryFieldIndexes() {
    return Collections.unmodifiableList(geometryAttributeIndexes);
  }

  @Override
  public String getGeometryFieldName() {
    return getFieldName(geometryFieldIndex);
  }

  @Override
  public List<String> getGeometryFieldNames() {
    return Collections.unmodifiableList(geometryAttributeNames);
  }

  @Override
  public FieldDefinition getIdField() {
    if (idFieldIndex >= 0) {
      return fields.get(idFieldIndex);
    } else {
      return null;
    }
  }

  @Override
  public int getIdFieldIndex() {
    return idFieldIndex;
  }

  @Override
  public List<Integer> getIdFieldIndexes() {
    return Collections.unmodifiableList(idFieldIndexes);
  }

  @Override
  public String getIdFieldName() {
    return getFieldName(idFieldIndex);
  }

  @Override
  public List<String> getIdFieldNames() {
    return Collections.unmodifiableList(idFieldNames);
  }

  @Override
  public List<FieldDefinition> getIdFields() {
    return Collections.unmodifiableList(idFields);
  }

  @Override
  public int getInstanceId() {
    return instanceId;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public RecordDefinitionFactory getRecordDefinitionFactory() {
    if (recordDefinitionFactory == null) {
      final RecordStore dataStore = getRecordStore();
      return dataStore;
    } else {
      return recordDefinitionFactory;
    }
  }

  @Override
  public RecordFactory getRecordFactory() {
    return dataObjectFactory;
  }

  @Override
  public RecordStore getRecordStore() {
    if (dataStore == null) {
      return null;
    } else {
      return dataStore.get();
    }
  }

  public Map<String, Collection<Object>> getRestrictions() {
    return restrictions;
  }

  public RecordStoreSchema getSchema() {
    return schema;
  }

  @Override
  public String getTypeName() {
    return Path.getName(path);
  }

  @Override
  public boolean hasField(final CharSequence name) {
    final String lowerName = name.toString().toLowerCase();
    return fieldMap.containsKey(lowerName);
  }

  @Override
  public int hashCode() {
    if (path == null) {
      return super.hashCode();
    } else {
      return path.hashCode();
    }
  }

  @Override
  public boolean isFieldRequired(final CharSequence name) {
    final FieldDefinition field = getField(name);
    return field.isRequired();
  }

  @Override
  public boolean isFieldRequired(final int i) {
    final FieldDefinition field = getField(i);
    return field.isRequired();
  }

  @Override
  public boolean isInstanceOf(final RecordDefinition classDefinition) {
    if (classDefinition == null) {
      return false;
    }
    if (equals(classDefinition)) {
      return true;
    }
    for (final RecordDefinition superClass : superClasses) {
      if (superClass.isInstanceOf(classDefinition)) {
        return true;
      }
    }
    return false;
  }

  private void readObject(final ObjectInputStream ois) throws ClassNotFoundException, IOException {
    ois.defaultReadObject();
    METADATA_CACHE.put(instanceId, this);
  }

  public void replaceAttribute(final FieldDefinition field, final FieldDefinition newAttribute) {
    final String name = field.getName();
    final String lowerName = name.toLowerCase();
    final String newName = newAttribute.getName();
    if (fields.contains(field) && name.equals(newName)) {
      final int index = field.getIndex();
      fields.set(index, newAttribute);
      fieldMap.put(lowerName, newAttribute);
      newAttribute.setIndex(index);
    } else {
      addField(newAttribute);
    }
  }

  public void setCodeTableByColumnMap(final Map<String, CodeTable> codeTableByColumnMap) {
    this.codeTableByColumnMap = codeTableByColumnMap;
  }

  @Override
  public void setDefaultValues(final Map<String, ? extends Object> defaultValues) {
    if (defaultValues == null) {
      this.defaultValues = new HashMap<>();
    } else {
      this.defaultValues = new HashMap<>(defaultValues);
    }
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  @Override
  public void setGeometryFactory(final GeometryFactory geometryFactory) {
    final FieldDefinition geometryAttribute = getGeometryField();
    if (geometryAttribute != null) {
      geometryAttribute.setProperty(FieldProperties.GEOMETRY_FACTORY, geometryFactory);
    }
  }

  /**
   * @param geometryFieldIndex the geometryFieldIndex to set
   */
  public void setGeometryFieldIndex(final int geometryAttributeIndex) {
    geometryFieldIndex = geometryAttributeIndex;
  }

  public void setGeometryFieldName(final String name) {
    final int id = getFieldIndex(name);
    setGeometryFieldIndex(id);
  }

  /**
   * @param idFieldIndex the idFieldIndex to set
   */
  public void setIdAttributeIndex(final int idFieldIndex) {
    this.idFieldIndex = idFieldIndex;
    idFieldIndexes.clear();
    idFieldIndexes.add(idFieldIndex);
    idFieldNames.clear();
    idFieldNames.add(getIdFieldName());
    idFields.clear();
    idFields.add(getIdField());
  }

  public void setIdFieldName(final String name) {
    final int id = getFieldIndex(name);
    setIdAttributeIndex(id);
  }

  public void setIdFieldNames(final Collection<String> names) {
    if (names != null) {
      if (names.size() == 1) {
        final String name = CollectionUtil.get(names, 0);
        setIdFieldName(name);
      } else {
        for (final String name : names) {
          final int index = getFieldIndex(name);
          if (index == -1) {
            LoggerFactory.getLogger(getClass()).error(
              "Cannot set ID " + getPath() + "." + name + " does not exist");
          } else {
            idFieldIndexes.add(index);
            idFieldNames.add(name);
            idFields.add(getField(index));
          }
        }
      }
    }
  }

  public void setIdFieldNames(final String... names) {
    setIdFieldNames(Arrays.asList(names));
  }

  @Override
  public void setName(final String path) {
    this.path = path;
  }

  @Override
  public void setProperties(final Map<String, ? extends Object> properties) {
    if (properties != null) {
      for (final Entry<String, ? extends Object> entry : properties.entrySet()) {
        final String key = entry.getKey();
        final Object value = entry.getValue();
        if (value instanceof ValueRecordDefinitionProperty) {
          final ValueRecordDefinitionProperty valueProperty = (ValueRecordDefinitionProperty)value;
          final String propertyName = valueProperty.getPropertyName();
          final Object propertyValue = valueProperty.getValue();
          JavaBeanUtil.setProperty(this, propertyName, propertyValue);
        }
        if (value instanceof RecordDefinitionProperty) {
          final RecordDefinitionProperty property = (RecordDefinitionProperty)value;
          final RecordDefinitionProperty clonedProperty = property.clone();
          clonedProperty.setRecordDefinition(this);
        } else {
          setProperty(key, value);
        }
      }
    }

  }

  public void setRecordDefinitionFactory(final RecordDefinitionFactory recordDefinitionFactory) {
    this.recordDefinitionFactory = recordDefinitionFactory;
  }

  @Override
  public Map<String, Object> toMap() {
    final Map<String, Object> map = new LinkedHashMap<String, Object>();
    map.put("type", "dataRecordDefinition");
    final String path = getPath();
    map.put("path", path);
    final GeometryFactory geometryFactory = getGeometryFactory();
    MapSerializerUtil.add(map, "geometryFactory", geometryFactory, null);
    final List<FieldDefinition> fields = getFields();
    MapSerializerUtil.add(map, "fields", fields);
    return map;
  }

  @Override
  public String toString() {
    return path.toString();
  }
}
