package com.revolsys.io;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.io.AbstractRecordIoFactory;
import com.revolsys.data.record.schema.AbstractRecordStore;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.record.schema.RecordStoreSchema;
import com.revolsys.io.filter.DirectoryFilenameFilter;
import com.revolsys.io.filter.ExtensionFilenameFilter;
import com.vividsolutions.jts.geom.Geometry;

public class DirectoryDataObjectStore extends AbstractRecordStore {

  private boolean createMissingTables = true;

  private final Map<String, Writer<Record>> writers = new HashMap<String, Writer<Record>>();

  private File directory;

  private String fileExtension;

  private Writer<Record> writer;

  private boolean createMissingDataStore = true;

  public DirectoryDataObjectStore(final File directory, final String fileExtension) {
    this.directory = directory;
    this.fileExtension = fileExtension;
  }

  @Override
  public void close() {
    this.directory = null;
    if (this.writers != null) {
      for (final Writer<Record> writer : this.writers.values()) {
        writer.close();
      }
      this.writers.clear();
    }
    if (this.writer != null) {
      this.writer.close();
      this.writer = null;
    }
    super.close();
  }

  @Override
  public Writer<Record> createWriter() {
    return new DirectoryDataObjectStoreWriter(this);
  }

  public File getDirectory() {
    return this.directory;
  }

  public String getFileExtension() {
    return this.fileExtension;
  }

  @Override
  public RecordDefinition getRecordDefinition(final RecordDefinition objectMetaData) {
    final RecordDefinition metaData = super.getRecordDefinition(objectMetaData);
    if (metaData == null && this.createMissingTables) {
      final String typePath = objectMetaData.getPath();
      final String schemaName = Path.getPath(typePath);
      RecordStoreSchema schema = getSchema(schemaName);
      if (schema == null && this.createMissingTables) {
        schema = new RecordStoreSchema(this, schemaName);
        addSchema(schema);
      }
      final File schemaDirectory = new File(this.directory, schemaName);
      if (!schemaDirectory.exists()) {
        schemaDirectory.mkdirs();
      }
      final RecordDefinitionImpl newMetaData = new RecordDefinitionImpl(schema, typePath);
      for (final FieldDefinition attribute : objectMetaData.getFields()) {
        final FieldDefinition newAttribute = new FieldDefinition(attribute);
        newMetaData.addField(newAttribute);
      }
      schema.addElement(newMetaData);
    }
    return metaData;
  }

  @Override
  public int getRowCount(final Query query) {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized Writer<Record> getWriter() {
    if (this.writer == null && this.directory != null) {
      this.writer = new DirectoryDataObjectStoreWriter(this);
    }
    return this.writer;
  }

  @PostConstruct
  @Override
  public void initialize() {
    if (!this.directory.exists()) {
      this.directory.mkdirs();
    }
    super.initialize();
  }

  @Override
  public synchronized void insert(final Record object) {
    final RecordDefinition metaData = object.getRecordDefinition();
    final String typePath = metaData.getPath();
    Writer<Record> writer = this.writers.get(typePath);
    if (writer == null) {
      final String schemaName = Path.getPath(typePath);
      final File subDirectory = new File(getDirectory(), schemaName);
      final File file = new File(subDirectory, metaData.getName() + "." + getFileExtension());
      final Resource resource = new FileSystemResource(file);
      writer = AbstractRecordIoFactory.dataObjectWriter(metaData, resource);
      if (writer instanceof ObjectWithProperties) {
        final ObjectWithProperties properties = writer;
        properties.setProperties(getProperties());
      }
      this.writers.put(typePath, writer);
    }
    writer.write(object);
    addStatistic("Insert", object);
  }

  public boolean isCreateMissingDataStore() {
    return this.createMissingDataStore;
  }

  public boolean isCreateMissingTables() {
    return this.createMissingTables;
  }

  protected RecordDefinition loadMetaData(final String schemaName, final File file) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void loadSchemaDataObjectMetaData(final RecordStoreSchema schema,
    final Map<String, RecordDefinition> metaDataMap) {
    final String schemaName = schema.getPath();
    final File subDirectory = new File(this.directory, schemaName);
    final File[] files = subDirectory.listFiles(new ExtensionFilenameFilter(this.fileExtension));
    if (files != null) {
      for (final File file : files) {
        final RecordDefinition metaData = loadMetaData(schemaName, file);
        if (metaData != null) {
          final String typePath = metaData.getPath();
          metaDataMap.put(typePath, metaData);
        }
      }
    }
  }

  @Override
  protected void loadSchemas(final Map<String, RecordStoreSchema> schemaMap) {
    final File[] directories = this.directory.listFiles(new DirectoryFilenameFilter());
    if (directories != null) {
      for (final File subDirectory : directories) {
        final String directoryName = FileUtil.getFileName(subDirectory);
        addSchema(new RecordStoreSchema(this, directoryName));
      }
    }
  }

  @Override
  public Reader<Record> query(final RecordFactory dataObjectFactory, final String typePath,
    final Geometry geometry) {
    throw new UnsupportedOperationException();
  }

  public void setCreateMissingRecordStore(final boolean createMissingDataStore) {
    this.createMissingDataStore = createMissingDataStore;
  }

  public void setCreateMissingTables(final boolean createMissingTables) {
    this.createMissingTables = createMissingTables;
  }

  public void setDirectory(final File directory) {
    this.directory = directory;
  }

  protected void setFileExtension(final String fileExtension) {
    this.fileExtension = fileExtension;
  }
}
