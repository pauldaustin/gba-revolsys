package com.revolsys.format.cogojson;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.springframework.core.io.Resource;

import com.revolsys.data.record.io.AbstractRecordAndGeometryWriterFactory;
import com.revolsys.data.record.io.RecordWriter;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.format.geojson.GeoJsonConstants;
import com.revolsys.format.geojson.GeoJsonGeometryIterator;
import com.revolsys.format.geojson.GeoJsonRecordWriter;
import com.revolsys.gis.data.io.GeometryReader;
import com.revolsys.gis.geometry.io.GeometryReaderFactory;
import com.revolsys.io.FileUtil;

public class CogoJsonIoFactory extends AbstractRecordAndGeometryWriterFactory
  implements GeometryReaderFactory {

  public CogoJsonIoFactory() {
    super(GeoJsonConstants.COGO_DESCRIPTION, true, true);
    addMediaTypeAndFileExtension(GeoJsonConstants.COGO_MEDIA_TYPE,
      GeoJsonConstants.COGO_FILE_EXTENSION);
  }

  @Override
  public GeometryReader createGeometryReader(final Resource resource) {
    try {
      final GeoJsonGeometryIterator iterator = new GeoJsonGeometryIterator(resource);
      return new GeometryReader(iterator);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to create reader for " + resource, e);
    }
  }

  @Override
  public RecordWriter createRecordWriter(final String baseName, final RecordDefinition metaData,
    final OutputStream outputStream, final Charset charset) {
    final OutputStreamWriter writer = FileUtil.createUtf8Writer(outputStream);
    return new GeoJsonRecordWriter(writer, true);
  }

  @Override
  public boolean isBinary() {
    return false;
  }
}
