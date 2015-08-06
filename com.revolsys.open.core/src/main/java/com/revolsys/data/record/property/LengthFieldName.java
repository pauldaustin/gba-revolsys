package com.revolsys.data.record.property;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.util.Property;
import com.vividsolutions.jts.geom.LineString;

public class LengthFieldName extends AbstractRecordDefinitionProperty {
  public static final String PROPERTY_NAME = LengthFieldName.class.getName() + ".propertyName";

  public static LengthFieldName getProperty(final Record object) {
    final RecordDefinition metaData = object.getRecordDefinition();
    return getProperty(metaData);
  }

  public static LengthFieldName getProperty(final RecordDefinition metaData) {
    LengthFieldName property = metaData.getProperty(PROPERTY_NAME);
    if (property == null) {
      property = new LengthFieldName();
      property.setRecordDefinition(metaData);
    }
    return property;
  }

  public static void setObjectLength(final Record object) {
    final LengthFieldName property = getProperty(object);
    property.setLength(object);
  }

  private String attributeName;

  public LengthFieldName() {
  }

  public LengthFieldName(final String attributeName) {
    this.attributeName = attributeName;
  }

  public String getAttributeName() {
    return this.attributeName;
  }

  @Override
  public String getPropertyName() {
    return PROPERTY_NAME;
  }

  public void setFieldName(final String attributeName) {
    this.attributeName = attributeName;
  }

  public void setLength(final Record object) {
    if (Property.hasValue(this.attributeName)) {
      final LineString line = object.getGeometry();
      final double length = line.getLength();
      object.setValue(this.attributeName, length);
    }
  }

  @Override
  public String toString() {
    return "LengthAttribute " + this.attributeName;
  }
}
