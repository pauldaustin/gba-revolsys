package com.revolsys.gis.data.io;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.core.io.Resource;

import com.revolsys.gis.io.Statistics;
import com.revolsys.io.FileNames;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.Reader;
import com.revolsys.record.Record;
import com.revolsys.record.io.RecordReader;
import com.revolsys.record.io.RecordReaderFactory;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordDefinitionFactory;

public class RecordDirectoryReader extends AbstractDirectoryReader<Record>
  implements RecordDefinitionFactory {

  private Statistics statistics = new Statistics();

  private final Map<String, RecordDefinition> typePathRecordDefinitionMap = new HashMap<String, RecordDefinition>();

  public RecordDirectoryReader() {
  }

  protected void addRecordDefinition(final RecordReader reader) {
    final RecordDefinition recordDefinition = reader.getRecordDefinition();
    if (recordDefinition != null) {
      final String path = recordDefinition.getPath();
      this.typePathRecordDefinitionMap.put(path, recordDefinition);
    }
  }

  @Override
  protected Reader<Record> createReader(final Resource resource) {
    final IoFactoryRegistry registry = IoFactoryRegistry.getInstance();
    final String filename = resource.getFilename();
    final String extension = FileNames.getFileNameExtension(filename);
    final RecordReaderFactory factory = registry
      .getFactoryByFileExtension(RecordReaderFactory.class, extension);
    final RecordReader reader = factory.createRecordReader(resource);
    addRecordDefinition(reader);
    return reader;
  }

  @Override
  public RecordDefinition getRecordDefinition(final String path) {
    final RecordDefinition recordDefinition = this.typePathRecordDefinitionMap.get(path);
    return recordDefinition;
  }

  public Statistics getStatistics() {
    return this.statistics;
  }

  /**
   * Get the next data object read by this reader.
   *
   * @return The next record.
   * @exception NoSuchElementException If the reader has no more data objects.
   */
  @Override
  public Record next() {
    final Record record = super.next();
    this.statistics.add(record);
    return record;
  }

  public void setStatistics(final Statistics statistics) {
    if (this.statistics != statistics) {
      this.statistics = statistics;
      statistics.connect();
    }
  }

}
