package com.revolsys.format.wkt;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.springframework.core.io.Resource;

import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.data.record.Records;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.io.RecordIterator;
import com.revolsys.data.record.property.FieldProperties;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoConstants;
import com.revolsys.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Geometry;

public class WktRecordIterator extends AbstractIterator<Record> implements RecordIterator {

  private RecordFactory factory;

  private BufferedReader in;

  private WktParser wktParser;

  private RecordDefinition metaData;

  public WktRecordIterator(final RecordFactory factory, final Resource resource)
    throws IOException {
    this.factory = factory;
    this.in = new BufferedReader(FileUtil.createUtf8Reader(resource.getInputStream()));
    this.metaData = Records.createGeometryMetaData();
  }

  @Override
  protected void doClose() {
    FileUtil.closeSilent(this.in);
    this.factory = null;
    this.in = null;
    this.wktParser = null;
    this.metaData = null;
  }

  @Override
  protected void doInit() {
    GeometryFactory geometryFactory;
    final FieldDefinition geometryAttribute = this.metaData.getGeometryField();
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
    this.wktParser = new WktParser(geometryFactory);
  }

  @Override
  protected Record getNext() {
    try {
      final String wkt = this.in.readLine();
      final Geometry geometry = this.wktParser.parseGeometry(wkt);
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
    return this.metaData;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
