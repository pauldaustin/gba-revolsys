package com.revolsys.gis.data.model;

import com.revolsys.io.ObjectWithProperties;

public interface DataObjectMetaDataFactory extends ObjectWithProperties {
  RecordDefinition getMetaData(String path);

}
