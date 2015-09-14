package com.revolsys.format.saif;

import org.springframework.core.io.Resource;

import com.revolsys.record.RecordFactory;
import com.revolsys.record.io.AbstractRecordAndGeometryReaderFactory;
import com.revolsys.record.io.RecordReader;

public class SaifIoFactory extends AbstractRecordAndGeometryReaderFactory {

  public SaifIoFactory() {
    super("SAIF", false);
    addMediaTypeAndFileExtension("zip/x-saif", "saf");
  }

  @Override
  public RecordReader createRecordReader(final Resource resource,
    final RecordFactory recordFactory) {
    final SaifReader reader = new SaifReader(resource);
    return reader;
  }
}
