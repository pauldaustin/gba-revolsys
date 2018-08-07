package com.revolsys.swing.map.layer.record;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.revolsys.datatype.DataType;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.io.PathName;
import com.revolsys.predicate.Predicates;
import com.revolsys.record.Record;
import com.revolsys.record.RecordState;
import com.revolsys.record.Records;
import com.revolsys.record.query.Condition;
import com.revolsys.record.query.Query;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordDefinitionImpl;
import com.revolsys.swing.map.layer.record.table.RecordLayerTable;
import com.revolsys.swing.map.layer.record.table.RecordLayerTablePanel;
import com.revolsys.swing.map.layer.record.table.model.ListRecordLayerTableModel;
import com.revolsys.swing.map.layer.record.table.model.RecordLayerErrors;

public class ListRecordLayer extends AbstractRecordLayer {

  public static ListRecordLayer newLayer(final String name, final GeometryFactory geometryFactory,
    final DataType geometryType) {
    final RecordDefinitionImpl recordDefinition = newRecordDefinition(name, geometryFactory,
      geometryType);
    return new ListRecordLayer(recordDefinition);
  }

  public static RecordDefinitionImpl newRecordDefinition(final String name,
    final GeometryFactory geometryFactory, final DataType geometryType) {
    final RecordDefinitionImpl recordDefinition = new RecordDefinitionImpl(
      PathName.newPathName(name));
    recordDefinition.addField("GEOMETRY", geometryType, true);
    recordDefinition.setGeometryFactory(geometryFactory);
    return recordDefinition;
  }

  private List<LayerRecord> records = new ArrayList<>();

  public ListRecordLayer(final Map<String, ? extends Object> properties) {
    this("recordListLayer");
    setProperties(properties);
  }

  public ListRecordLayer(final RecordDefinition recordDefinition) {
    this("listRecordLayer");
    final String name = recordDefinition.getName();
    setName(name);
    setRecordDefinition(recordDefinition);
  }

  protected ListRecordLayer(final String type) {
    super(type);
    setEditable(true);
  }

  public void addNewRecordPersisted(final Map<String, Object> values) {
    final LayerRecord record = newRecordPersisted(values);
    addRecord(record);
  }

  protected void addRecord(final LayerRecord record) {
    addRecordDo(record);
    fireRecordsChanged();
  }

  protected void addRecordDo(final LayerRecord record) {
    synchronized (this.records) {
      this.records.add(record);
      expandBoundingBox(record);
    }
    addToIndex(record);
  }

  protected void addRecords(final List<? extends LayerRecord> records) {
    for (final LayerRecord record : records) {
      addRecordDo(record);
    }
    fireRecordsChanged();
  }

  public void clearRecords() {
    clearRecordsDo();
    fireRecordsChanged();
  }

  protected void clearRecordsDo() {
    this.clearSelectedRecords();
    synchronized (this.records) {
      this.records.clear();
    }
    clearIndex();
    cleanCachedRecords();
  }

  @Override
  public ListRecordLayer clone() {
    final ListRecordLayer clone = (ListRecordLayer)super.clone();
    clone.records = new ArrayList<>();
    return clone;
  }

  @Override
  protected void deleteRecordPost(final LayerRecord record) {
    super.deleteRecordPost(record);
    removeRecord(record);
  }

  @Override
  protected void deleteRecordsPost(final List<LayerRecord> recordsDeleted,
    final List<LayerRecord> recordsSelected) {
    removeFromIndex(recordsDeleted);
    for (final LayerRecord record : recordsDeleted) {
      removeRecord(record);
    }
    refreshBoundingBox();
    fireRecordsChanged();
    fireEmpty();
    super.deleteRecordsPost(recordsDeleted, recordsSelected);
    saveChanges(recordsDeleted);
  }

  protected void expandBoundingBox(final LayerRecord record) {
    if (record != null) {
      BoundingBox boundingBox = getBoundingBox();
      if (boundingBox.isEmpty()) {
        boundingBox = Records.boundingBox(record);
      } else {
        boundingBox = boundingBox.expandToInclude(record);
      }
      setBoundingBox(boundingBox);
    }
  }

  public void fireEmpty() {
    final boolean empty = isEmpty();
    firePropertyChange("empty", !empty, empty);
  }

  @Override
  public void fireRecordsChanged() {
    super.fireRecordsChanged();
    fireEmpty();
  }

  @Override
  protected void forEachRecordInternal(final Query query,
    final Consumer<? super LayerRecord> consumer) {
    final List<LayerRecord> records = getRecordsPersisted(query);
    records.forEach(consumer);
  }

  @Override
  public LayerRecord getRecord(final int index) {
    if (index >= 0) {
      synchronized (this.records) {
        if (index < this.records.size()) {
          return this.records.get(index);
        }
      }
    }
    return null;
  }

  @Override
  public int getRecordCount(final Query query) {
    synchronized (this.records) {
      final Predicate<Record> filter = query.getWhereCondition();
      return Predicates.count(this.records, filter);
    }
  }

  @Override
  public int getRecordCountDeleted() {
    return 0;
  }

  @Override
  public int getRecordCountModified() {
    return 0;
  }

  @Override
  public int getRecordCountNew() {
    return 0;
  }

  @Override
  public int getRecordCountPersisted() {
    return this.records.size();
  }

  @Override
  public int getRecordCountPersisted(final Query query) {
    final Condition filter = query.getWhereCondition();
    return Predicates.count(this.records, filter);
  }

  @Override
  public List<LayerRecord> getRecords() {
    synchronized (this.records) {
      return new ArrayList<>(this.records);
    }
  }

  @Override
  public List<LayerRecord> getRecordsPersisted(final Query query) {
    final List<LayerRecord> records = getRecords();
    final Condition filter = query.getWhereCondition();
    final Map<? extends CharSequence, Boolean> orderBy = query.getOrderBy();
    Records.filterAndSort(records, filter, orderBy);
    return records;
  }

  public boolean isEmpty() {
    return this.records.isEmpty();
  }

  @Override
  public LayerRecord newLayerRecord(final Map<String, ? extends Object> values) {
    final LayerRecord record = super.newLayerRecord(values);
    if (record != null) {
      addRecord(record);
    }
    return record;
  }

  public LayerRecord newRecordPersisted(final Map<String, Object> values) {
    final LayerRecord record = newLayerRecord(getRecordDefinition());
    record.setState(RecordState.INITIALIZING);
    try {
      record.setValues(values);
    } finally {
      record.setState(RecordState.PERSISTED);
    }
    return record;
  }

  @Override
  public RecordLayerTablePanel newTablePanel(final Map<String, Object> config) {
    final RecordLayerTable table = ListRecordLayerTableModel.newTable(this);
    return new RecordLayerTablePanel(this, table, config);
  }

  protected void refreshBoundingBox() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    if (geometryFactory != null) {
      BoundingBox boundingBox = geometryFactory.newBoundingBoxEmpty();
      for (final LayerRecord record : getRecords()) {
        boundingBox = boundingBox.expandToInclude(record);
      }
      setBoundingBox(boundingBox);
    }
  }

  @Override
  protected void refreshDo() {
    super.refreshDo();
    setIndexRecords(getRecords());
  }

  protected void removeRecord(final LayerRecord record) {
    removeFromIndex(record);
    synchronized (this.records) {
      record.removeFrom(this.records);
    }
  }

  @Override
  protected boolean saveChangesDo(final RecordLayerErrors errors, final LayerRecord record) {
    if (record.isDeleted()) {
      return true;
    } else {
      return super.saveChangesDo(errors, record);
    }
  }

  public void setRecords(final List<? extends LayerRecord> records) {
    clearRecordsDo();
    for (final LayerRecord record : records) {
      addRecordDo(record);
    }
    fireRecordsChanged();
  }
}
