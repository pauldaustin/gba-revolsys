package com.revolsys.gis.data.model;

public interface DataObjectMetaDataProperty extends Cloneable {

  DataObjectMetaDataProperty clone();

  RecordDefinition getMetaData();

  String getPropertyName();

  void setMetaData(RecordDefinition metaData);
}
