package com.revolsys.format.wkt;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.springframework.core.io.Resource;

import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoConstants;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.record.Record;
import com.revolsys.record.RecordFactory;
import com.revolsys.record.Records;
import com.revolsys.record.io.RecordIterator;
import com.revolsys.record.property.FieldProperties;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;
import com.vividsolutions.jts.geom.Geometry;

public class WktRecordIterator extends AbstractIterator<Record>implements RecordIterator {

  private RecordFactory factory;

  private BufferedReader in;

  private RecordDefinition recordDefinition;

  private WktParserOld wktParserOld;

  public WktRecordIterator(final RecordFactory factory, final Resource resource)
    throws IOException {
    this.factory = factory;
    this.in = new BufferedReader(FileUtil.createUtf8Reader(resource.getInputStream()));
    this.recordDefinition = Records.createGeometryRecordDefinition();
  }

  @Override
  protected void doClose() {
    FileUtil.closeSilent(this.in);
    this.factory = null;
    this.in = null;
    this.wktParserOld = null;
    this.recordDefinition = null;
  }

  @Override
  protected void doInit() {
    GeometryFactory geometryFactory;
    final FieldDefinition geometryAttribute = this.recordDefinition.getGeometryField();
    if (geometryAttribute == null) {
      geometryFactory = GeometryFactory.getFactory();
    } else {
      geometryFactory = geometryAttribute.getProperty(FieldProperties.GEOMETRY_FACTORY);
      if (geometryFactory == null) {
        geometryFactory = getProperty(IoConstants.GEOMETRY_FACTORY);
        if (geometryFactory == null) {
          geometryFactory = GeometryFactory.getFactory();
        }
        geometryAttribute.setProperty(FieldProperties.GEOMETRY_FACTORY, geometryFactory);
      }
    }
    this.wktParserOld = new WktParserOld(geometryFactory);
  }

  @Override
  protected Record getNext() {
    try {
      final String wkt = this.in.readLine();
      final Geometry geometry = this.wktParserOld.parseGeometry(wkt);
      if (geometry == null) {
        throw new NoSuchElementException();
      } else {
        final Record object = this.factory.createRecord(getRecordDefinition());
        object.setGeometryValue(geometry);
        return object;
      }
    } catch (final IOException e) {
      throw new RuntimeException("Error reading geometry ", e);
    }

  }

  @Override
  public RecordDefinition getRecordDefinition() {
    return this.recordDefinition;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
