package com.revolsys.gis.esri.gdb.file;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.revolsys.collection.map.Maps;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.Paths;
import com.revolsys.record.io.RecordStoreFactory;
import com.revolsys.record.io.RecordStoreFactoryRegistry;
import com.revolsys.record.io.RecordStoreRecordAndGeometryWriterFactory;
import com.revolsys.record.schema.AbstractRecordStore;
import com.revolsys.record.schema.RecordStore;

public class FileGdbRecordStoreFactory implements RecordStoreFactory {

  private static final Map<String, AtomicInteger> COUNTS = new HashMap<String, AtomicInteger>();

  private static final List<String> FILE_NAME_EXTENSIONS = Arrays.asList("gdb");

  private static final Map<String, FileGdbRecordStore> RECORD_STORES = new HashMap<String, FileGdbRecordStore>();

  private static final List<String> URL_PATTERNS = Arrays.asList("file:/(//)?.*.gdb/?",
    "folderconnection:/(//)?.*.gdb/?");

  static {
    final RecordStoreRecordAndGeometryWriterFactory writerFactory = new RecordStoreRecordAndGeometryWriterFactory(
      "ESRI File Geodatabase", "application/x-esri-gdb", true, true, "gdb");
    IoFactoryRegistry.getInstance().addFactory(writerFactory);
  }

  public static FileGdbRecordStore create(final File file) {
    if (file == null) {
      return null;
    } else {
      synchronized (COUNTS) {
        final String fileName = FileUtil.getCanonicalPath(file);
        final AtomicInteger count = Maps.get(COUNTS, fileName, new AtomicInteger());
        count.incrementAndGet();
        FileGdbRecordStore recordStore = RECORD_STORES.get(fileName);
        if (recordStore == null || recordStore.isClosed()) {
          recordStore = new FileGdbRecordStore(file);
          RECORD_STORES.put(fileName, recordStore);
        }
        return recordStore;
      }
    }
  }

  /**
   * Release the record store for the file. Decrements the count of references to the file. If
   * the count <=0 then the record store will be removed.
   *
   * @param fileName
   * @return True if the record store has no references and was released. False otherwise
   */
  static boolean release(final FileGdbRecordStore recordStore) {
    synchronized (COUNTS) {
      final String fileName = recordStore.getFileName();
      final FileGdbRecordStore currentRecordStore = RECORD_STORES.get(fileName);
      if (currentRecordStore == recordStore) {
        final AtomicInteger countHolder = Maps.get(COUNTS, fileName, new AtomicInteger());
        final int count = countHolder.decrementAndGet();
        if (count <= 0) {
          COUNTS.remove(fileName);
          RECORD_STORES.remove(fileName);
          return true;
        }
      } else {
        return !recordStore.isClosed();
      }
    }
    return false;
  }

  @Override
  public boolean canOpen(final Path path) {
    if (RecordStoreFactory.super.canOpen(path)) {
      if (Paths.exists(Paths.getPath(path, "timestamps"))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public AbstractRecordStore createRecordStore(
    final Map<String, ? extends Object> connectionProperties) {
    final Map<String, Object> properties = new LinkedHashMap<String, Object>(connectionProperties);
    final String url = (String)properties.remove("url");
    final File file = FileUtil.getUrlFile(url);

    final AbstractRecordStore recordStore = create(file);
    RecordStoreFactoryRegistry.setConnectionProperties(recordStore, properties);
    return recordStore;
  }

  @Override
  public String getName() {
    return "ESRI File Geodatabase";
  }

  @Override
  public List<String> getRecordStoreFileExtensions() {
    return FILE_NAME_EXTENSIONS;
  }

  @Override
  public Class<? extends RecordStore> getRecordStoreInterfaceClass(
    final Map<String, ? extends Object> connectionProperties) {
    return RecordStore.class;
  }

  @Override
  public List<String> getUrlPatterns() {
    return URL_PATTERNS;
  }

}
