package com.revolsys.gis.data.model;

import com.revolsys.data.record.schema.RecordDefinition;

public interface DataObjectMetaDataProperty extends Cloneable {

  DataObjectMetaDataProperty clone();

  RecordDefinition getMetaData();

  String getPropertyName();

  void setMetaData(RecordDefinition metaData);
}
