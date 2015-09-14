package com.revolsys.gis.graph.attribute;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.revolsys.equals.RecordEquals;
import com.revolsys.geometry.graph.attribute.InvokeMethodObjectPropertyProxy;
import com.revolsys.gis.graph.Node;
import com.revolsys.properties.ObjectPropertyProxy;
import com.revolsys.record.Record;
import com.revolsys.record.property.AbstractRecordDefinitionProperty;
import com.revolsys.record.schema.RecordDefinition;

public class PseudoNodeProperty extends AbstractRecordDefinitionProperty {
  protected static final List<String> DEFAULT_EXCLUDE = Arrays.asList(RecordEquals.EXCLUDE_ID,
    RecordEquals.EXCLUDE_GEOMETRY);

  public static final String PROPERTY_NAME = PseudoNodeProperty.class.getName() + ".propertyName";

  public static AbstractRecordDefinitionProperty getProperty(final Record object) {
    final RecordDefinition recordDefinition = object.getRecordDefinition();
    return getProperty(recordDefinition);
  }

  public static PseudoNodeProperty getProperty(final RecordDefinition recordDefinition) {
    PseudoNodeProperty property = recordDefinition.getProperty(PROPERTY_NAME);
    if (property == null) {
      property = new PseudoNodeProperty();
      property.setRecordDefinition(recordDefinition);
    }
    return property;
  }

  private Set<String> equalExcludeAttributes = new HashSet<String>(DEFAULT_EXCLUDE);

  public PseudoNodeProperty() {
  }

  public PseudoNodeAttribute createAttribute(final Node<Record> node) {
    return new PseudoNodeAttribute(node, getTypePath(), this.equalExcludeAttributes);
  }

  public PseudoNodeAttribute getAttribute(final Node<Record> node) {
    final String fieldName = PseudoNodeProperty.PROPERTY_NAME;
    if (!node.hasAttribute(fieldName)) {
      final ObjectAttributeProxy<PseudoNodeAttribute, Node<Record>> proxy = new InvokeMethodObjectAttributeProxy<PseudoNodeAttribute, Node<Record>>(
        this, "createAttribute", Node.class);
      node.setAttribute(fieldName, proxy);
    }
    final PseudoNodeAttribute value = node.getAttribute(fieldName);
    return value;
  }

  public Collection<String> getEqualExcludeAttributes() {
    return this.equalExcludeAttributes;
  }

  public PseudoNodeAttribute getProperty(final com.revolsys.geometry.graph.Node<Record> node) {
    final String fieldName = PseudoNodeProperty.PROPERTY_NAME;
    if (!node.hasProperty(fieldName)) {
      final ObjectPropertyProxy<PseudoNodeAttribute, Node<Record>> proxy = new InvokeMethodObjectPropertyProxy<>(
        this, "createProperty", Node.class);
      node.setProperty(fieldName, proxy);
    }
    final PseudoNodeAttribute value = node.getProperty(fieldName);
    return value;
  }

  @Override
  public String getPropertyName() {
    return PROPERTY_NAME;
  }

  public void setEqualExcludeAttributes(final Collection<String> equalExcludeAttributes) {
    if (equalExcludeAttributes == null) {
      this.equalExcludeAttributes.clear();
    } else {
      this.equalExcludeAttributes = new HashSet<String>(equalExcludeAttributes);
    }
    this.equalExcludeAttributes.addAll(DEFAULT_EXCLUDE);
  }

  @Override
  public void setRecordDefinition(final RecordDefinition recordDefinition) {
    super.setRecordDefinition(recordDefinition);
  }

  @Override
  public String toString() {
    return "Pseudo Node";
  }
}
