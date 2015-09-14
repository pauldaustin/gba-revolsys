package com.revolsys.gis.data.io;

import com.revolsys.record.schema.AbstractRecordStore;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordDefinitionImpl;
import com.revolsys.record.schema.RecordStoreSchema;

public class RecordStoreSchemaProxy extends RecordStoreSchema {
  private final RecordStoreSchema schema;

  public RecordStoreSchemaProxy(final AbstractRecordStore recordStore, final String name,
    final RecordStoreSchema schema) {
    super(recordStore, name);
    this.schema = schema;
  }

  @Override
  public synchronized RecordDefinition findRecordDefinition(final String typePath) {
    RecordDefinition recordDefinition = super.findRecordDefinition(typePath);
    if (recordDefinition == null) {
      recordDefinition = this.schema.findRecordDefinition(typePath);
      if (recordDefinition != null) {
        recordDefinition = new RecordDefinitionImpl(this, recordDefinition);
        addElement(recordDefinition);
      }
    }
    return recordDefinition;
  }

  @Override
  public synchronized RecordDefinition getRecordDefinition(final String typePath) {
    RecordDefinition recordDefinition = findRecordDefinition(typePath);
    if (recordDefinition == null) {
      recordDefinition = this.schema.getRecordDefinition(typePath);
      if (recordDefinition != null) {
        recordDefinition = new RecordDefinitionImpl(this, recordDefinition);
        addElement(recordDefinition);
      }
    }
    return recordDefinition;
  }
}
