package com.revolsys.format.moep;

import org.springframework.core.io.Resource;

import com.revolsys.record.RecordFactory;
import com.revolsys.record.io.AbstractRecordAndGeometryReaderFactory;
import com.revolsys.record.io.RecordReader;
import com.revolsys.record.schema.RecordDefinition;

public class MoepBinaryReaderFactory extends AbstractRecordAndGeometryReaderFactory {
  public MoepBinaryReaderFactory() {
    super("MOEP (BC Ministry of Environment and Parks)", true);
    addMediaTypeAndFileExtension("application/x-bcgov-moep-bin", "bin");
    setCustomAttributionSupported(false);
  }

  public RecordReader createRecordReader(final RecordDefinition recordDefinition,
    final Resource resource, final RecordFactory recordFactory) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RecordReader createRecordReader(final Resource resource,
    final RecordFactory recordFactory) {
    return new MoepBinaryReader(null, resource, recordFactory);
  }
}
