package com.revolsys.gis.esri.gdb.file;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.revolsys.gis.data.io.RecordStore;
import com.revolsys.gis.data.io.DataObjectStoreFactory;
import com.revolsys.gis.data.io.DataObjectStoreFactoryRegistry;
import com.revolsys.io.FileUtil;
import com.revolsys.util.CollectionUtil;

public class FileGdbRecordStoreFactory implements DataObjectStoreFactory {

  private static final List<String> FILE_NAME_EXTENSIONS = Arrays.asList("gdb");

  private static final List<String> URL_PATTERNS = Arrays.asList(
    "file:/(//)?.*.gdb/?", "folderconnection:/(//)?.*.gdb/?");

  private static final Map<String, AtomicInteger> COUNTS = new HashMap<String, AtomicInteger>();

  private static final Map<String, CapiFileGdbRecordStore> DATA_STORES = new HashMap<String, CapiFileGdbRecordStore>();

  public static CapiFileGdbRecordStore create(final File file) {
    if (file == null) {
      return null;
    } else {
      synchronized (COUNTS) {
        final String fileName = FileUtil.getCanonicalPath(file);
        final AtomicInteger count = CollectionUtil.get(COUNTS, fileName,
          new AtomicInteger());
        count.incrementAndGet();
        CapiFileGdbRecordStore dataStore = DATA_STORES.get(fileName);
        if (dataStore == null) {
          dataStore = new CapiFileGdbRecordStore(file);
          dataStore.setCreateMissingDataStore(false);
          DATA_STORES.put(fileName, dataStore);
        }
        return dataStore;
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
          final CapiFileGdbRecordStore dataStore = DATA_STORES.remove(fileName);
          if (dataStore != null) {
            dataStore.doClose();
          }
          COUNTS.remove(fileName);
        }
      }
    }
  }

  @Override
  public FileGdbRecordStore createDataObjectStore(
    final Map<String, ? extends Object> connectionProperties) {
    final Map<String, Object> properties = new LinkedHashMap<String, Object>(
      connectionProperties);
    final String url = (String)properties.remove("url");
    final File file = FileUtil.getUrlFile(url);

    final FileGdbRecordStore dataObjectStore = create(file);
    DataObjectStoreFactoryRegistry.setConnectionProperties(dataObjectStore,
      properties);
    return dataObjectStore;
  }

  @Override
  public Class<? extends RecordStore> getDataObjectStoreInterfaceClass(
    final Map<String, ? extends Object> connectionProperties) {
    return RecordStore.class;
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
  public List<String> getUrlPatterns() {
    return URL_PATTERNS;
  }

}
