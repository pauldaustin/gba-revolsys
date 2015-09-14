package com.revolsys.format.shp;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.springframework.core.io.Resource;

import com.revolsys.gis.data.io.ZipRecordReader;
import com.revolsys.io.FileUtil;
import com.revolsys.io.ZipRecordWriter;
import com.revolsys.record.RecordFactory;
import com.revolsys.record.io.AbstractRecordAndGeometryIoFactory;
import com.revolsys.record.io.RecordReader;
import com.revolsys.record.io.RecordWriter;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.spring.resource.FileSystemResource;

public class ShapefileZipIoFactory extends AbstractRecordAndGeometryIoFactory {

  public ShapefileZipIoFactory() {
    super("ESRI Shapefile inside a ZIP archive", true, true);
    addMediaTypeAndFileExtension("application/x-shp+zip", "shpz");
  }

  @Override
  public RecordReader createRecordReader(final Resource resource, final RecordFactory factory) {
    return new ZipRecordReader(resource, ShapefileConstants.FILE_EXTENSION, factory);
  }

  @Override
  public RecordWriter createRecordWriter(final String baseName,
    final RecordDefinition recordDefinition, final OutputStream outputStream,
    final Charset charset) {
    File directory;
    try {
      directory = FileUtil.createTempDirectory(baseName, "zipDir");
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to create temporary directory", e);
    }
    final Resource tempResource = new FileSystemResource(new File(directory, baseName + ".shp"));
    final RecordWriter shapeWriter = new ShapefileRecordWriter(recordDefinition, tempResource);
    return new ZipRecordWriter(directory, shapeWriter, outputStream);
  }

}
