package com.revolsys.gis.data.model;

import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.io.ObjectWithProperties;

public interface DataObjectMetaDataFactory extends ObjectWithProperties {
  RecordDefinition getRecordDefinition(String path);

}
