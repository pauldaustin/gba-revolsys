package com.revolsys.gis.data.io;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.gis.data.query.Query;
import com.revolsys.io.Reader;
import com.revolsys.parallel.process.AbstractProcess;

public class DataStoreQueryTask extends AbstractProcess {

  private final RecordStore dataStore;

  private final BoundingBox boundingBox;

  private List<Record> objects;

  private final String path;

  public DataStoreQueryTask(final RecordStore dataStore, final String path,
    final BoundingBox boundingBox) {
    this.dataStore = dataStore;
    this.path = path;
    this.boundingBox = boundingBox;
  }

  public void cancel() {
    objects = null;
  }

  @Override
  public String getBeanName() {
    return getClass().getName();
  }

  @Override
  public void run() {
    objects = new ArrayList<Record>();
    final Query query = new Query(path);
    query.setBoundingBox(boundingBox);
    final Reader<Record> reader = dataStore.query(query);
    try {
      for (final Record object : reader) {
        try {
          objects.add(object);
        } catch (final NullPointerException e) {
          return;
        }
      }
    } finally {
      reader.close();
    }
  }

  @Override
  public void setBeanName(final String name) {
  }
}
