package com.revolsys.format.shp;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;

import com.revolsys.record.RecordFactory;
import com.revolsys.record.io.AbstractRecordAndGeometryIoFactory;
import com.revolsys.record.io.RecordIteratorReader;
import com.revolsys.record.io.RecordReader;
import com.revolsys.record.io.RecordStoreFactory;
import com.revolsys.record.io.RecordWriter;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordStore;
import com.revolsys.spring.resource.OutputStreamResource;

public class ShapefileIoFactory extends AbstractRecordAndGeometryIoFactory
  implements RecordStoreFactory {
  public ShapefileIoFactory() {
    super(ShapefileConstants.DESCRIPTION, true, true);
    addMediaTypeAndFileExtension(ShapefileConstants.MIME_TYPE, ShapefileConstants.FILE_EXTENSION);
    setSingleFile(false);
  }

  @Override
  public RecordReader createRecordReader(final Resource resource,
    final RecordFactory recordFactory) {
    try {
      final ShapefileIterator iterator = new ShapefileIterator(resource, recordFactory);
      return new RecordIteratorReader(iterator);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to create reader for " + resource, e);
    }
  }

  @Override
  public RecordStore createRecordStore(final Map<String, ? extends Object> connectionProperties) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RecordWriter createRecordWriter(final RecordDefinition recordDefinition,
    final Resource resource) {
    return new ShapefileRecordWriter(recordDefinition, resource);
  }

  @Override
  public RecordWriter createRecordWriter(final String baseName,
    final RecordDefinition recordDefinition, final OutputStream outputStream,
    final Charset charset) {
    return createRecordWriter(recordDefinition, new OutputStreamResource(baseName, outputStream));
  }

  @Override
  public List<String> getRecordStoreFileExtensions() {
    return Collections.emptyList();
  }

  @Override
  public Class<? extends RecordStore> getRecordStoreInterfaceClass(
    final Map<String, ? extends Object> connectionProperties) {
    return RecordStore.class;
  }

  @Override
  public List<String> getUrlPatterns() {
    return null;
  }
}