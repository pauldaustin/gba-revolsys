package com.revolsys.record.io;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.io.Reader;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.parallel.process.AbstractProcess;
import com.revolsys.record.Record;
import com.revolsys.record.query.Query;
import com.revolsys.record.schema.RecordStore;

public class RecordStoreQueryTask extends AbstractProcess {

  private final BoundingBox boundingBox;

  private List<Record> objects;

  private final String path;

  private final RecordStore recordStore;

  public RecordStoreQueryTask(final RecordStore recordStore, final String path,
    final BoundingBox boundingBox) {
    this.recordStore = recordStore;
    this.path = path;
    this.boundingBox = boundingBox;
  }

  public void cancel() {
    this.objects = null;
  }

  @Override
  public String getBeanName() {
    return getClass().getName();
  }

  @Override
  public void run() {
    this.objects = new ArrayList<Record>();
    final Query query = new Query(this.path);
    query.setBoundingBox(this.boundingBox);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      for (final Record object : reader) {
        try {
          this.objects.add(object);
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
