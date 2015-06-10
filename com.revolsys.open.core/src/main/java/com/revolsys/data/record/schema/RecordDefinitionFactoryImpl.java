package com.revolsys.data.record.schema;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.revolsys.io.AbstractObjectWithProperties;

public class RecordDefinitionFactoryImpl extends AbstractObjectWithProperties implements
  RecordDefinitionFactory {

  private final Map<String, RecordDefinition> types = new LinkedHashMap<>();

  public void addMetaData(final RecordDefinition type) {
    if (type != null) {
      this.types.put(type.getPath(), type);
    }
  }

  @Override
  public RecordDefinition getRecordDefinition(final String path) {
    return this.types.get(path);
  }

  public Collection<RecordDefinition> getTypes() {
    return this.types.values();
  }
}
