package com.revolsys.gis.data.io;

import java.io.File;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.revolsys.data.record.ArrayRecordFactory;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.io.RecordIo;
import com.revolsys.data.record.io.RecordReader;
import com.revolsys.data.record.io.RecordReaderFactory;
import com.revolsys.io.IoFactoryRegistry;

public class FileRecordReaderFactory extends AbstractFactoryBean<RecordReader> {

  protected static RecordReaderFactory getRecordReaderFactory(final Resource resource) {
    final IoFactoryRegistry ioFactoryRegistry = IoFactoryRegistry.getInstance();
    final RecordReaderFactory readerFactory = ioFactoryRegistry
      .getFactory(RecordReaderFactory.class, resource);
    return readerFactory;
  }

  public static RecordReader recordReader(final File file) {
    final Resource resource = new FileSystemResource(file);
    return recordReader(resource);
  }

  public static RecordReader recordReader(final Resource resource) {
    final RecordReaderFactory readerFactory = getRecordReaderFactory(resource);
    if (readerFactory == null) {
      return null;
    } else {
      final RecordReader reader = readerFactory.createRecordReader(resource);
      return reader;
    }
  }

  public static RecordReader recordReader(final Resource resource, final RecordFactory factory) {
    final RecordReaderFactory readerFactory = getRecordReaderFactory(resource);
    if (readerFactory == null) {
      return null;
    } else {
      final RecordReader reader = readerFactory.createRecordReader(resource, factory);
      return reader;
    }
  }

  private RecordFactory factory = new ArrayRecordFactory();

  private Resource resource;

  @Override
  public RecordReader createInstance() throws Exception {
    final Resource resource1 = this.resource;
    final RecordFactory factory1 = this.factory;
    return RecordReader.create(resource1, factory1);
  }

  @Override
  protected void destroyInstance(final RecordReader reader) throws Exception {
    reader.close();
    this.factory = null;
    this.resource = null;
  }

  public RecordFactory getFactory() {
    return this.factory;
  }

  @Override
  public Class<?> getObjectType() {
    return RecordReader.class;
  }

  public Resource getResource() {
    return this.resource;
  }

  public void setFactory(final RecordFactory factory) {
    this.factory = factory;
  }

  @Required
  public void setResource(final Resource resource) {
    this.resource = resource;
  }

}
