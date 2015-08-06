package com.revolsys.gis.oracle.esri;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.PreDestroy;

import com.esri.sde.sdk.client.SeConnection;
import com.esri.sde.sdk.client.SeEnvelope;
import com.esri.sde.sdk.client.SeException;
import com.esri.sde.sdk.client.SeFilter;
import com.esri.sde.sdk.client.SeLayer;
import com.esri.sde.sdk.client.SeQuery;
import com.esri.sde.sdk.client.SeRow;
import com.esri.sde.sdk.client.SeShape;
import com.esri.sde.sdk.client.SeShapeFilter;
import com.esri.sde.sdk.client.SeSqlConstruct;
import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.RecordState;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.gis.io.Statistics;
import com.revolsys.jdbc.io.JdbcRecordStore;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;

public class ArcSdeBinaryGeometryQueryIterator extends AbstractIterator<Record> {

  private SeConnection connection;

  private RecordFactory recordFactory;

  private JdbcRecordStore recordStore;

  private RecordDefinition metaData;

  private SeQuery seQuery;

  private List<FieldDefinition> attributes = new ArrayList<FieldDefinition>();

  private Query query;

  private Statistics statistics;

  private ArcSdeBinaryGeometryRecordUtil sdeUtil;

  public ArcSdeBinaryGeometryQueryIterator(final ArcSdeBinaryGeometryRecordUtil sdeUtil,
    final JdbcRecordStore recordStore, final Query query, final Map<String, Object> properties) {
    this.sdeUtil = sdeUtil;
    this.recordFactory = query.getProperty("recordFactory");
    if (this.recordFactory == null) {
      this.recordFactory = recordStore.getRecordFactory();
    }
    this.recordStore = recordStore;
    this.query = query;
    this.statistics = (Statistics)properties.get(Statistics.class.getName());
  }

  @Override
  @PreDestroy
  public void doClose() {
    if (this.sdeUtil != null) {
      try {
        this.seQuery = this.sdeUtil.close(this.seQuery);
      } finally {
        this.connection = this.sdeUtil.close(this.connection);
      }
    }
    this.sdeUtil = null;
    this.attributes = null;
    this.recordFactory = null;
    this.recordStore = null;
    this.metaData = null;
    this.query = null;
    this.seQuery = null;
    this.statistics = null;
  }

  @Override
  protected void doInit() {
    String tableName = this.recordStore.getDatabaseQualifiedTableName(this.query.getTypeName());
    this.metaData = this.query.getRecordDefinition();
    if (this.metaData == null) {
      if (tableName != null) {
        this.metaData = this.recordStore.getRecordDefinition(tableName);
        this.query.setRecordDefinition(this.metaData);

      }
    }
    if (this.metaData != null) {
      tableName = this.sdeUtil.getTableName(this.metaData);
    }
    try {

      final List<String> attributeNames = new ArrayList<String>(this.query.getFieldNames());
      if (attributeNames.isEmpty()) {
        this.attributes.addAll(this.metaData.getFields());
        attributeNames.addAll(this.metaData.getFieldNames());
      } else {
        for (final String attributeName : attributeNames) {
          if (attributeName.equals("*")) {
            this.attributes.addAll(this.metaData.getFields());
            attributeNames.addAll(this.metaData.getFieldNames());
          } else {
            final FieldDefinition attribute = this.metaData.getField(attributeName);
            if (attribute != null) {
              this.attributes.add(attribute);
            }
            attributeNames.add(attributeName);
          }
        }
      }

      this.connection = this.sdeUtil.createSeConnection();
      final SeSqlConstruct sqlConstruct = new SeSqlConstruct(tableName);
      final String[] columnNames = attributeNames.toArray(new String[0]);
      this.seQuery = new SeQuery(this.connection, columnNames, sqlConstruct);
      BoundingBox boundingBox = this.query.getBoundingBox();
      if (boundingBox != null) {
        final SeLayer layer = new SeLayer(this.connection, tableName,
          this.metaData.getGeometryFieldName());

        final GeometryFactory geometryFactory = this.metaData.getGeometryFactory();
        boundingBox = boundingBox.convert(geometryFactory);
        final SeEnvelope envelope = new SeEnvelope(boundingBox.getMinX(), boundingBox.getMinY(),
          boundingBox.getMaxX(), boundingBox.getMaxY());
        final SeShape shape = new SeShape(layer.getCoordRef());
        shape.generateRectangle(envelope);
        final SeShapeFilter filter = new SeShapeFilter(tableName,
          this.metaData.getGeometryFieldName(), shape, SeFilter.METHOD_ENVP);
        this.seQuery.setSpatialConstraints(SeQuery.SE_SPATIAL_FIRST, false, new SeFilter[] {
          filter
        });
      }
      // TODO where clause
      // TODO how to load geometry for non-spatial queries
      this.seQuery.prepareQuery();
      this.seQuery.execute();

      final String typePath = this.query.getTypeNameAlias();
      if (typePath != null) {
        final RecordDefinitionImpl newRecordDefinition = ((RecordDefinitionImpl)this.metaData)
          .rename(typePath);
        this.metaData = newRecordDefinition;
      }
    } catch (final SeException e) {
      this.seQuery = this.sdeUtil.close(this.seQuery);
      throw new RuntimeException("Error performing query", e);
    }
  }

  public RecordDefinition getMetaData() {
    if (this.metaData == null) {
      hasNext();
    }
    return this.metaData;
  }

  @Override
  protected Record getNext() throws NoSuchElementException {
    try {
      if (this.seQuery != null) {
        final SeRow row = this.seQuery.fetch();
        if (row != null) {
          final Record object = getNextRecord(this.metaData, row);
          if (this.statistics != null) {
            this.statistics.add(object);
          }
          return object;
        }
      }
      close();
      throw new NoSuchElementException();
    } catch (final SeException e) {
      close();
      throw new RuntimeException(this.query.getSql(), e);
    } catch (final RuntimeException e) {
      close();
      throw e;
    } catch (final Error e) {
      close();
      throw e;
    }
  }

  private Record getNextRecord(final RecordDefinition metaData, final SeRow row) {
    final Record object = this.recordFactory.createRecord(metaData);
    if (object != null) {
      object.setState(RecordState.Initalizing);
      for (int columnIndex = 0; columnIndex < this.attributes.size(); columnIndex++) {
        this.sdeUtil.setValueFromRow(object, row, columnIndex);
      }
      object.setState(RecordState.Persisted);
      this.recordStore.addStatistic("query", object);
    }
    return object;
  }

}
