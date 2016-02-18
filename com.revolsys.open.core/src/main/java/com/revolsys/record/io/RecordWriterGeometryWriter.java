package com.revolsys.record.io;

import java.util.Map;

import com.revolsys.geometry.io.GeometryWriter;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.io.AbstractWriter;
import com.revolsys.record.ArrayRecord;
import com.revolsys.record.Record;
import com.revolsys.record.Records;
import com.revolsys.record.schema.RecordDefinition;

public class RecordWriterGeometryWriter extends AbstractWriter<Geometry>implements GeometryWriter {
  private final RecordWriter writer;

  public RecordWriterGeometryWriter(final RecordWriter writer) {
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
    final RecordDefinition recordDefinition = Records.newGeometryRecordDefinition();
    final Record object = new ArrayRecord(recordDefinition);
    object.setGeometryValue(geometry);
    this.writer.write(object);
  }

}