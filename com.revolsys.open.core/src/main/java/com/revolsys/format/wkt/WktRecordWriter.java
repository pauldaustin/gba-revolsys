package com.revolsys.format.wkt;

import java.io.BufferedWriter;
import java.io.PrintWriter;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.io.RecordWriter;
import com.revolsys.data.record.property.FieldProperties;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.io.AbstractWriter;
import com.revolsys.io.IoConstants;
import com.revolsys.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Geometry;

public class WktRecordWriter extends AbstractWriter<Record>implements RecordWriter {

  private final RecordDefinition metaData;

  private boolean open;

  private final PrintWriter out;

  public WktRecordWriter(final RecordDefinition metaData, final java.io.Writer out) {
    this.metaData = metaData;
    this.out = new PrintWriter(new BufferedWriter(out));
    final FieldDefinition geometryAttribute = metaData.getGeometryField();
    if (geometryAttribute != null) {
      final GeometryFactory geometryFactory = geometryAttribute
        .getProperty(FieldProperties.GEOMETRY_FACTORY);
      setProperty(IoConstants.GEOMETRY_FACTORY, geometryFactory);
    }

  }

  @Override
  public void close() {
    this.out.close();
  }

  @Override
  public void flush() {
    this.out.flush();
  }

  @Override
  public String toString() {
    return this.metaData.getPath().toString();
  }

  @Override
  public void write(final Record object) {
    if (!this.open) {
      this.open = true;
    }
    final Geometry geometry = object.getGeometry();
    final int srid = geometry.getSRID();
    if (srid > 0) {
      this.out.print("SRID=");
      this.out.print(srid);
      this.out.print(';');
    }
    WktWriterOld.write(this.out, geometry);
    this.out.println();
  }

}
