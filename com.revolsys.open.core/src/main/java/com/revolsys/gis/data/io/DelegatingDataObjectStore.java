package com.revolsys.gis.data.io;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.transaction.PlatformTransactionManager;

import com.revolsys.collection.ResultPager;
import com.revolsys.data.codes.CodeTable;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.property.RecordDefinitionProperty;
import com.revolsys.data.record.schema.AbstractRecordStore;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordStoreSchema;
import com.revolsys.gis.io.Statistics;
import com.revolsys.gis.io.StatisticsMap;
import com.revolsys.io.Reader;
import com.revolsys.io.Writer;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Geometry;

public class DelegatingDataObjectStore extends AbstractRecordStore {
  private final AbstractRecordStore dataStore;

  public DelegatingDataObjectStore(final AbstractRecordStore dataStore) {
    this.dataStore = dataStore;
  }

  @Override
  public void addCodeTable(final CodeTable codeTable) {
    this.dataStore.addCodeTable(codeTable);
  }

  @Override
  public void addCodeTable(final String columnName, final CodeTable codeTable) {
    this.dataStore.addCodeTable(columnName, codeTable);
  }

  @Override
  public void addCodeTables(final Collection<CodeTable> codeTables) {
    this.dataStore.addCodeTables(codeTables);
  }

  @Override
  public void addStatistic(final String statisticName, final Record object) {
    this.dataStore.addStatistic(statisticName, object);
  }

  @Override
  public void addStatistic(final String statisticName, final String typePath, final int count) {
    this.dataStore.addStatistic(statisticName, typePath, count);
  }

  @Override
  public void clearProperties() {
    this.dataStore.clearProperties();
  }

  @Override
  @PreDestroy
  public void close() {
    this.dataStore.close();
  }

  @Override
  public Record create(final RecordDefinition objectMetaData) {
    return this.dataStore.create(objectMetaData);
  }

  @Override
  public Record create(final String typePath) {
    return this.dataStore.create(typePath);
  }

  @Override
  public <T> T createPrimaryIdValue(final String typePath) {
    return this.dataStore.createPrimaryIdValue(typePath);
  }

  @Override
  public Query createQuery(final String typePath, final String whereClause,
    final BoundingBox boundingBox) {
    return this.dataStore.createQuery(typePath, whereClause, boundingBox);
  }

  @Override
  public DataObjectStoreQueryReader createReader() {
    return this.dataStore.createReader();
  }

  @Override
  public Writer<Record> createWriter() {
    return this.dataStore.createWriter();
  }

  @Override
  public int delete(final Query query) {
    return this.dataStore.delete(query);
  }

  @Override
  public void delete(final Record object) {
    this.dataStore.delete(object);
  }

  @Override
  public void deleteAll(final Collection<Record> objects) {
    this.dataStore.deleteAll(objects);
  }

  @Override
  public boolean equals(final Object obj) {
    return this.dataStore.equals(obj);
  }

  @Override
  public CodeTable getCodeTable(final String typePath) {
    return this.dataStore.getCodeTable(typePath);
  }

  @Override
  public Map<String, CodeTable> getCodeTableByColumnMap() {
    return this.dataStore.getCodeTableByColumnMap();
  }

  @Override
  public CodeTable getCodeTableByFieldName(final String columnName) {
    return this.dataStore.getCodeTableByFieldName(columnName);
  }

  @Override
  public Map<String, List<String>> getCodeTableColumNames() {
    return this.dataStore.getCodeTableColumNames();
  }

  public AbstractRecordStore getDataStore() {
    return this.dataStore;
  }

  @Override
  public GeometryFactory getGeometryFactory() {
    return this.dataStore.getGeometryFactory();
  }

  @Override
  public String getLabel() {
    return this.dataStore.getLabel();
  }

  @Override
  public Map<String, Object> getProperties() {
    return this.dataStore.getProperties();
  }

  @Override
  public <C> C getProperty(final String name) {
    return this.dataStore.getProperty(name);
  }

  @Override
  public <C> C getProperty(final String name, final C defaultValue) {
    return this.dataStore.getProperty(name, defaultValue);
  }

  @Override
  public RecordDefinition getRecordDefinition(final RecordDefinition objectMetaData) {
    return this.dataStore.getRecordDefinition(objectMetaData);
  }

  @Override
  public RecordDefinition getRecordDefinition(final String typePath) {
    return this.dataStore.getRecordDefinition(typePath);
  }

  @Override
  public RecordFactory getRecordFactory() {
    return this.dataStore.getRecordFactory();
  }

  @Override
  public int getRowCount(final Query query) {
    return this.dataStore.getRowCount(query);
  }

  @Override
  public RecordStoreSchema getSchema(final String schemaName) {
    return this.dataStore.getSchema(schemaName);
  }

  @Override
  public Map<String, RecordStoreSchema> getSchemaMap() {
    return this.dataStore.getSchemaMap();
  }

  @Override
  public List<RecordStoreSchema> getSchemas() {
    return this.dataStore.getSchemas();
  }

  @Override
  public StatisticsMap getStatistics() {
    return this.dataStore.getStatistics();
  }

  @Override
  public Statistics getStatistics(final String name) {
    return this.dataStore.getStatistics(name);
  }

  @Override
  public String getString(final Object name) {
    return this.dataStore.getString(name);
  }

  @Override
  public PlatformTransactionManager getTransactionManager() {
    return this.dataStore.getTransactionManager();
  }

  @Override
  public List<String> getTypeNames(final String schemaName) {
    return this.dataStore.getTypeNames(schemaName);
  }

  @Override
  public List<RecordDefinition> getTypes(final String namespace) {
    return this.dataStore.getTypes(namespace);
  }

  @Override
  public Writer<Record> getWriter() {
    return this.dataStore.getWriter();
  }

  @Override
  public int hashCode() {
    return this.dataStore.hashCode();
  }

  @Override
  @PostConstruct
  public void initialize() {
    this.dataStore.initialize();
  }

  @Override
  public void insert(final Record dataObject) {
    this.dataStore.insert(dataObject);
  }

  @Override
  public void insertAll(final Collection<Record> objects) {
    this.dataStore.insertAll(objects);
  }

  @Override
  public boolean isEditable(final String typePath) {
    return this.dataStore.isEditable(typePath);
  }

  @Override
  public Record load(final String typePath, final Object... id) {
    return this.dataStore.load(typePath, id);
  }

  @Override
  protected void loadSchemaDataObjectMetaData(final RecordStoreSchema schema,
    final Map<String, RecordDefinition> metaDataMap) {
  }

  @Override
  protected void loadSchemas(final Map<String, RecordStoreSchema> schemaMap) {
  }

  @Override
  public Record lock(final String typePath, final Object id) {
    return this.dataStore.lock(typePath, id);
  }

  @Override
  public ResultPager<Record> page(final Query query) {
    return this.dataStore.page(query);
  }

  @Override
  public Reader<Record> query(final List<?> queries) {
    return this.dataStore.query(queries);
  }

  @Override
  public Reader<Record> query(final Query... queries) {
    return this.dataStore.query(queries);
  }

  @Override
  public Reader<Record> query(final RecordFactory dataObjectFactory, final String typePath,
    final Geometry geometry) {
    return this.dataStore.query(dataObjectFactory, typePath, geometry);
  }

  @Override
  public Reader<Record> query(final String path) {
    return this.dataStore.query(path);
  }

  @Override
  public Record queryFirst(final Query query) {
    return this.dataStore.queryFirst(query);
  }

  @Override
  public void removeProperty(final String propertyName) {
    this.dataStore.removeProperty(propertyName);
  }

  @Override
  public void setCodeTableColumNames(final Map<String, List<String>> domainColumNames) {
    this.dataStore.setCodeTableColumNames(domainColumNames);
  }

  @Override
  public void setCommonMetaDataProperties(
    final List<RecordDefinitionProperty> commonMetaDataProperties) {
    this.dataStore.setCommonMetaDataProperties(commonMetaDataProperties);
  }

  @Override
  public void setDataObjectFactory(final RecordFactory dataObjectFactory) {
    this.dataStore.setDataObjectFactory(dataObjectFactory);
  }

  @Override
  public void setGeometryFactory(final GeometryFactory geometryFactory) {
    this.dataStore.setGeometryFactory(geometryFactory);
  }

  @Override
  public void setLabel(final String label) {
    this.dataStore.setLabel(label);
  }

  @Override
  public void setProperties(final Map<String, ? extends Object> properties) {
    this.dataStore.setProperties(properties);
  }

  @Override
  public void setProperty(final String name, final Object value) {
    this.dataStore.setProperty(name, value);
  }

  @Override
  public void setPropertySoft(final String name, final Object value) {
    this.dataStore.setPropertySoft(name, value);
  }

  @Override
  public void setPropertyWeak(final String name, final Object value) {
    this.dataStore.setPropertyWeak(name, value);
  }

  @Override
  public void setSchemaMap(final Map<String, RecordStoreSchema> schemaMap) {
    this.dataStore.setSchemaMap(schemaMap);
  }

  @Override
  public void setTypeMetaDataProperties(
    final Map<String, List<RecordDefinitionProperty>> typeMetaProperties) {
    this.dataStore.setTypeMetaDataProperties(typeMetaProperties);
  }

  @Override
  public String toString() {
    return this.dataStore.toString();
  }

  @Override
  public void update(final Record object) {
    this.dataStore.update(object);
  }

  @Override
  public void updateAll(final Collection<Record> objects) {
    this.dataStore.updateAll(objects);
  }
}
