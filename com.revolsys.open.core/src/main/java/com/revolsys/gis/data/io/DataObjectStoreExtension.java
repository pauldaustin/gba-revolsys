package com.revolsys.gis.data.io;

import java.util.Map;

import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.data.record.schema.RecordStoreSchema;

public interface DataObjectStoreExtension {

  public abstract void initialize(RecordStore dataStore, Map<String, Object> connectionProperties);

  boolean isEnabled(RecordStore dataStore);

  public abstract void postProcess(RecordStoreSchema schema);

  public abstract void preProcess(RecordStoreSchema schema);
}
