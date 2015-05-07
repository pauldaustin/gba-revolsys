package com.revolsys.gis.esri.gdb.file;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.collection.AbstractIterator;
import com.revolsys.data.record.Record;
import com.revolsys.io.AbstractMultipleIteratorReader;
import com.revolsys.jts.geom.BoundingBox;

public class FileGdbReader extends AbstractMultipleIteratorReader<Record> {

  private BoundingBox boundingBox;

  private final FileGdbRecordStoreImpl dataStore;

  private int index = 0;

  private List<String> typePaths = new ArrayList<String>();

  public FileGdbReader(final FileGdbRecordStoreImpl dataStore) {
    this.dataStore = dataStore;
  }

  public BoundingBox getBoundingBox() {
    return this.boundingBox;
  }

  @Override
  protected AbstractIterator<Record> getNextIterator() {
    if (this.index < this.typePaths.size()) {
      final String typePath = this.typePaths.get(this.index);
      final FileGdbQueryIterator iterator = new FileGdbQueryIterator(
        this.dataStore, typePath);
      if (this.boundingBox != null) {
        iterator.setBoundingBox(this.boundingBox);
      }
      this.index++;
      return iterator;
    } else {
      return null;
    }
  }

  public List<String> getTypeNames() {
    return this.typePaths;
  }

  public void setBoundingBox(final BoundingBox boundingBox) {
    this.boundingBox = boundingBox;
  }

  public void setTypeNames(final List<String> typePaths) {
    this.typePaths = typePaths;
  }

  @Override
  public String toString() {
    return "Reader " + this.dataStore.getLabel() + " " + this.typePaths + " "
      + this.boundingBox;
  }
}
