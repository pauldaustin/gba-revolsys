package com.revolsys.gis.data.io;

import java.util.Map;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.data.model.ArrayRecord;
import com.revolsys.gis.data.model.DataObjectUtil;
import com.revolsys.io.AbstractWriter;
import com.revolsys.io.Writer;
import com.vividsolutions.jts.geom.Geometry;

public class DataObjectWriterGeometryWriter extends AbstractWriter<Geometry> {
  private final Writer<Record> writer;

  public DataObjectWriterGeometryWriter(final Writer<Record> writer) {
    this.writer = writer;
  }

  @Override
  public void close() {
    this.writer.close();
  }

  @Override
  public void flush() {
    this.writer.flush();
  }

  @Override
  public Map<String, Object> getProperties() {
    return this.writer.getProperties();
  }

  @Override
  public <V> V getProperty(final String name) {
    return (V)this.writer.getProperty(name);
  }

  @Override
  public void setProperty(final String name, final Object value) {
    this.writer.setProperty(name, value);
  }

  @Override
  public void write(final Geometry geometry) {
    final RecordDefinition metaData = DataObjectUtil.createGeometryMetaData();
    final Record object = new ArrayRecord(metaData);
    object.setGeometryValue(geometry);
    this.writer.write(object);
  }

}
