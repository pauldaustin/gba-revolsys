package com.revolsys.data.record.property;

import com.revolsys.data.record.schema.RecordDefinition;

public interface RecordDefinitionProperty extends Cloneable {

  RecordDefinitionProperty clone();

  RecordDefinition getRecordDefinition();

  String getPropertyName();

  void setRecordDefinition(RecordDefinition metaData);
}
