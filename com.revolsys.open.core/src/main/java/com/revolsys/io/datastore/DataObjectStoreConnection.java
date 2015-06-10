package com.revolsys.io.datastore;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.revolsys.collection.map.Maps;
import com.revolsys.data.record.io.RecordStoreFactoryRegistry;
import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.data.record.schema.RecordStoreSchema;
import com.revolsys.io.FileUtil;
import com.revolsys.io.map.MapSerializer;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.Property;

public class DataObjectStoreConnection implements MapSerializer {
  private Map<String, Object> config;

  private String name;

  private RecordStore dataStore;

  private DataObjectStoreConnectionRegistry registry;

  public DataObjectStoreConnection(final DataObjectStoreConnectionRegistry registry,
    final String resourceName, final Map<String, ? extends Object> config) {
    this.registry = registry;
    this.config = new LinkedHashMap<String, Object>(config);
    this.name = Maps.getString(config, "name");
    if (!Property.hasValue(this.name)) {
      this.name = FileUtil.getBaseName(resourceName);
    }
  }

  public DataObjectStoreConnection(final DataObjectStoreConnectionRegistry registry,
    final String name, final RecordStore dataStore) {
    this.registry = registry;
    this.name = name;
    this.dataStore = dataStore;
  }

  public void delete() {
    if (this.registry != null) {
      this.registry.removeConnection(this);
    }
    this.config = null;
    this.dataStore = null;
    this.name = null;
    this.registry = null;

  }

  public RecordStore getDataStore() {
    synchronized (this) {
      if (this.dataStore == null) {
        try {
          final Map<String, Object> connectionProperties = CollectionUtil.get(this.config,
            "connection", Collections.<String, Object> emptyMap());
          if (connectionProperties.isEmpty()) {
            LoggerFactory.getLogger(getClass()).error(
              "Data store must include a 'connection' map property: " + this.name);
          } else {
            this.dataStore = RecordStoreFactoryRegistry.createDataObjectStore(connectionProperties);
            this.dataStore.initialize();
          }
        } catch (final Throwable e) {
          LoggerFactory.getLogger(getClass()).error("Error creating data store for: " + this.name,
            e);
        }
      }
    }
    return this.dataStore;
  }

  public String getName() {
    return this.name;
  }

  public List<RecordStoreSchema> getSchemas() {
    final RecordStore dataStore = getDataStore();
    if (dataStore == null) {
      return Collections.emptyList();
    } else {
      return dataStore.getSchemas();
    }
  }

  public boolean isReadOnly() {
    if (this.registry == null) {
      return true;
    } else {
      return this.registry.isReadOnly();
    }
  }

  @Override
  public Map<String, Object> toMap() {
    return this.config;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
