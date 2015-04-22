package com.revolsys.gis.data.io;

import com.revolsys.data.record.schema.RecordStore;

public interface DataObjectStoreProxy {
  <V extends RecordStore> V getDataStore();
}
