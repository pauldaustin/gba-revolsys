package com.revolsys.gis.esri.gdb.file;

import com.revolsys.gis.data.io.RecordStore;
import com.revolsys.gis.data.model.RecordDefinition;

public interface FileGdbRecordStore extends RecordStore {

  void deleteGeodatabase();

  @Override
  RecordDefinition getMetaData(RecordDefinition metaData);

  @Override
  void initialize();

  void setCreateMissingDataStore(boolean createMissingDataStore);

  void setCreateMissingTables(boolean createMissingTables);

  void setDefaultSchema(String string);

}
