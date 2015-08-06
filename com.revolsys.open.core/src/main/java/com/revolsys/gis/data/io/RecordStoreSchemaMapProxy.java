package com.revolsys.gis.data.io;

import java.util.Map;
import java.util.TreeMap;

import com.revolsys.data.record.schema.AbstractRecordStore;
import com.revolsys.data.record.schema.RecordStoreSchema;

public class RecordStoreSchemaMapProxy extends TreeMap<String, RecordStoreSchema> {

  /**
   *
   */
  private static final long serialVersionUID = -1711922998363200190L;

  private final Map<String, RecordStoreSchema> map;

  private final AbstractRecordStore recordStore;

  public RecordStoreSchemaMapProxy(final AbstractRecordStore recordStore,
    final Map<String, RecordStoreSchema> map) {
    this.recordStore = recordStore;
    this.map = map;
  }

  @Override
  public RecordStoreSchema get(final Object key) {
    RecordStoreSchema schema = super.get(key);
    if (schema == null) {
      schema = this.map.get(key);
      if (schema != null) {
        final String path = schema.getPath();
        schema = new RecordStoreSchemaProxy(this.recordStore, path, schema);
        super.put(path, schema);
      }
    }
    return schema;
  }

  @Override
  public RecordStoreSchema put(final String key, final RecordStoreSchema schema) {
    final RecordStoreSchemaProxy schemaProxy = new RecordStoreSchemaProxy(
      this.recordStore, key, schema);
    this.map.put(key, schema);
    return super.put(key, schemaProxy);
  }
}
