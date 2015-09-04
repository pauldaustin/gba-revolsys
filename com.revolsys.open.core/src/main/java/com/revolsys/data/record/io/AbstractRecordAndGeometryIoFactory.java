package com.revolsys.data.record.io;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.core.io.Resource;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.Records;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.epsg.EpsgCoordinateSystems;
import com.revolsys.gis.data.io.RecordWriterGeometryWriter;
import com.revolsys.gis.geometry.io.GeometryWriterFactory;
import com.revolsys.io.Writer;
import com.vividsolutions.jts.geom.Geometry;

public abstract class AbstractRecordAndGeometryIoFactory extends
  AbstractRecordAndGeometryReaderFactory implements RecordWriterFactory, GeometryWriterFactory {

  private Set<CoordinateSystem> coordinateSystems = EpsgCoordinateSystems.getCoordinateSystems();

  public AbstractRecordAndGeometryIoFactory(final String name, final boolean binary,
    final boolean customAttributionSupported) {
    super(name, binary);
    setCustomAttributionSupported(customAttributionSupported);
  }

  @Override
  public Writer<Geometry> createGeometryWriter(final Resource resource) {
    final RecordDefinition recordDefinition = Records.createGeometryRecordDefinition();
    final Writer<Record> recordWriter = createRecordWriter(recordDefinition, resource);
    return createGeometryWriter(recordWriter);
  }

  @Override
  public Writer<Geometry> createGeometryWriter(final String baseName, final OutputStream out) {
    final RecordDefinition recordDefinition = Records.createGeometryRecordDefinition();
    final Writer<Record> recordWriter = createRecordWriter(baseName, recordDefinition, out);
    return createGeometryWriter(recordWriter);
  }

  @Override
  public Writer<Geometry> createGeometryWriter(final String baseName, final OutputStream out,
    final Charset charset) {
    final RecordDefinition recordDefinition = Records.createGeometryRecordDefinition();
    final Writer<Record> recordWriter = createRecordWriter(baseName, recordDefinition, out,
      charset);
    return createGeometryWriter(recordWriter);
  }

  public Writer<Geometry> createGeometryWriter(final Writer<Record> recordWriter) {
    final Writer<Geometry> geometryWriter = new RecordWriterGeometryWriter(recordWriter);
    return geometryWriter;
  }

  @Override
  public Set<CoordinateSystem> getCoordinateSystems() {
    return this.coordinateSystems;
  }

  @Override
  public boolean isCoordinateSystemSupported(final CoordinateSystem coordinateSystem) {
    return this.coordinateSystems.contains(coordinateSystem);
  }

  @Override
  public boolean isGeometrySupported() {
    return true;
  }

  @Override
  protected void setCoordinateSystems(final CoordinateSystem... coordinateSystems) {
    setCoordinateSystems(new LinkedHashSet<CoordinateSystem>(Arrays.asList(coordinateSystems)));
  }

  @Override
  protected void setCoordinateSystems(final Set<CoordinateSystem> coordinateSystems) {
    this.coordinateSystems = coordinateSystems;
  }
}
