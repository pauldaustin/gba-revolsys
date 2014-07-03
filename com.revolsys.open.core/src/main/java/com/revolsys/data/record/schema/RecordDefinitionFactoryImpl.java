package com.revolsys.data.record.schema;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.revolsys.io.AbstractObjectWithProperties;

public class RecordDefinitionFactoryImpl extends AbstractObjectWithProperties
implements RecordDefinitionFactory {

  private final Map<String, RecordDefinition> recordDefinitions = new LinkedHashMap<>();

  public void addMetaData(final RecordDefinition recordDefinition) {
    if (recordDefinition != null) {
      final String path = recordDefinition.getPath();
      this.recordDefinitions.put(path, recordDefinition);
    }
  }

  @Override
  public RecordDefinition getRecordDefinition(final String path) {
    return this.recordDefinitions.get(path);
  }

  public Collection<RecordDefinition> getRecordDefinitions() {
    return this.recordDefinitions.values();
  }
}
