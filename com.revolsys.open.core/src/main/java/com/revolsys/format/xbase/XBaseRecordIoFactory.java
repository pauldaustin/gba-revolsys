package com.revolsys.format.xbase;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.springframework.core.io.Resource;

import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.io.AbstractRecordIoFactory;
import com.revolsys.data.record.io.RecordIteratorReader;
import com.revolsys.data.record.io.RecordReader;
import com.revolsys.data.record.io.RecordWriter;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.spring.resource.OutputStreamResource;

public class XBaseRecordIoFactory extends AbstractRecordIoFactory {
  public XBaseRecordIoFactory() {
    super("D-Base", true, false, true);
    addMediaTypeAndFileExtension("application/dbase", "dbf");
    addMediaTypeAndFileExtension("application/dbf", "dbf");
  }

  @Override
  public RecordReader createRecordReader(final Resource resource,
    final RecordFactory recordFactory) {
    try {
      final XbaseIterator iterator = new XbaseIterator(resource, recordFactory);

      return new RecordIteratorReader(iterator);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to create reader for " + resource, e);
    }
  }

  @Override
  public RecordWriter createRecordWriter(final RecordDefinition metaData, final Resource resource) {
    return new XbaseRecordWriter(metaData, resource);
  }

  @Override
  public RecordWriter createRecordWriter(final String baseName, final RecordDefinition metaData,
    final OutputStream outputStream, final Charset charset) {
    return createRecordWriter(metaData, new OutputStreamResource(baseName, outputStream));
  }

}
