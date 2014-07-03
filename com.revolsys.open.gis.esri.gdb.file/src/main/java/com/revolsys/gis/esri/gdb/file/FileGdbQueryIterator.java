package com.revolsys.gis.esri.gdb.file;

import java.util.NoSuchElementException;

import com.revolsys.collection.AbstractIterator;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.RecordState;
import com.revolsys.data.record.property.AttributeProperties;
import com.revolsys.data.record.schema.Attribute;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.esri.gdb.file.capi.swig.EnumRows;
import com.revolsys.gis.esri.gdb.file.capi.swig.Row;
import com.revolsys.gis.esri.gdb.file.capi.swig.Table;
import com.revolsys.gis.esri.gdb.file.capi.type.AbstractFileGdbAttribute;
import com.revolsys.gis.esri.gdb.file.convert.GeometryConverter;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;

public class FileGdbQueryIterator extends AbstractIterator<Record> {

  private RecordFactory dataObjectFactory;

  private Table table;

  private String fields;

  private String sql;

  private BoundingBox boundingBox;

  private RecordDefinition metaData;

  private CapiFileGdbDataObjectStore dataStore;

  private EnumRows rows;

  private final String typePath;

  private final int offset;

  private final int limit;

  private int count;

  FileGdbQueryIterator(final CapiFileGdbDataObjectStore dataStore,
    final String typePath) {
    this(dataStore, typePath, "*", "", null, 0, -1);
  }

  FileGdbQueryIterator(final CapiFileGdbDataObjectStore dataStore,
    final String typePath, final String whereClause) {
    this(dataStore, typePath, "*", whereClause, null, 0, -1);
  }

  FileGdbQueryIterator(final CapiFileGdbDataObjectStore dataStore,
    final String typePath, final String whereClause,
    final BoundingBox boundingBox, final Query query, final int offset,
    final int limit) {
    this(dataStore, typePath, "*", whereClause, boundingBox, offset, limit);
    final RecordFactory factory = query.getProperty("dataObjectFactory");
    if (factory != null) {
      this.dataObjectFactory = factory;
    }
  }

  FileGdbQueryIterator(final CapiFileGdbDataObjectStore dataStore,
    final String typePath, final String fields, final String sql,
    final BoundingBox boundingBox, final int offset, final int limit) {
    this.dataStore = dataStore;
    this.typePath = typePath;
    this.metaData = dataStore.getRecordDefinition(typePath);
    this.table = dataStore.getTable(typePath);
    this.fields = fields;
    this.sql = sql;
    setBoundingBox(boundingBox);
    this.dataObjectFactory = dataStore.getDataObjectFactory();
    this.offset = offset;
    this.limit = limit;
  }

  @Override
  protected void doClose() {
    if (dataStore != null) {
      try {
        dataStore.closeEnumRows(rows);
      } catch (final Throwable e) {
      } finally {
        boundingBox = null;
        dataStore = null;
        fields = null;
        metaData = null;
        rows = null;
        sql = null;
        table = null;
      }
    }
  }

  @Override
  protected void doInit() {
    if (metaData != null) {
      synchronized (dataStore) {
        if (boundingBox == null) {
          if (sql.startsWith("SELECT")) {
            rows = dataStore.query(sql, true);
          } else {
            rows = dataStore.search(table, fields, sql, true);
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
          rows = dataStore.search(table, fields, sql, envelope, true);
        }
      }
    }
  }

  protected RecordDefinition getMetaData() {
    if (metaData == null) {
      hasNext();
    }
    return metaData;
  }

  @Override
  protected Record getNext() throws NoSuchElementException {
    if (rows == null || metaData == null) {
      throw new NoSuchElementException();
    } else {
      Row row = null;
      while (offset > 0 && count < offset) {
        dataStore.nextRow(rows);
        count++;
      }
      if (limit > -1 && count >= offset + limit) {
        throw new NoSuchElementException();
      }
      row = dataStore.nextRow(rows);
      count++;
      if (row == null) {
        throw new NoSuchElementException();
      } else {
        try {
          final Record object = dataObjectFactory.createRecord(metaData);
          dataStore.addStatistic("query", object);
          object.setState(RecordState.Initalizing);
          for (final Attribute attribute : metaData.getAttributes()) {
            final String name = attribute.getName();
            final AbstractFileGdbAttribute esriAttribute = (AbstractFileGdbAttribute)attribute;
            final Object value;
            synchronized (dataStore) {
              value = esriAttribute.getValue(row);
            }
            object.setValue(name, value);
          }
          object.setState(RecordState.Persisted);
          return object;

        } finally {
          dataStore.closeRow(row);
        }
      }
    }
  }

  public void setBoundingBox(final BoundingBox boundingBox) {
    final RecordDefinition metaData = this.metaData;
    if (metaData != null) {
      this.boundingBox = boundingBox;

      if (boundingBox != null) {
        final Attribute geometryAttribute = metaData.getGeometryAttribute();
        if (geometryAttribute != null) {
          final GeometryFactory geometryFactory = geometryAttribute.getProperty(AttributeProperties.GEOMETRY_FACTORY);
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
    return typePath.toString();
  }

}
