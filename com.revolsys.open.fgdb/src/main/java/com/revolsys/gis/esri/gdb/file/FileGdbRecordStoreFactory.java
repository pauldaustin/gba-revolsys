package com.revolsys.gis.esri.gdb.file;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.gis.data.io.RecordStoreFactory;
import com.revolsys.gis.data.io.RecordStoreFactoryRegistry;
import com.revolsys.io.FileUtil;
import com.revolsys.util.CollectionUtil;

public class FileGdbRecordStoreFactory implements RecordStoreFactory {

  private static final Map<String, AtomicInteger> COUNTS = new HashMap<String, AtomicInteger>();

  private static final List<String> FILE_NAME_EXTENSIONS = Arrays.asList("gdb");

  private static final Map<String, FileGdbRecordStoreImpl> RECORD_STORES = new HashMap<String, FileGdbRecordStoreImpl>();

  private static final List<String> URL_PATTERNS = Arrays.asList(
    "file:/(//)?.*.gdb/?", "folderconnection:/(//)?.*.gdb/?");

  public static FileGdbRecordStoreImpl create(final File file) {
    if (file == null) {
      return null;
    } else {
      synchronized (COUNTS) {
        final String fileName = FileUtil.getCanonicalPath(file);
        final AtomicInteger count = CollectionUtil.get(COUNTS, fileName,
          new AtomicInteger());
        count.incrementAndGet();
        FileGdbRecordStoreImpl recordStore = RECORD_STORES.get(fileName);
        if (recordStore == null || recordStore.isClosed()) {
          recordStore = new FileGdbRecordStoreImpl(file);
          recordStore.setCreateMissingRecordStore(false);
          RECORD_STORES.put(fileName, recordStore);
        }
        return recordStore;
      }
    }
  }

  static void release(final String fileName) {
    if (fileName != null) {
      synchronized (COUNTS) {
        final AtomicInteger countHolder = CollectionUtil.get(COUNTS, fileName,
          new AtomicInteger());
        final int count = countHolder.decrementAndGet();
        if (count <= 0) {
          COUNTS.remove(fileName);
          final FileGdbRecordStoreImpl dataStore = RECORD_STORES.remove(fileName);
          if (dataStore != null) {
            dataStore.doClose();
          }
          COUNTS.remove(fileName);
        }
      }
    }
  }

  @Override
  public FileGdbRecordStore createRecordStore(
    final Map<String, ? extends Object> connectionProperties) {
    final Map<String, Object> properties = new LinkedHashMap<String, Object>(
      connectionProperties);
    final String url = (String)properties.remove("url");
    final File file = FileUtil.getUrlFile(url);

    final FileGdbRecordStore dataObjectStore = create(file);
    RecordStoreFactoryRegistry.setConnectionProperties(dataObjectStore,
      properties);
    return dataObjectStore;
  }

  @Override
  public List<String> getFileExtensions() {
    return FILE_NAME_EXTENSIONS;
  }

  @Override
  public String getName() {
    return "ESRI File Geodatabase";
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
