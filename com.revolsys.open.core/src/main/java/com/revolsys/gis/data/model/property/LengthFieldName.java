package com.revolsys.gis.data.model.property;

import org.springframework.util.StringUtils;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.data.model.AbstractDataObjectMetaDataProperty;
import com.vividsolutions.jts.geom.LineString;

public class LengthFieldName extends AbstractDataObjectMetaDataProperty {
  public static final String PROPERTY_NAME = LengthFieldName.class.getName()
    + ".propertyName";

  public static LengthFieldName getProperty(final Record object) {
    final RecordDefinition metaData = object.getRecordDefinition();
    return getProperty(metaData);
  }

  public static LengthFieldName getProperty(
    final RecordDefinition metaData) {
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
    return attributeName;
  }

  @Override
  public String getPropertyName() {
    return PROPERTY_NAME;
  }

  public void setFieldName(final String attributeName) {
    this.attributeName = attributeName;
  }

  public void setLength(final Record object) {
    if (StringUtils.hasText(attributeName)) {
      final LineString line = object.getGeometryValue();
      final double length = line.getLength();
      object.setValue(attributeName, length);
    }
  }

  @Override
  public String toString() {
    return "LengthAttribute " + attributeName;
  }
}
