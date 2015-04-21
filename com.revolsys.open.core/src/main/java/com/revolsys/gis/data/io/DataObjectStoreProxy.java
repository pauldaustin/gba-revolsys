package com.revolsys.gis.data.io;

public interface DataObjectStoreProxy {
  <V extends RecordStore> V getDataStore();
}
