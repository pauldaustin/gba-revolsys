package com.revolsys.io.wkt;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.springframework.core.io.Resource;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.io.AbstractRecordAndGeometryIoFactory;
import com.revolsys.data.record.io.RecordReader;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.data.io.DataObjectIteratorReader;
import com.revolsys.io.FileUtil;
import com.revolsys.io.Writer;

public class WktIoFactory extends AbstractRecordAndGeometryIoFactory
  implements WktConstants {
  public WktIoFactory() {
    super(WktConstants.DESCRIPTION, false, false);
    addMediaTypeAndFileExtension(MEDIA_TYPE, FILE_EXTENSION);
  }

  @Override
  public RecordReader createRecordReader(final Resource resource,
    final RecordFactory factory) {
    try {
      final WktDataObjectIterator iterator = new WktDataObjectIterator(factory,
        resource);

      return new DataObjectIteratorReader(iterator);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to create reader for " + resource, e);
    }
  }

  @Override
  public Writer<Record> createRecordWriter(final String baseName,
    final RecordDefinition metaData, final OutputStream outputStream,
    final Charset charset) {
    final OutputStreamWriter writer = FileUtil.createUtf8Writer(outputStream);
    return new WktDataObjectWriter(metaData, writer);
  }
}
