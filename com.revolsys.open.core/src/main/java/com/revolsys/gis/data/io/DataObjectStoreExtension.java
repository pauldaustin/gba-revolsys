package com.revolsys.gis.data.io;

import java.util.Map;

public interface DataObjectStoreExtension {

  public abstract void initialize(RecordStore dataStore,
    Map<String, Object> connectionProperties);

  boolean isEnabled(RecordStore dataStore);

  public abstract void postProcess(DataObjectStoreSchema schema);

  public abstract void preProcess(DataObjectStoreSchema schema);
}
