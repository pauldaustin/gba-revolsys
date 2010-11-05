package com.revolsys.gis.ecsv.io;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.AttributeProperties;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.gis.ecsv.io.type.EcsvFieldType;
import com.revolsys.gis.ecsv.io.type.EcsvFieldTypeRegistry;
import com.revolsys.gis.ecsv.io.type.StringFieldType;
import com.revolsys.io.AbstractWriter;
import com.revolsys.io.IoConstants;

public class EcsvDataObjectWriter extends AbstractWriter<DataObject> implements
  EcsvConstants {

  private final DataObjectMetaData metaData;

  private boolean open;

  private final PrintWriter out;

  public EcsvDataObjectWriter(
    final DataObjectMetaData metaData,
    final java.io.Writer out) {
    this.metaData = metaData;
    this.out = new PrintWriter(new BufferedWriter(out));

  }

  @Override
  public void close() {
    out.print(MULTI_LINE_LIST_END);
    out.close();
  }

  @Override
  public void flush() {
    out.flush();
  }

  private void newLine() {
    out.print(RECORD_SEPARATOR);
  }

  @Override
  public String toString() {
    return metaData.getName().toString();
  }

  public void write(
    final DataObject object) {
    if (!open) {
      open = true;
      writeHeader();
    }
    final int attributeCount = metaData.getAttributeCount();
    for (int i = 0; i < attributeCount; i++) {
      final Object value = object.getValue(i);
      final DataType dataType = metaData.getAttributeType(i);
      writeField(dataType, value);
      if (i < attributeCount - 1) {
        out.print(FIELD_SEPARATOR);
      }
    }
    newLine();
  }

  private void writeAttributeHeaders(
    final DataObjectMetaData metaData) {
    writeMapStart(ATTRIBUTE_PROPERTIES);
    writeMultiLineListStart(EcsvProperties.ATTRIBUTE_NAME, DataTypes.STRING,
      COLLECTION_START);
    final int numAttributes = metaData.getAttributeCount();
    for (int i = 0; i < numAttributes; i++) {
      final String name = metaData.getAttributeName(i);
      writeField(DataTypes.STRING, name);
      if (i < numAttributes - 1) {
        out.print(FIELD_SEPARATOR);
      }
    }
    newLine();
    writeMultiLineEnd(COLLECTION_END);

    writeMultiLineListStart(EcsvProperties.ATTRIBUTE_TYPE, DataTypes.QNAME,
      COLLECTION_START);
    for (int i = 0; i < numAttributes; i++) {
      final DataType type = metaData.getAttributeType(i);
      writeField(DataTypes.QNAME, type.getName());
      if (i < numAttributes - 1) {
        out.print(FIELD_SEPARATOR);
      }
    }
    newLine();
    writeMultiLineEnd(COLLECTION_END);

    writeMultiLineListStart(EcsvProperties.ATTRIBUTE_LENGTH, DataTypes.INT,
      COLLECTION_START);
    for (int i = 0; i < numAttributes; i++) {
      final Integer length = metaData.getAttributeLength(i);
      writeField(DataTypes.INTEGER, length);
      if (i < numAttributes - 1) {
        out.print(FIELD_SEPARATOR);
      }
    }
    newLine();
    writeMultiLineEnd(COLLECTION_END);

    writeMultiLineListStart(EcsvProperties.ATTRIBUTE_SCALE, DataTypes.INT,
      COLLECTION_START);
    for (int i = 0; i < numAttributes; i++) {
      final Integer scale = metaData.getAttributeScale(i);
      writeField(DataTypes.INTEGER, scale);
      if (i < numAttributes - 1) {
        out.print(FIELD_SEPARATOR);
      }
    }
    newLine();
    writeMultiLineEnd(COLLECTION_END);

    writeMultiLineListStart(EcsvProperties.ATTRIBUTE_REQUIRED,
      DataTypes.BOOLEAN, COLLECTION_START);
    for (int i = 0; i < numAttributes; i++) {
      final Boolean required = metaData.isAttributeRequired(i);
      writeField(DataTypes.BOOLEAN, required);
      if (i < numAttributes - 1) {
        out.print(FIELD_SEPARATOR);
      }
    }
    newLine();
    writeMultiLineEnd(COLLECTION_END);

    writeMapEnd();
  }

  private void writeDataStart(
    final DataObjectMetaData metaData) {
    writeMultiLineListStart(RECORDS, metaData.getName(), MULTI_LINE_LIST_START);
  }

  private void writeField(
    final DataType dataType,
    final Object value) {
    if (value != null) {
      final EcsvFieldType fieldType = EcsvFieldTypeRegistry.INSTANCE.getFieldType(dataType);
      if (fieldType == null) {
        StringFieldType.writeQuotedString(out, value);
      } else {
        fieldType.writeValue(out, value);
      }
    }
  }

  private void writeFileProperties() {
    for (final Entry<String, Object> property : getProperties().entrySet()) {
      final String name = property.getKey();
      final Object value = property.getValue();
      final String defaultPrefix = "com.revolsys.io.";
      if (name.startsWith(defaultPrefix)) {
        writeProperty(name.substring(defaultPrefix.length()), value);
      } else if (!name.startsWith("java:")) {
        writeProperty(name, value);
      } else if (name.equals(IoConstants.GEOMETRY_FACTORY)) {
        writeGeometryFactoryProperty((GeometryFactory)value);
      }
    }
  }

  private void writeHeader() {
    writeProperty(ECSV_VERSION, VERSION_1_0_0_DRAFT1);

    writeFileProperties();
    writeTypeDefinition(metaData);
    writeDataStart(metaData);
  }

  private void writeMapEnd() {
    out.write(MAP_END);
    newLine();
  }

  private void writeMapStart(
    final String propertyName) {
    writeMultiLineStart(propertyName, MAP_TYPE, MAP_START);
  }

  private void writeMultiLineEnd(
    final String collectionStart) {
    out.write(collectionStart);
    newLine();
  }

  private void writeMultiLineListStart(
    final String propertyName,
    final DataType dataType,
    final String collectionStart) {
    final QName typeName = dataType.getName();
    writeMultiLineListStart(propertyName, typeName, collectionStart);
  }

  private void writeMultiLineListStart(
    final String propertyName,
    final QName typeName,
    final String collectionStart) {
    final String type = LIST_TYPE + TYPE_PARAMETER_START + typeName
      + TYPE_PARAMETER_END;
    writeMultiLineStart(propertyName, type, collectionStart);
  }

  private void writeMultiLineStart(
    final String propertyName,
    final QName typeName,
    final String collectionStart) {
    writeMultiLineStart(propertyName, typeName.toString(), collectionStart);
  }

  private void writeMultiLineStart(
    final String propertyName,
    final String typeName,
    final String collectionStart) {
    StringFieldType.writeQuotedString(out, propertyName);
    out.print(FIELD_SEPARATOR);
    StringFieldType.writeQuotedString(out, typeName);
    out.print(FIELD_SEPARATOR);
    out.print(collectionStart);
    newLine();
  }

  private void writeProperty(
    final String name,
    final Object value) {
    if (value != null) {
      final DataType dataType = DataTypes.getType(value.getClass());
      EcsvFieldType fieldType = EcsvFieldTypeRegistry.INSTANCE.getFieldType(dataType);
      if (fieldType == null) {
        fieldType = new StringFieldType();
      }
      final QName type = fieldType.getTypeName();
      StringFieldType.writeQuotedString(out, name);
      out.write(FIELD_SEPARATOR);
      writeField(DataTypes.QNAME, type);
      out.write(FIELD_SEPARATOR);
      fieldType.writeValue(out, value);
      newLine();
    }
  }

  private void writeProperty(
    final String name,
    final QName type,
    final Object value) {
    if (value != null) {
      StringFieldType.writeQuotedString(out, name);
      out.write(FIELD_SEPARATOR);
      final DataType dataType = DataTypes.getType(value.getClass());
      EcsvFieldType fieldType = EcsvFieldTypeRegistry.INSTANCE.getFieldType(dataType);
      if (fieldType == null) {
        fieldType = new StringFieldType();
      }
      writeField(DataTypes.QNAME, type);
      out.write(FIELD_SEPARATOR);
      fieldType.writeValue(out, value);
      newLine();
    }
  }

  private void writeTypeDefinition(
    final DataObjectMetaData metaData) {

    writeMapStart(TYPE_DEFINITION);

    writeProperty(EcsvProperties.NAME, metaData.getName());
    final Attribute geometryAttribute = metaData.getGeometryAttribute();
    if (geometryAttribute != null) {
      final GeometryFactory geometryFactory = geometryAttribute.getProperty(AttributeProperties.GEOMETRY_FACTORY);
      writeGeometryFactoryProperty(geometryFactory);
    }

    writeAttributeHeaders(metaData);

    writeMapEnd();
  }

  private void writeGeometryFactoryProperty(
    final GeometryFactory geometryFactory) {
    if (geometryFactory != null) {
      final int srid = geometryFactory.getSRID();
      final double scaleXY = geometryFactory.getScaleXY();
      final double scaleZ = geometryFactory.getScaleZ();
      writeProperty(EcsvProperties.GEOMETRY_FACTORY_PROPERTY,
        GEOMETRY_FACTORY_TYPE, Arrays.asList(srid, scaleXY, scaleZ));
    }
  }

}
