package com.revolsys.format.wkt;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.springframework.core.io.Resource;

import com.revolsys.io.FileUtil;
import com.revolsys.record.RecordFactory;
import com.revolsys.record.io.AbstractRecordAndGeometryIoFactory;
import com.revolsys.record.io.RecordIteratorReader;
import com.revolsys.record.io.RecordReader;
import com.revolsys.record.io.RecordWriter;
import com.revolsys.record.schema.RecordDefinition;

public class WktIoFactory extends AbstractRecordAndGeometryIoFactory implements WktConstants {
  public WktIoFactory() {
    super(WktConstants.DESCRIPTION, false, false);
    addMediaTypeAndFileExtension(MEDIA_TYPE, FILE_EXTENSION);
  }

  @Override
  public RecordReader createRecordReader(final Resource resource, final RecordFactory factory) {
    try {
      final WktRecordIterator iterator = new WktRecordIterator(factory, resource);

      return new RecordIteratorReader(iterator);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to create reader for " + resource, e);
    }
  }

  @Override
  public RecordWriter createRecordWriter(final String baseName,
    final RecordDefinition recordDefinition, final OutputStream outputStream,
    final Charset charset) {
    final OutputStreamWriter writer = FileUtil.createUtf8Writer(outputStream);
    return new WktRecordWriter(recordDefinition, writer);
  }
}
