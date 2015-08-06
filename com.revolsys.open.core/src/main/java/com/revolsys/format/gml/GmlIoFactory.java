package com.revolsys.format.gml;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.springframework.core.io.Resource;

import com.revolsys.data.record.io.AbstractRecordAndGeometryWriterFactory;
import com.revolsys.data.record.io.RecordWriter;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.data.io.GeometryReader;
import com.revolsys.gis.geometry.io.GeometryReaderFactory;
import com.revolsys.io.FileUtil;

public class GmlIoFactory extends AbstractRecordAndGeometryWriterFactory implements
  GeometryReaderFactory {
  public GmlIoFactory() {
    super(GmlConstants.FORMAT_DESCRIPTION, true, true);
    addMediaTypeAndFileExtension(GmlConstants.MEDIA_TYPE, GmlConstants.FILE_EXTENSION);
  }

  @Override
  public GeometryReader createGeometryReader(final Resource resource) {
    final GmlGeometryIterator iterator = new GmlGeometryIterator(resource);
    return new GeometryReader(iterator);
  }

  @Override
  public RecordWriter createRecordWriter(final String baseName, final RecordDefinition metaData,
    final OutputStream outputStream, final Charset charset) {
    final OutputStreamWriter writer = FileUtil.createUtf8Writer(outputStream);
    return new GmlRecordWriter(metaData, writer);
  }

  @Override
  public boolean isBinary() {
    return false;
  }
}
