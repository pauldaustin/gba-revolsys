package com.revolsys.gis.data.io;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.revolsys.data.record.schema.RecordStore;

public class DataObjectStoreRegistry {
  private Map<String, RecordStore> dataStores = new HashMap<String, RecordStore>();

  public void addDataStore(final String name, final RecordStore dataStore) {
    this.dataStores.put(name, dataStore);
  }

  public RecordStore getDataObjectStore(final String name) {
    return dataStores.get(name);
  }

  public Map<String, RecordStore> getDataStores() {
    return Collections.unmodifiableMap(dataStores);
  }

  public void setDataStores(final Map<String, RecordStore> dataStores) {
    this.dataStores = dataStores;
  }

}
