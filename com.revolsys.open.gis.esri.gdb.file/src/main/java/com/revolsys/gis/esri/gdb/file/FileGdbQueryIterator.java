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

  private RecordFactory dataObjectFactory;

  private Table table;

  private String fields;

  private String sql;

  private BoundingBox boundingBox;

  private RecordDefinition metaData;

  private CapiFileGdbRecordStore dataStore;

  private EnumRows rows;

  private final String typePath;

  private final int offset;

  private final int limit;

  private int count;

  FileGdbQueryIterator(final CapiFileGdbRecordStore dataStore,
    final String typePath) {
    this(dataStore, typePath, "*", "", null, 0, -1);
  }

  FileGdbQueryIterator(final CapiFileGdbRecordStore dataStore,
    final String typePath, final String whereClause) {
    this(dataStore, typePath, "*", whereClause, null, 0, -1);
  }

  FileGdbQueryIterator(final CapiFileGdbRecordStore dataStore,
    final String typePath, final String whereClause,
    final BoundingBox boundingBox, final Query query, final int offset,
    final int limit) {
    this(dataStore, typePath, "*", whereClause, boundingBox, offset, limit);
    final RecordFactory factory = query.getProperty("dataObjectFactory");
    if (factory != null) {
      this.dataObjectFactory = factory;
    }
  }

  FileGdbQueryIterator(final CapiFileGdbRecordStore dataStore,
    final String typePath, final String fields, final String sql,
    final BoundingBox boundingBox, final int offset, final int limit) {
    this.dataStore = dataStore;
    this.typePath = typePath;
    this.metaData = dataStore.getRecordDefinition(typePath);
    this.table = dataStore.getTable(typePath);
    this.fields = fields;
    this.sql = sql;
    setBoundingBox(boundingBox);
    this.dataObjectFactory = dataStore.getRecordFactory();
    this.offset = offset;
    this.limit = limit;
  }

  @Override
  protected void doClose() {
    if (this.dataStore != null) {
      try {
        this.dataStore.closeEnumRows(this.rows);
      } catch (final Throwable e) {
      } finally {
        this.boundingBox = null;
        this.dataStore = null;
        this.fields = null;
        this.metaData = null;
        this.rows = null;
        this.sql = null;
        this.table = null;
      }
    }
  }

  @Override
  protected void doInit() {
    if (this.metaData != null) {
      synchronized (this.dataStore) {
        if (this.boundingBox == null) {
          if (this.sql.startsWith("SELECT")) {
            this.rows = this.dataStore.query(this.sql, true);
          } else {
            this.rows = this.dataStore.search(this.table, this.fields,
              this.sql, true);
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
          this.rows = this.dataStore.search(this.table, this.fields, this.sql,
            envelope, true);
        }
      }
    }
  }

  protected RecordDefinition getMetaData() {
    if (this.metaData == null) {
      hasNext();
    }
    return this.metaData;
  }

  @Override
  protected Record getNext() throws NoSuchElementException {
    if (this.rows == null || this.metaData == null) {
      throw new NoSuchElementException();
    } else {
      Row row = null;
      while (this.offset > 0 && this.count < this.offset) {
        this.dataStore.nextRow(this.rows);
        this.count++;
      }
      if (this.limit > -1 && this.count >= this.offset + this.limit) {
        throw new NoSuchElementException();
      }
      row = this.dataStore.nextRow(this.rows);
      this.count++;
      if (row == null) {
        throw new NoSuchElementException();
      } else {
        try {
          final Record object = this.dataObjectFactory.createRecord(this.metaData);
          this.dataStore.addStatistic("query", object);
          object.setState(RecordState.Initalizing);
          for (final FieldDefinition attribute : this.metaData.getFields()) {
            final String name = attribute.getName();
            final AbstractFileGdbFieldDefinition esriFieldDefinition = (AbstractFileGdbFieldDefinition)attribute;
            final Object value;
            synchronized (this.dataStore) {
              value = esriFieldDefinition.getValue(row);
            }
            object.setValue(name, value);
          }
          object.setState(RecordState.Persisted);
          return object;

        } finally {
          this.dataStore.closeRow(row);
        }
      }
    }
  }

  public void setBoundingBox(final BoundingBox boundingBox) {
    final RecordDefinition metaData = this.metaData;
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

  public void setSql(final String whereClause) {
    this.sql = whereClause;
  }

  @Override
  public String toString() {
    return this.typePath.toString();
  }

}
