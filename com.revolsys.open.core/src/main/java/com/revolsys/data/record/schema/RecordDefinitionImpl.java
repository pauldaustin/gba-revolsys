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
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.types.DataType;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.model.ArrayDataObjectFactory;
import com.revolsys.gis.data.model.FieldProperties;
import com.revolsys.gis.data.model.DataObjectMetaDataFactory;
import com.revolsys.gis.data.model.DataObjectMetaDataFactoryImpl;
import com.revolsys.gis.data.model.DataObjectMetaDataProperty;
import com.revolsys.gis.data.model.ValueMetaDataProperty;
import com.revolsys.gis.data.model.codes.CodeTable;
import com.revolsys.io.AbstractObjectWithProperties;
import com.revolsys.io.PathUtil;
import com.revolsys.io.map.InvokeMethodMapObjectFactory;
import com.revolsys.io.map.MapObjectFactory;
import com.revolsys.io.map.MapObjectFactoryRegistry;
import com.revolsys.io.map.MapSerializerUtil;
import com.revolsys.util.CaseConverter;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.JavaBeanUtil;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.util.AssertionFailedException;

public class RecordDefinitionImpl extends AbstractObjectWithProperties
  implements RecordDefinition, Cloneable {
  private static final AtomicInteger INSTANCE_IDS = new AtomicInteger(0);

  private static final Map<Integer, RecordDefinitionImpl> METADATA_CACHE = new WeakCache<Integer, RecordDefinitionImpl>();

  public static final MapObjectFactory FACTORY = new InvokeMethodMapObjectFactory(
    "dataRecordDefinition", "Data Record Definition",
    RecordDefinitionImpl.class, "create");

  public static RecordDefinitionImpl create(
    final Map<String, Object> properties) {
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

  private final Map<String, Integer> attributeIdMap = new HashMap<String, Integer>();

  private final Map<String, FieldDefinition> attributeMap = new HashMap<String, FieldDefinition>();

  private final List<String> attributeNames = new ArrayList<String>();

  private final List<FieldDefinition> attributes = new ArrayList<FieldDefinition>();

  private Map<String, CodeTable> codeTableByColumnMap = new HashMap<String, CodeTable>();

  private RecordFactory dataObjectFactory = new ArrayDataObjectFactory();

  private DataObjectMetaDataFactory dataObjectMetaDataFactory;

  private Reference<RecordStore> dataStore;

  private Map<String, Object> defaultValues = new HashMap<String, Object>();

  /** The index of the primary geometry attribute. */
  private int geometryAttributeIndex = -1;

  private final List<Integer> geometryAttributeIndexes = new ArrayList<Integer>();

  private final List<String> geometryAttributeNames = new ArrayList<String>();

  private final List<Integer> idAttributeIndexes = new ArrayList<Integer>();

  private final List<String> idAttributeNames = new ArrayList<String>();

  private final List<FieldDefinition> idAttributes = new ArrayList<FieldDefinition>();

  /** The index of the ID attribute. */
  private int idAttributeIndex = -1;

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
    METADATA_CACHE.put(this.instanceId, this);
  }

  public RecordDefinitionImpl(final RecordStore dataObjectStore,
    final RecordStoreSchema schema, final RecordDefinition metaData) {
    this(metaData);
    this.dataStore = new WeakReference<RecordStore>(dataObjectStore);
    this.dataObjectFactory = dataObjectStore.getRecordFactory();
    this.schema = schema;
    METADATA_CACHE.put(this.instanceId, this);
  }

  public RecordDefinitionImpl(final RecordStore dataObjectStore,
    final RecordStoreSchema schema, final String typePath) {
    this(typePath);
    this.dataStore = new WeakReference<RecordStore>(dataObjectStore);
    this.dataObjectFactory = dataObjectStore.getRecordFactory();
    this.schema = schema;
    METADATA_CACHE.put(this.instanceId, this);
  }

  public RecordDefinitionImpl(final String name) {
    this.path = name;
    METADATA_CACHE.put(this.instanceId, this);
  }

  public RecordDefinitionImpl(final String name,
    final FieldDefinition... attributes) {
    this(name, null, attributes);
  }

  public RecordDefinitionImpl(final String name,
    final List<FieldDefinition> attributes) {
    this(name, null, attributes);
  }

  public RecordDefinitionImpl(final String name,
    final Map<String, Object> properties, final FieldDefinition... attributes) {
    this(name, properties, Arrays.asList(attributes));
  }

  public RecordDefinitionImpl(final String name,
    final Map<String, Object> properties, final List<FieldDefinition> attributes) {
    this.path = name;
    for (final FieldDefinition attribute : attributes) {
      addField(attribute.clone());
    }
    cloneProperties(properties);
    METADATA_CACHE.put(this.instanceId, this);
  }

  public void addField(final FieldDefinition attribute) {
    final int index = this.attributeNames.size();
    final String name = attribute.getName();
    String lowerName;
    if (name == null) {
      lowerName = null;
    } else {
      lowerName = name.toLowerCase();

    }
    this.attributeNames.add(name);
    this.attributes.add(attribute);
    this.attributeMap.put(lowerName, attribute);
    this.attributeIdMap.put(lowerName, this.attributeIdMap.size());
    final DataType dataType = attribute.getType();
    if (dataType == null) {
      LoggerFactory.getLogger(getClass()).debug(attribute.toString());
    } else {
      final Class<?> dataClass = dataType.getJavaClass();
      if (Geometry.class.isAssignableFrom(dataClass)) {
        this.geometryAttributeIndexes.add(index);
        this.geometryAttributeNames.add(name);
        if (this.geometryAttributeIndex == -1) {
          this.geometryAttributeIndex = index;
        }
      }
    }
    attribute.setIndex(index);
    attribute.setMetaData(this);
  }

  /**
   * Adds an attribute with the given case-sensitive name.
   *
   * @throws AssertionFailedException if a second Geometry is being added
   */
  public FieldDefinition addField(final String attributeName,
    final DataType type) {
    return addField(attributeName, type, false);
  }

  public FieldDefinition addField(final String name, final DataType type,
    final boolean required) {
    final FieldDefinition attribute = new FieldDefinition(name, type, required);
    addField(attribute);
    return attribute;
  }

  public FieldDefinition addAttribute(final String name, final DataType type,
    final int length, final boolean required) {
    final FieldDefinition attribute = new FieldDefinition(name, type, length,
      required);
    addField(attribute);
    return attribute;
  }

  public FieldDefinition addAttribute(final String name, final DataType type,
    final int length, final int scale, final boolean required) {
    final FieldDefinition attribute = new FieldDefinition(name, type, length,
      scale, required);
    addField(attribute);
    return attribute;
  }

  public void addColumnCodeTable(final String column, final CodeTable codeTable) {
    this.codeTableByColumnMap.put(column, codeTable);
  }

  @Override
  public void addDefaultValue(final String attributeName,
    final Object defaultValue) {
    this.defaultValues.put(attributeName, defaultValue);
  }

  public void addRestriction(final String attributePath,
    final Collection<Object> values) {
    this.restrictions.put(attributePath, values);
  }

  public void addSuperClass(final RecordDefinition superClass) {
    if (!this.superClasses.contains(superClass)) {
      this.superClasses.add(superClass);
    }
  }

  @Override
  public RecordDefinitionImpl clone() {
    final RecordDefinitionImpl clone = new RecordDefinitionImpl(this.path,
      getProperties(), this.attributes);
    clone.setIdAttributeIndex(this.idAttributeIndex);
    clone.setProperties(getProperties());
    return clone;
  }

  public void cloneProperties(final Map<String, Object> properties) {
    if (properties != null) {
      for (final Entry<String, Object> property : properties.entrySet()) {
        final String propertyName = property.getKey();
        if (property instanceof DataObjectMetaDataProperty) {
          DataObjectMetaDataProperty metaDataProperty = (DataObjectMetaDataProperty)property;
          metaDataProperty = metaDataProperty.clone();
          metaDataProperty.setMetaData(this);
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
    if (otherPath == this.path) {
      return 0;
    } else if (this.path == null) {
      return 1;
    } else if (otherPath == null) {
      return -1;
    } else {
      return this.path.compareTo(otherPath);
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
    METADATA_CACHE.remove(this.instanceId);
    this.attributeIdMap.clear();
    this.attributeMap.clear();
    this.attributeNames.clear();
    this.attributes.clear();
    this.codeTableByColumnMap.clear();
    this.dataObjectFactory = null;
    this.dataObjectMetaDataFactory = new DataObjectMetaDataFactoryImpl();
    this.dataStore = null;
    this.defaultValues.clear();
    this.description = "";
    this.geometryAttributeIndex = -1;
    this.geometryAttributeIndexes.clear();
    this.geometryAttributeNames.clear();
    this.restrictions.clear();
    this.schema = new RecordStoreSchema();
    this.superClasses.clear();
  }

  @Override
  public boolean equals(final Object other) {
    return other == this;
  }

  @Override
  public FieldDefinition getAttribute(final CharSequence name) {
    if (name == null) {
      return null;
    } else {
      final String lowerName = name.toString().toLowerCase();
      return this.attributeMap.get(lowerName);
    }
  }

  @Override
  public FieldDefinition getAttribute(final int i) {
    return this.attributes.get(i);
  }

  @Override
  public Class<?> getAttributeClass(final CharSequence name) {
    final DataType dataType = getFieldType(name);
    if (dataType == null) {
      return Object.class;
    } else {
      return dataType.getJavaClass();
    }
  }

  @Override
  public Class<?> getAttributeClass(final int i) {
    final DataType dataType = getAttributeType(i);
    if (dataType == null) {
      return Object.class;
    } else {
      return dataType.getJavaClass();
    }
  }

  @Override
  public int getAttributeCount() {
    return this.attributes.size();
  }

  @Override
  public int getAttributeIndex(final CharSequence name) {
    if (name == null) {
      return -1;
    } else {
      final String lowerName = name.toString().toLowerCase();
      final Integer attributeId = this.attributeIdMap.get(lowerName);
      if (attributeId == null) {
        return -1;
      } else {
        return attributeId;
      }
    }
  }

  @Override
  public int getAttributeLength(final int i) {
    try {
      final FieldDefinition attribute = this.attributes.get(i);
      return attribute.getLength();
    } catch (final ArrayIndexOutOfBoundsException e) {
      throw e;
    }
  }

  @Override
  public String getAttributeName(final int i) {
    try {
      if (i == -1) {
        return null;
      } else if (this.attributes == null) {
        return null;
      } else {
        final FieldDefinition attribute = this.attributes.get(i);
        return attribute.getName();
      }
    } catch (final ArrayIndexOutOfBoundsException e) {
      throw e;
    }
  }

  @Override
  public List<String> getFieldNames() {
    return new ArrayList<String>(this.attributeNames);
  }

  @Override
  public List<FieldDefinition> getFields() {
    return new ArrayList<FieldDefinition>(this.attributes);
  }

  @Override
  public int getAttributeScale(final int i) {
    final FieldDefinition attribute = this.attributes.get(i);
    return attribute.getScale();
  }

  @Override
  public String getAttributeTitle(final String fieldName) {
    final FieldDefinition attribute = getAttribute(fieldName);
    if (attribute == null) {
      return CaseConverter.toCapitalizedWords(fieldName);
    } else {
      return attribute.getTitle();
    }
  }

  @Override
  public List<String> getAttributeTitles() {
    final List<String> titles = new ArrayList<String>();
    for (final FieldDefinition attribute : getFields()) {
      titles.add(attribute.getTitle());
    }
    return titles;
  }

  @Override
  public DataType getFieldType(final CharSequence name) {
    final int index = getAttributeIndex(name);
    if (index == -1) {
      return null;
    } else {
      return getAttributeType(index);
    }
  }

  @Override
  public DataType getAttributeType(final int i) {
    final FieldDefinition attribute = this.attributes.get(i);
    return attribute.getType();
  }

  @Override
  public CodeTable getCodeTableByColumn(final String column) {
    final RecordStore dataStore = getRecordStore();
    if (dataStore == null) {
      return null;
    } else {
      CodeTable codeTable = this.codeTableByColumnMap.get(column);
      if (codeTable == null && dataStore != null) {
        codeTable = dataStore.getCodeTableByFieldName(column);
      }
      return codeTable;
    }
  }

  @Override
  public RecordFactory getDataObjectFactory() {
    return this.dataObjectFactory;
  }

  @Override
  public DataObjectMetaDataFactory getDataObjectMetaDataFactory() {
    if (this.dataObjectMetaDataFactory == null) {
      final RecordStore dataStore = getRecordStore();
      return dataStore;
    } else {
      return this.dataObjectMetaDataFactory;
    }
  }

  @Override
  public RecordStore getRecordStore() {
    if (this.dataStore == null) {
      return null;
    } else {
      return this.dataStore.get();
    }
  }

  @Override
  public Object getDefaultValue(final String attributeName) {
    return this.defaultValues.get(attributeName);
  }

  @Override
  public Map<String, Object> getDefaultValues() {
    return this.defaultValues;
  }

  public String getDescription() {
    return this.description;
  }

  @Override
  public FieldDefinition getGeometryField() {
    if (this.geometryAttributeIndex == -1) {
      return null;
    } else {
      return this.attributes.get(this.geometryAttributeIndex);
    }
  }

  @Override
  public int getGeometryAttributeIndex() {
    return this.geometryAttributeIndex;
  }

  @Override
  public List<Integer> getGeometryAttributeIndexes() {
    return Collections.unmodifiableList(this.geometryAttributeIndexes);
  }

  @Override
  public String getGeometryAttributeName() {
    return getAttributeName(this.geometryAttributeIndex);
  }

  @Override
  public List<String> getGeometryAttributeNames() {
    return Collections.unmodifiableList(this.geometryAttributeNames);
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
  public FieldDefinition getIdAttribute() {
    if (this.idAttributeIndex >= 0) {
      return this.attributes.get(this.idAttributeIndex);
    } else {
      return null;
    }
  }

  @Override
  public int getIdFieldIndex() {
    return this.idAttributeIndex;
  }

  @Override
  public List<Integer> getIdAttributeIndexes() {
    return Collections.unmodifiableList(this.idAttributeIndexes);
  }

  @Override
  public String getIdFieldName() {
    return getAttributeName(this.idAttributeIndex);
  }

  @Override
  public List<String> getIdAttributeNames() {
    return Collections.unmodifiableList(this.idAttributeNames);
  }

  @Override
  public List<FieldDefinition> getIdAttributes() {
    return Collections.unmodifiableList(this.idAttributes);
  }

  @Override
  public int getInstanceId() {
    return this.instanceId;
  }

  @Override
  public String getPath() {
    return this.path;
  }

  public Map<String, Collection<Object>> getRestrictions() {
    return this.restrictions;
  }

  public RecordStoreSchema getSchema() {
    return this.schema;
  }

  @Override
  public String getTypeName() {
    return PathUtil.getName(this.path);
  }

  @Override
  public boolean hasAttribute(final CharSequence name) {
    final String lowerName = name.toString().toLowerCase();
    return this.attributeMap.containsKey(lowerName);
  }

  @Override
  public int hashCode() {
    if (this.path == null) {
      return super.hashCode();
    } else {
      return this.path.hashCode();
    }
  }

  @Override
  public boolean isAttributeRequired(final CharSequence name) {
    final FieldDefinition attribute = getAttribute(name);
    return attribute.isRequired();
  }

  @Override
  public boolean isAttributeRequired(final int i) {
    final FieldDefinition attribute = getAttribute(i);
    return attribute.isRequired();
  }

  @Override
  public boolean isInstanceOf(final RecordDefinition classDefinition) {
    if (classDefinition == null) {
      return false;
    }
    if (equals(classDefinition)) {
      return true;
    }
    for (final RecordDefinition superClass : this.superClasses) {
      if (superClass.isInstanceOf(classDefinition)) {
        return true;
      }
    }
    return false;
  }

  private void readObject(final ObjectInputStream ois)
    throws ClassNotFoundException, IOException {
    ois.defaultReadObject();
    METADATA_CACHE.put(this.instanceId, this);
  }

  public void replaceAttribute(final FieldDefinition attribute,
    final FieldDefinition newAttribute) {
    final String name = attribute.getName();
    final String lowerName = name.toLowerCase();
    final String newName = newAttribute.getName();
    if (this.attributes.contains(attribute) && name.equals(newName)) {
      final int index = attribute.getIndex();
      this.attributes.set(index, newAttribute);
      this.attributeMap.put(lowerName, newAttribute);
      newAttribute.setIndex(index);
    } else {
      addField(newAttribute);
    }
  }

  public void setCodeTableByColumnMap(
    final Map<String, CodeTable> codeTableByColumnMap) {
    this.codeTableByColumnMap = codeTableByColumnMap;
  }

  public void setDataObjectMetaDataFactory(
    final DataObjectMetaDataFactory dataObjectMetaDataFactory) {
    this.dataObjectMetaDataFactory = dataObjectMetaDataFactory;
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

  /**
   * @param geometryAttributeIndex the geometryAttributeIndex to set
   */
  public void setGeometryAttributeIndex(final int geometryAttributeIndex) {
    this.geometryAttributeIndex = geometryAttributeIndex;
  }

  public void setGeometryFieldName(final String name) {
    final int id = getAttributeIndex(name);
    setGeometryAttributeIndex(id);
  }

  @Override
  public void setGeometryFactory(final GeometryFactory geometryFactory) {
    final FieldDefinition geometryAttribute = getGeometryField();
    if (geometryAttribute != null) {
      geometryAttribute.setProperty(FieldProperties.GEOMETRY_FACTORY,
        geometryFactory);
    }
  }

  /**
   * @param idAttributeIndex the idAttributeIndex to set
   */
  public void setIdAttributeIndex(final int idAttributeIndex) {
    this.idAttributeIndex = idAttributeIndex;
    this.idAttributeIndexes.clear();
    this.idAttributeIndexes.add(idAttributeIndex);
    this.idAttributeNames.clear();
    this.idAttributeNames.add(getIdFieldName());
    this.idAttributes.clear();
    this.idAttributes.add(getIdAttribute());
  }

  public void setIdFieldName(final String name) {
    final int id = getAttributeIndex(name);
    setIdAttributeIndex(id);
  }

  public void setIdAttributeNames(final Collection<String> names) {
    if (names != null) {
      if (names.size() == 1) {
        final String name = CollectionUtil.get(names, 0);
        setIdFieldName(name);
      } else {
        for (final String name : names) {
          final int index = getAttributeIndex(name);
          if (index == -1) {
            LoggerFactory.getLogger(getClass()).error(
              "Cannot set ID " + getPath() + "." + name + " does not exist");
          } else {
            this.idAttributeIndexes.add(index);
            this.idAttributeNames.add(name);
            this.idAttributes.add(getAttribute(index));
          }
        }
      }
    }
  }

  public void setIdAttributeNames(final String... names) {
    setIdAttributeNames(Arrays.asList(names));
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
        if (value instanceof ValueMetaDataProperty) {
          final ValueMetaDataProperty valueProperty = (ValueMetaDataProperty)value;
          final String propertyName = valueProperty.getPropertyName();
          final Object propertyValue = valueProperty.getValue();
          JavaBeanUtil.setProperty(this, propertyName, propertyValue);
        }
        if (value instanceof DataObjectMetaDataProperty) {
          final DataObjectMetaDataProperty property = (DataObjectMetaDataProperty)value;
          final DataObjectMetaDataProperty clonedProperty = property.clone();
          clonedProperty.setMetaData(this);
        } else {
          setProperty(key, value);
        }
      }
    }

  }

  @Override
  public Map<String, Object> toMap() {
    final Map<String, Object> map = new LinkedHashMap<String, Object>();
    map.put("type", "dataRecordDefinition");
    final String path = getPath();
    map.put("path", path);
    final GeometryFactory geometryFactory = getGeometryFactory();
    MapSerializerUtil.add(map, "geometryFactory", geometryFactory, null);
    final List<FieldDefinition> attributes = getFields();
    MapSerializerUtil.add(map, "fields", attributes);
    return map;
  }

  @Override
  public String toString() {
    return this.path.toString();
  }
}
