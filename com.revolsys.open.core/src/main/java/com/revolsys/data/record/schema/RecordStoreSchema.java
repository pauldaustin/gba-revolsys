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

  private Map<String, RecordDefinition> recordDefinitionCache = new TreeMap<String, RecordDefinition>();

  private String path;

  public RecordStoreSchema() {
  }

  public RecordStoreSchema(final AbstractRecordStore recordStore, final String path) {
    this.recordStore = new WeakReference<AbstractRecordStore>(recordStore);
    this.path = path;
  }

  public void addRecordDefinition(final RecordDefinition metaData) {
    addRecordDefinition(metaData.getPath(), metaData);
  }

  protected void addRecordDefinition(final String typePath, final RecordDefinition metaData) {
    this.recordDefinitionCache.put(typePath.toUpperCase(), metaData);
  }

  @Override
  @PreDestroy
  public void close() {
    if (this.recordDefinitionCache != null) {
      for (final RecordDefinition metaData : this.recordDefinitionCache.values()) {
        metaData.destroy();
      }
      this.recordDefinitionCache.clear();
    }
    this.recordStore = null;
    this.recordDefinitionCache = null;
    this.path = null;
    super.close();
  }

  public synchronized RecordDefinition findRecordDefinition(final String typePath) {
    final RecordDefinition metaData = this.recordDefinitionCache.get(typePath);
    return metaData;
  }

  public GeometryFactory getGeometryFactory() {
    final GeometryFactory geometryFactory = getProperty("geometryFactory");
    if (geometryFactory == null) {
      final AbstractRecordStore recordStore = getRecordStore();
      if (recordStore == null) {
        return GeometryFactory.getFactory();
      } else {
        return recordStore.getGeometryFactory();
      }
    } else {
      return geometryFactory;
    }
  }

  protected Map<String, RecordDefinition> getRecordDefinitionCache() {
    return this.recordDefinitionCache;
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
      if (this.recordDefinitionCache.isEmpty()) {
        refreshMetaData();
      }
      final RecordDefinition metaData = this.recordDefinitionCache.get(typePath.toUpperCase());
      return metaData;
    } else {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  public <V extends RecordStore> V getRecordStore() {
    if (this.recordStore == null) {
      return null;
    } else {
      return (V)this.recordStore.get();
    }
  }

  public List<String> getTypeNames() {
    if (this.recordDefinitionCache.isEmpty()) {
      refreshMetaData();
    }
    return new ArrayList<String>(this.recordDefinitionCache.keySet());
  }

  public List<RecordDefinition> getTypes() {
    if (this.recordDefinitionCache.isEmpty()) {
      refreshMetaData();
    }
    return new ArrayList<RecordDefinition>(this.recordDefinitionCache.values());
  }

  public void refreshMetaData() {
    final AbstractRecordStore recordStore = getRecordStore();
    if (recordStore != null) {
      final Collection<DataObjectStoreExtension> extensions = recordStore.getRecordStoreExtensions();
      for (final DataObjectStoreExtension extension : extensions) {
        try {
          if (extension.isEnabled(recordStore)) {
            extension.preProcess(this);
          }
        } catch (final Throwable e) {
          ExceptionUtil.log(extension.getClass(), "Unable to pre-process schema " + this, e);
        }
      }
      recordStore.loadSchemaDataObjectMetaData(this, this.recordDefinitionCache);
      for (final DataObjectStoreExtension extension : extensions) {
        try {
          if (extension.isEnabled(recordStore)) {
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
