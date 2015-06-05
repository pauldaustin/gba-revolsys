package com.revolsys.gis.data.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.io.RecordReader;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.io.AbstractReader;

public class ListDataObjectReader extends AbstractReader<Record> implements
  RecordReader {
  private RecordDefinition metaData;

  private List<Record> objects = new ArrayList<Record>();

  public ListDataObjectReader(final RecordDefinition metaData,
    final Collection<? extends Record> objects) {
    this.metaData = metaData;
    this.objects = new ArrayList<Record>(objects);
  }

  public ListDataObjectReader(final RecordDefinition metaData,
    final Record... objects) {
    this(metaData, Arrays.asList(objects));
  }

  @Override
  public void close() {
    metaData = null;
    objects = null;
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    return metaData;
  }

  @Override
  public Iterator<Record> iterator() {
    return objects.iterator();
  }

  @Override
  public void open() {
  }
}
