/*
 * $URL $
 * $Author:paul.austin@revolsys.com $
 * $Date:2007-06-09 09:28:28 -0700 (Sat, 09 Jun 2007) $
 * $Revision:265 $

 * Copyright 2004-2005 Revolution Systems Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.revolsys.format.shp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.springframework.core.io.Resource;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.types.DataType;
import com.revolsys.data.types.DataTypes;
import com.revolsys.format.xbase.FieldDefinition;
import com.revolsys.format.xbase.XbaseDataObjectWriter;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.CoordinateSystems;
import com.revolsys.gis.cs.esri.EsriCsWktWriter;
import com.revolsys.gis.cs.projection.GeometryProjectionUtil;
import com.revolsys.gis.io.EndianOutput;
import com.revolsys.gis.io.ResourceEndianOutput;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoConstants;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.spring.NonExistingResource;
import com.revolsys.spring.SpringUtil;
import com.revolsys.util.MathUtil;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class ShapefileDataObjectWriter extends XbaseDataObjectWriter {
  private static final Logger LOG = Logger.getLogger(ShapefileDataObjectWriter.class);

  private static final ShapefileGeometryUtil SHP_WRITER = ShapefileGeometryUtil.SHP_INSTANCE;

  private final Envelope envelope = new Envelope();

  private GeometryFactory geometryFactory;

  private String geometryPropertyName = "geometry";

  private Method geometryWriteMethod;

  private boolean hasGeometry = false;

  private ResourceEndianOutput indexOut;

  private ResourceEndianOutput out;

  private int recordNumber = 1;

  private final Resource resource;

  private final double zMax = 0; // Double.MIN_VALUE;

  private final double zMin = 0; // Double.MAX_VALUE;

  private int shapeType = ShapefileConstants.NULL_SHAPE;

  public ShapefileDataObjectWriter(final RecordDefinition metaData, final Resource resource) {
    super(metaData, SpringUtil.getResourceWithExtension(resource, "dbf"));
    this.resource = resource;
  }

  @Override
  protected int addDbaseField(final String name, final DataType dataType,
    final Class<?> typeJavaClass, final int length, final int scale) {
    if (Geometry.class.isAssignableFrom(typeJavaClass)) {
      if (this.hasGeometry) {
        return super.addDbaseField(name, DataTypes.STRING, String.class, 254, 0);
      } else {
        this.hasGeometry = true;
        addFieldDefinition(name, FieldDefinition.OBJECT_TYPE, 0);
        return 0;
      }
    } else {
      return super.addDbaseField(name, dataType, typeJavaClass, length, scale);
    }
  }

  @Override
  public void close() {
    super.close();
    try {
      updateHeader(this.out);
      if (this.indexOut != null) {
        updateHeader(this.indexOut);
      }
    } catch (final IOException e) {
      LOG.error(e.getMessage(), e);
    } finally {
      this.out = null;
      this.indexOut = null;
    }
  }

  private void createGeometryWriter(final Geometry geometry) {
    this.geometryWriteMethod = ShapefileGeometryUtil.getWriteMethod(geometry);
    this.shapeType = ShapefileGeometryUtil.getShapeType(geometry);
  }

  private void createPrjFile(final GeometryFactory geometryFactory) throws IOException {
    if (geometryFactory != null) {
      final CoordinateSystem coordinateSystem = geometryFactory.getCoordinateSystem();
      if (coordinateSystem != null) {
        final int srid = coordinateSystem.getId();
        final Resource prjResource = SpringUtil.getResourceWithExtension(this.resource, "prj");
        if (!(prjResource instanceof NonExistingResource)) {
          final OutputStream out = SpringUtil.getOutputStream(prjResource);
          final PrintWriter writer = new PrintWriter(FileUtil.createUtf8Writer(out));
          final CoordinateSystem esriCoordinateSystem = CoordinateSystems.getCoordinateSystem(new QName(
            "ESRI", String.valueOf(srid)));
          EsriCsWktWriter.write(writer, esriCoordinateSystem, -1);
          writer.close();
        }
      }
    }
  }

  @Override
  protected void init() throws IOException {
    super.init();
    final RecordDefinitionImpl metaData = (RecordDefinitionImpl)getMetaData();
    if (metaData != null) {
      this.geometryPropertyName = metaData.getGeometryFieldName();
      if (this.geometryPropertyName != null) {

        this.out = new ResourceEndianOutput(this.resource);
        writeHeader(this.out);

        if (!hasField(this.geometryPropertyName)) {
          addFieldDefinition(this.geometryPropertyName, FieldDefinition.OBJECT_TYPE, 0);
        }

        final Resource indexResource = SpringUtil.getResourceWithExtension(this.resource, "shx");
        if (!(indexResource instanceof NonExistingResource)) {
          this.indexOut = new ResourceEndianOutput(indexResource);
          writeHeader(this.indexOut);
        }
        this.geometryFactory = getProperty(IoConstants.GEOMETRY_FACTORY);
      }
    }
  }

  @Override
  protected void preFirstWrite(final Record object) throws IOException {
    if (this.geometryPropertyName != null) {
      if (this.geometryFactory == null) {
        final Geometry geometry = object.getGeometryValue();
        if (geometry != null) {
          this.geometryFactory = GeometryFactory.getFactory(geometry);
        }
      }
      createPrjFile(this.geometryFactory);
    }
  }

  @Override
  public String toString() {
    return "ShapefileWriter(" + this.resource + ")";
  }

  private void updateHeader(final ResourceEndianOutput out) throws IOException {
    if (out != null) {

      out.seek(24);
      final int sizeInShorts = (int)(out.length() / 2);
      out.writeInt(sizeInShorts);
      out.seek(32);
      out.writeLEInt(this.shapeType);
      out.writeLEDouble(this.envelope.getMinX());
      out.writeLEDouble(this.envelope.getMinY());
      out.writeLEDouble(this.envelope.getMaxX());
      out.writeLEDouble(this.envelope.getMaxY());
      switch (this.shapeType) {
        case ShapefileConstants.POINT_ZM_SHAPE:
        case ShapefileConstants.MULTI_POINT_ZM_SHAPE:
        case ShapefileConstants.POLYLINE_ZM_SHAPE:
        case ShapefileConstants.POLYGON_ZM_SHAPE:
          out.writeLEDouble(this.zMin);
          out.writeLEDouble(this.zMax);
        break;

        default:
          out.writeLEDouble(0.0);
          out.writeLEDouble(0.0);
        break;
      }
      out.writeLEDouble(0.0);
      out.writeLEDouble(0.0);
      out.close();
    }
  }

  @Override
  protected boolean writeField(final Record object, final FieldDefinition field) throws IOException {
    if (field.getFullName().equals(this.geometryPropertyName)) {
      final long recordIndex = this.out.getFilePointer();
      Geometry geometry = object.getGeometryValue();
      geometry = GeometryProjectionUtil.performCopy(geometry, this.geometryFactory);
      this.envelope.expandToInclude(geometry.getEnvelopeInternal());
      if (geometry == null || geometry.isEmpty()) {
        writeNull(this.out);
      } else {
        if (this.geometryWriteMethod == null) {
          createGeometryWriter(geometry);
        }
        this.out.writeInt(this.recordNumber);
        SHP_WRITER.write(this.geometryWriteMethod, this.out, geometry);

        this.recordNumber++;
        if (this.indexOut != null) {
          final long recordLength = this.out.getFilePointer() - recordIndex;
          final int offsetShort = (int)(recordIndex / MathUtil.BYTES_IN_SHORT);
          this.indexOut.writeInt(offsetShort);
          final int lengthShort = (int)(recordLength / MathUtil.BYTES_IN_SHORT) - 4;
          this.indexOut.writeInt(lengthShort);
        }
      }
      return true;
    } else {
      return super.writeField(object, field);
    }
  }

  private void writeHeader(final EndianOutput out) throws IOException {
    out.writeInt(ShapefileConstants.FILE_CODE);
    for (int i = 0; i < 5; i++) { // Unused
      out.writeInt(0);
    }
    out.writeInt(0); // File length updated on close
    out.writeLEInt(ShapefileConstants.VERSION);
    out.writeLEInt(0); // Shape Type updated on close
    // shape type and bounding box will be updated on file close
    for (int i = 0; i < 8; i++) {
      out.writeLEDouble(0);
    }
  }

  private int writeNull(final EndianOutput out) throws IOException {
    final int recordLength = MathUtil.BYTES_IN_INT;
    out.writeInt(recordLength);
    out.writeLEInt(ShapefileConstants.NULL_SHAPE);
    return ShapefileConstants.NULL_SHAPE;
  }
}
