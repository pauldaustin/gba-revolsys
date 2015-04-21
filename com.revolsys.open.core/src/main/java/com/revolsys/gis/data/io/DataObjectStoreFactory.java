package com.revolsys.gis.data.io;

import java.util.List;
import java.util.Map;

public interface DataObjectStoreFactory {
  RecordStore createDataObjectStore(
    Map<String, ? extends Object> connectionProperties);

  Class<? extends RecordStore> getDataObjectStoreInterfaceClass(
    Map<String, ? extends Object> connectionProperties);

  List<String> getFileExtensions();

  String getName();

  List<String> getUrlPatterns();
}
