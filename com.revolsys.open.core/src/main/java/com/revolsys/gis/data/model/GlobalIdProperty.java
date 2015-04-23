package com.revolsys.gis.data.model;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;

public class GlobalIdProperty extends AbstractDataObjectMetaDataProperty {
  static final String PROPERTY_NAME = "http://revolsys.com/gis/globalId";

  public static GlobalIdProperty getProperty(final Record object) {
    final RecordDefinition metaData = object.getRecordDefinition();
    return getProperty(metaData);
  }

  public static GlobalIdProperty getProperty(final RecordDefinition metaData) {
    if (metaData == null) {
      return null;
    } else {
      return metaData.getProperty(PROPERTY_NAME);
    }
  }

  private String attributeName;

  public GlobalIdProperty() {
  }

  public GlobalIdProperty(final String attributeName) {
    this.attributeName = attributeName;
  }

  @Override
  public GlobalIdProperty clone() {
    return (GlobalIdProperty)super.clone();
  }

  public String getAttributeName() {
    return attributeName;
  }

  @Override
  public String getPropertyName() {
    return PROPERTY_NAME;
  }

  public void setAttributeName(final String attributeName) {
    this.attributeName = attributeName;
  }

  @Override
  public void setRecordDefinition(final RecordDefinition metaData) {
    if (attributeName == null) {
      attributeName = metaData.getIdFieldName();
    }
    super.setRecordDefinition(metaData);
  }

}
