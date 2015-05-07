package com.revolsys.gis.esri.gdb.file;

import java.util.NoSuchElementException;

import com.revolsys.collection.AbstractIterator;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.RecordState;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.model.FieldProperties;
import com.revolsys.gis.data.query.Query;
import com.revolsys.gis.esri.gdb.file.capi.swig.EnumRows;
import com.revolsys.gis.esri.gdb.file.capi.swig.Row;
import com.revolsys.gis.esri.gdb.file.capi.swig.Table;
import com.revolsys.gis.esri.gdb.file.capi.type.AbstractFileGdbFieldDefinition;
import com.revolsys.gis.esri.gdb.file.convert.GeometryConverter;

public class FileGdbQueryIterator extends AbstractIterator<Record> {

  private BoundingBox boundingBox;

  private int count;

  private String fields;

  private final int limit;

  private final int offset;

  private RecordDefinition recordDefinition;

  private RecordFactory recordFactory;

  private FileGdbRecordStoreImpl recordStore;

  private EnumRows rows;

  private String sql;

  private Table table;

  private final String typePath;

  FileGdbQueryIterator(final FileGdbRecordStoreImpl recordStore,
    final String typePath) {
    this(recordStore, typePath, "*", "", null, 0, -1);
  }

  FileGdbQueryIterator(final FileGdbRecordStoreImpl recordStore,
    final String typePath, final String whereClause) {
    this(recordStore, typePath, "*", whereClause, null, 0, -1);
  }

  FileGdbQueryIterator(final FileGdbRecordStoreImpl recordStore,
    final String typePath, final String whereClause,
    final BoundingBox boundingBox, final Query query, final int offset,
    final int limit) {
    this(recordStore, typePath, "*", whereClause, boundingBox, offset, limit);
    final RecordFactory factory = query.getProperty("dataObjectFactory");
    if (factory != null) {
      this.recordFactory = factory;
    }
  }

  FileGdbQueryIterator(final FileGdbRecordStoreImpl recordStore,
    final String typePath, final String fields, final String sql,
    final BoundingBox boundingBox, final int offset, final int limit) {
    this.recordStore = recordStore;
    this.typePath = typePath;
    this.recordDefinition = recordStore.getRecordDefinition(typePath);
    this.table = recordStore.getTable(typePath);
    this.fields = fields;
    this.sql = sql;
    setBoundingBox(boundingBox);
    this.recordFactory = recordStore.getRecordFactory();
    this.offset = offset;
    this.limit = limit;
  }

  @Override
  protected void doClose() {
    if (this.recordStore != null) {
      try {
        try {
          this.recordStore.closeEnumRows(this.rows);
        } finally {
          this.recordStore.releaseTable(this.typePath);
        }
      } catch (final Throwable e) {
      } finally {
        this.boundingBox = null;
        this.recordStore = null;
        this.fields = null;
        this.recordDefinition = null;
        this.rows = null;
        this.sql = null;
        this.table = null;
      }
    }
  }

  @Override
  protected void doInit() {
    if (this.recordDefinition != null) {
      synchronized (this.recordStore) {
        if (this.boundingBox == null) {
          if (this.sql.startsWith("SELECT")) {
            this.rows = this.recordStore.query(this.sql, true);
          } else {
            this.rows = this.recordStore.search(this.typePath, this.table,
              this.fields, this.sql, true);
          }
        } else {
          BoundingBox boundingBox = this.boundingBox;
          if (boundingBox.getWidth() == 0) {
            boundingBox = boundingBox.expand(1, 0);
          }
          if (boundingBox.getHeight() == 0) {
            boundingBox = boundingBox.expand(0, 1);
          }
          final com.revolsys.gis.esri.gdb.file.capi.swig.Envelope envelope = GeometryConverter.toEsri(boundingBox);
          this.rows = this.recordStore.search(this.typePath, this.table,
            this.fields, this.sql, envelope, true);
        }
      }
    }
  }

  @Override
  protected Record getNext() throws NoSuchElementException {
    if (this.rows == null || this.recordDefinition == null) {
      throw new NoSuchElementException();
    } else {
      Row row = null;
      while (this.offset > 0 && this.count < this.offset) {
        this.recordStore.nextRow(this.rows);
        this.count++;
      }
      if (this.limit > -1 && this.count >= this.offset + this.limit) {
        throw new NoSuchElementException();
      }
      row = this.recordStore.nextRow(this.rows);
      this.count++;
      if (row == null) {
        throw new NoSuchElementException();
      } else {
        try {
          final Record record = this.recordFactory.createRecord(this.recordDefinition);
          this.recordStore.addStatistic("query", record);
          record.setState(RecordState.Initalizing);
          for (final FieldDefinition field : this.recordDefinition.getFields()) {
            final String name = field.getName();
            final AbstractFileGdbFieldDefinition esriFieldDefinition = (AbstractFileGdbFieldDefinition)field;
            final Object value;
            synchronized (this.recordStore) {
              value = esriFieldDefinition.getValue(row);
            }
            record.setValue(name, value);
          }
          record.setState(RecordState.Persisted);
          return record;

        } finally {
          this.recordStore.closeRow(row);
        }
      }
    }
  }

  public void setBoundingBox(final BoundingBox boundingBox) {
    final RecordDefinition metaData = this.recordDefinition;
    if (metaData != null) {
      this.boundingBox = boundingBox;
      if (boundingBox != null) {
        final FieldDefinition geometryFieldDefinition = metaData.getGeometryField();
        if (geometryFieldDefinition != null) {
          final GeometryFactory geometryFactory = geometryFieldDefinition.getProperty(FieldProperties.GEOMETRY_FACTORY);
          if (geometryFactory != null) {
            this.boundingBox = boundingBox.convert(geometryFactory);
          }
        }
      }
    }
  }

  @Override
  public String toString() {
    return this.typePath + " " + this.sql;
  }
}
