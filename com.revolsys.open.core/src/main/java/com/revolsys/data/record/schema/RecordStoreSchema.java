package com.revolsys.data.record.schema;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PreDestroy;

import com.revolsys.gis.data.io.DataObjectStoreExtension;
import com.revolsys.io.AbstractObjectWithProperties;
import com.revolsys.io.Path;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.util.ExceptionUtil;

public class RecordStoreSchema extends AbstractObjectWithProperties {

  private Reference<AbstractRecordStore> recordStore;

  private Map<String, RecordDefinition> metaDataCache = new TreeMap<String, RecordDefinition>();

  private String path;

  public RecordStoreSchema() {
  }

  public RecordStoreSchema(final AbstractRecordStore dataStore, final String path) {
    this.recordStore = new WeakReference<AbstractRecordStore>(dataStore);
    this.path = path;
  }

  public void addMetaData(final RecordDefinition metaData) {
    addMetaData(metaData.getPath(), metaData);
  }

  protected void addMetaData(final String typePath, final RecordDefinition metaData) {
    this.metaDataCache.put(typePath.toUpperCase(), metaData);
  }

  @Override
  @PreDestroy
  public void close() {
    if (this.metaDataCache != null) {
      for (final RecordDefinition metaData : this.metaDataCache.values()) {
        metaData.destroy();
      }
      this.metaDataCache.clear();
    }
    this.recordStore = null;
    this.metaDataCache = null;
    this.path = null;
    super.close();
  }

  public synchronized RecordDefinition findMetaData(final String typePath) {
    final RecordDefinition metaData = this.metaDataCache.get(typePath);
    return metaData;
  }

  @SuppressWarnings("unchecked")
  public <V extends RecordStore> V getRecordStore() {
    if (this.recordStore == null) {
      return null;
    } else {
      return (V)this.recordStore.get();
    }
  }

  public GeometryFactory getGeometryFactory() {
    final GeometryFactory geometryFactory = getProperty("geometryFactory");
    if (geometryFactory == null) {
      final AbstractRecordStore dataStore = getRecordStore();
      if (dataStore == null) {
        return GeometryFactory.getFactory();
      } else {
        return dataStore.getGeometryFactory();
      }
    } else {
      return geometryFactory;
    }
  }

  protected Map<String, RecordDefinition> getMetaDataCache() {
    return this.metaDataCache;
  }

  public String getName() {
    final String path = getPath();
    return Path.getName(path);
  }

  public String getPath() {
    return this.path;
  }

  public synchronized RecordDefinition getRecordDefinition(String typePath) {
    typePath = typePath.toUpperCase();
    if (typePath.startsWith(this.path + "/") || this.path.equals("/")) {
      if (this.metaDataCache.isEmpty()) {
        refreshMetaData();
      }
      final RecordDefinition metaData = this.metaDataCache.get(typePath.toUpperCase());
      return metaData;
    } else {
      return null;
    }
  }

  public List<String> getTypeNames() {
    if (this.metaDataCache.isEmpty()) {
      refreshMetaData();
    }
    return new ArrayList<String>(this.metaDataCache.keySet());
  }

  public List<RecordDefinition> getTypes() {
    if (this.metaDataCache.isEmpty()) {
      refreshMetaData();
    }
    return new ArrayList<RecordDefinition>(this.metaDataCache.values());
  }

  public void refreshMetaData() {
    final AbstractRecordStore dataStore = getRecordStore();
    if (dataStore != null) {
      final Collection<DataObjectStoreExtension> extensions = dataStore.getDataStoreExtensions();
      for (final DataObjectStoreExtension extension : extensions) {
        try {
          if (extension.isEnabled(dataStore)) {
            extension.preProcess(this);
          }
        } catch (final Throwable e) {
          ExceptionUtil.log(extension.getClass(), "Unable to pre-process schema " + this, e);
        }
      }
      dataStore.loadSchemaDataObjectMetaData(this, this.metaDataCache);
      for (final DataObjectStoreExtension extension : extensions) {
        try {
          if (extension.isEnabled(dataStore)) {
            extension.postProcess(this);
          }
        } catch (final Throwable e) {
          ExceptionUtil.log(extension.getClass(), "Unable to post-process schema " + this, e);
        }
      }
    }
  }

  @Override
  public String toString() {
    return this.path;
  }
}
