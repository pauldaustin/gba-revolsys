package com.revolsys.format.moep;

import java.util.HashMap;
import java.util.Map;

import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionFactory;
import com.revolsys.properties.BaseObjectWithProperties;

public class MoepRecordDefinitionFactory extends BaseObjectWithProperties
  implements RecordDefinitionFactory {
  private static final Map<String, RecordDefinition> META_DATA_CACHE = new HashMap<>();

  @Override
  public RecordDefinition getRecordDefinition(final String typePath) {
    synchronized (META_DATA_CACHE) {
      RecordDefinition recordDefinition = META_DATA_CACHE.get(typePath);
      if (recordDefinition == null) {
        recordDefinition = MoepConstants.createRecordDefinition(typePath);
        META_DATA_CACHE.put(typePath, recordDefinition);
      }
      return recordDefinition;
    }
  }

}
