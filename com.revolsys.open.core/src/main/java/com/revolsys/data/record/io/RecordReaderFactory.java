package com.revolsys.data.record.io;

import java.io.File;
import java.nio.file.Path;

import org.springframework.core.io.Resource;

import com.revolsys.data.record.ArrayRecordFactory;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.io.IoFactoryWithCoordinateSystem;
import com.revolsys.io.Reader;

public interface RecordReaderFactory extends IoFactoryWithCoordinateSystem {

  Reader<Record> createDirectoryRecordReader();

  Reader<Record> createDirectoryRecordReader(File file);

  Reader<Record> createDirectoryRecordReader(File file, RecordFactory factory);

  /**
   * Create a reader for the resource using the ({@link ArrayRecordFactory}
   * ).
   *
   * @param file The file to read.
   * @return The reader for the file.
   */
  default RecordReader createRecordReader(final Object object) {
    return createRecordReader(object, ArrayRecordFactory.INSTANCE);

  }

  /**
   * Create a {@link RecordReader} for the given source. The source can be one of the following
   * classes.
   *
   * <ul>
   *   <li>{@link Path}</li>
   *   <li>{@link File}</li>
   *   <li>{@link Resource}</li>
   * </ul>
   * @param source The source to read the records from.
   * @param recordFactory The factory used to create records.
   * @return The reader.
   * @throws IllegalArgumentException If the source is not a supported class.
   */
  default RecordReader createRecordReader(final Object source, final RecordFactory factory) {
    final Resource resource = com.revolsys.spring.resource.Resource.getResource(source);
    return createRecordReader(resource, factory);
  }

  RecordReader createRecordReader(Resource resource);

  RecordReader createRecordReader(Resource resource, RecordFactory factory);

  boolean isBinary();
}
