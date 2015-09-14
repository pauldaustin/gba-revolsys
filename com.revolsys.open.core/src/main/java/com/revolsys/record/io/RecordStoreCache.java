package com.revolsys.record.io;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.record.Record;
import com.revolsys.record.schema.RecordStore;

public class RecordStoreCache {
  public static RecordStoreCache getCache(final RecordStore recordStore) {
    return new RecordStoreCache(recordStore);
  }

  private final Map<BoundingBox, List<Record>> cachedRecords = Collections
    .synchronizedMap(new HashMap<>());

  private final Map<BoundingBox, RecordStoreQueryTask> loadTasks = new LinkedHashMap<>();

  private final RecordStore recordStore;

  private String typePath;

  public RecordStoreCache(final RecordStore recordStore) {
    this.recordStore = recordStore;
  }

  private void addBoundingBox(final BoundingBox boundingBox) {
    synchronized (this.loadTasks) {
      if (!this.loadTasks.containsKey(boundingBox)) {
        this.loadTasks.put(boundingBox,
          new RecordStoreQueryTask(this.recordStore, this.typePath, boundingBox));
      }
    }
  }

  public List<Record> getRecords(final BoundingBox boundingBox) {
    final List<Record> records = this.cachedRecords.get(boundingBox);
    if (records == null) {
      addBoundingBox(boundingBox);
    }
    return records;
  }

  public void removeRecords(final BoundingBox boundingBox) {
    synchronized (this.loadTasks) {
      final RecordStoreQueryTask task = this.loadTasks.get(boundingBox);
      if (task != null) {
        task.cancel();
        this.loadTasks.remove(task);
      }
    }
    this.cachedRecords.remove(boundingBox);
  }

}
