package com.revolsys.gis.data.io;

import com.revolsys.data.record.schema.AbstractRecordStore;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.record.schema.RecordStoreSchema;

public class DataObjectStoreSchemaProxy extends RecordStoreSchema {
  private final RecordStoreSchema schema;

  public DataObjectStoreSchemaProxy(final AbstractRecordStore dataObjectStore, final String name,
    final RecordStoreSchema schema) {
    super(dataObjectStore, name);
    this.schema = schema;
  }

  @Override
  public synchronized RecordDefinition findMetaData(final String typePath) {
    RecordDefinition metaData = super.findMetaData(typePath);
    if (metaData == null) {
      metaData = this.schema.findMetaData(typePath);
      if (metaData != null) {
        metaData = new RecordDefinitionImpl(this, metaData);
        addMetaData(typePath, metaData);
      }
    }
    return metaData;
  }

  @Override
  public synchronized RecordDefinition getRecordDefinition(final String typePath) {
    RecordDefinition metaData = findMetaData(typePath);
    if (metaData == null) {
      metaData = this.schema.getRecordDefinition(typePath);
      if (metaData != null) {
        metaData = new RecordDefinitionImpl(this, metaData);
        addMetaData(typePath, metaData);
      }
    }
    return metaData;
  }
}
