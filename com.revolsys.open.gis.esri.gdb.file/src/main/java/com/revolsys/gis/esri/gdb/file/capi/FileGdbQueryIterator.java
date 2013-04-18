package com.revolsys.gis.esri.gdb.file.capi;

import java.util.NoSuchElementException;

import com.revolsys.collection.AbstractIterator;
import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.AttributeProperties;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectFactory;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectState;
import com.revolsys.gis.data.query.Query;
import com.revolsys.gis.esri.gdb.file.capi.swig.EnumRows;
import com.revolsys.gis.esri.gdb.file.capi.swig.Row;
import com.revolsys.gis.esri.gdb.file.capi.swig.Table;
import com.revolsys.gis.esri.gdb.file.capi.type.AbstractFileGdbAttribute;
import com.revolsys.gis.esri.gdb.file.convert.GeometryConverter;

public class FileGdbQueryIterator extends AbstractIterator<DataObject> {

  private DataObjectFactory dataObjectFactory;

  private Table table;

  private String fields;

  private String whereClause;

  private BoundingBox boundingBox;

  private DataObjectMetaData metaData;

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
    final String typePath, final BoundingBox boundingBox) {
    this(dataStore, typePath, "*", "", boundingBox, 0, -1);
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
    final DataObjectFactory factory = query.getProperty("dataObjectFactory");
    if (factory != null) {
      this.dataObjectFactory = factory;
    }
  }

  FileGdbQueryIterator(final CapiFileGdbDataObjectStore dataStore,
    final String typePath, final String fields, final String whereClause) {
    this(dataStore, typePath, fields, whereClause, null, 0, -1);
  }

  FileGdbQueryIterator(final CapiFileGdbDataObjectStore dataStore,
    final String typePath, final String fields, final String whereClause,
    final BoundingBox boundingBox, final int offset, final int limit) {
    this.dataStore = dataStore;
    this.typePath = typePath;
    this.metaData = dataStore.getMetaData(typePath);
    if (metaData == null) {
      throw new IllegalArgumentException("Unknown type " + typePath);
    }
    this.table = dataStore.getTable(typePath);
    this.fields = fields;
    this.whereClause = whereClause;
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
        rows = null;
        if (table != null) {
          dataStore.closeTable(table);
          table = null;
        }
        dataStore = null;
        metaData = null;
        fields = null;
        whereClause = null;
        boundingBox = null;
      } catch (final Throwable t) {
        t.printStackTrace();
      }
    }
  }

  @Override
  protected void doInit() {
    synchronized (dataStore) {
      if (boundingBox == null) {
        if (whereClause.startsWith("SELECT *")) {
          rows = dataStore.getGeodatabase().query(whereClause, false);
        } else {
          rows = dataStore.search(table, fields, whereClause, true);
        }
      } else {
        final com.revolsys.gis.esri.gdb.file.capi.swig.Envelope boundingBox = GeometryConverter.toEsri(this.boundingBox);
        rows = dataStore.search(table, fields, whereClause, boundingBox, true);
      }
    }
  }

  protected DataObjectMetaData getMetaData() {
    if (metaData == null) {
      hasNext();
    }
    return metaData;
  }

  @Override
  protected DataObject getNext() throws NoSuchElementException {
    synchronized (dataStore) {
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
          final DataObject object = dataObjectFactory.createDataObject(metaData);
          object.setState(DataObjectState.Initalizing);
          for (final Attribute attribute : metaData.getAttributes()) {
            final String name = attribute.getName();
            final AbstractFileGdbAttribute esriAttribute = (AbstractFileGdbAttribute)attribute;
            final Object value = esriAttribute.getValue(row);
            object.setValue(name, value);
          }
          object.setState(DataObjectState.Persisted);
          return object;
        } finally {
          dataStore.closeRow(row);
        }
      }
    }
  }

  public void setBoundingBox(final BoundingBox boundingBox) {
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

  public void setWhereClause(final String whereClause) {
    this.whereClause = whereClause;
  }

  @Override
  public String toString() {
    return typePath.toString();
  }

}
