package com.revolsys.gis.data.io;

import java.util.List;
import java.util.Map;

import com.revolsys.data.record.schema.RecordStore;

public interface RecordStoreFactory {
  RecordStore createRecordStore(
    Map<String, ? extends Object> connectionProperties);

  Class<? extends RecordStore> getRecordStoreInterfaceClass(
    Map<String, ? extends Object> connectionProperties);

  List<String> getFileExtensions();

  String getName();

  List<String> getUrlPatterns();
}
