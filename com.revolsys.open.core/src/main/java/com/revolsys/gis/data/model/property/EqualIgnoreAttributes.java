package com.revolsys.gis.data.model.property;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.data.model.AbstractDataObjectMetaDataProperty;
import com.revolsys.gis.model.data.equals.DataObjectEquals;

public class EqualIgnoreAttributes extends AbstractDataObjectMetaDataProperty {
  public static final String PROPERTY_NAME = EqualIgnoreAttributes.class.getName()
    + ".propertyName";

  public static EqualIgnoreAttributes getProperty(final Record object) {
    final RecordDefinition metaData = object.getRecordDefinition();
    return getProperty(metaData);
  }

  public static EqualIgnoreAttributes getProperty(
    final RecordDefinition metaData) {
    EqualIgnoreAttributes property = metaData.getProperty(PROPERTY_NAME);
    if (property == null) {
      property = new EqualIgnoreAttributes();
      property.setRecordDefinition(metaData);
    }
    return property;
  }

  private Set<String> attributeNames = new LinkedHashSet<String>();

  public EqualIgnoreAttributes() {
  }

  public EqualIgnoreAttributes(final Collection<String> attributeNames) {
    this.attributeNames.addAll(attributeNames);
  }

  public EqualIgnoreAttributes(final String... attributeNames) {
    this(Arrays.asList(attributeNames));
  }

  public void addAttributeNames(final Collection<String> attributeNames) {
    this.attributeNames.addAll(attributeNames);
  }

  public void addAttributeNames(final String... attributeNames) {
    addAttributeNames(Arrays.asList(attributeNames));
  }

  public Set<String> getAttributeNames() {
    return attributeNames;
  }

  @Override
  public String getPropertyName() {
    return PROPERTY_NAME;
  }

  public boolean isAttributeIgnored(final String attributeName) {
    return attributeNames.contains(attributeName);
  }

  public void setAttributeNames(final Collection<String> attributeNames) {
    setAttributeNames(new LinkedHashSet<String>(attributeNames));
  }

  public void setAttributeNames(final Set<String> attributeNames) {
    this.attributeNames = attributeNames;
  }

  public void setFieldNames(final String... attributeNames) {
    setAttributeNames(Arrays.asList(attributeNames));
  }

  @Override
  public void setRecordDefinition(final RecordDefinition metaData) {
    super.setRecordDefinition(metaData);
    if (attributeNames.contains(DataObjectEquals.EXCLUDE_ID)) {
      final String idAttributeName = metaData.getIdFieldName();
      attributeNames.add(idAttributeName);
    }
    if (attributeNames.contains(DataObjectEquals.EXCLUDE_GEOMETRY)) {
      final String geometryAttributeName = metaData.getGeometryFieldName();
      attributeNames.add(geometryAttributeName);
    }
  }

  @Override
  public String toString() {
    return "EqualIgnore " + attributeNames;
  }
}
