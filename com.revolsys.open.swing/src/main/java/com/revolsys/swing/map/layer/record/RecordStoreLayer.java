package com.revolsys.swing.map.layer.record;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.swing.JComponent;
import javax.swing.SwingWorker;

import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.data.codes.CodeTable;
import com.revolsys.data.filter.OldRecordGeometryIntersectsFilter;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordState;
import com.revolsys.data.record.io.RecordStoreConnectionManager;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.gis.algorithm.index.RecordQuadTree;
import com.revolsys.gis.io.Statistics;
import com.revolsys.io.Path;
import com.revolsys.io.Reader;
import com.revolsys.io.Writer;
import com.revolsys.io.map.InvokeMethodMapObjectFactory;
import com.revolsys.io.map.MapObjectFactory;
import com.revolsys.io.map.MapSerializerUtil;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.component.BasePanel;
import com.revolsys.swing.component.ValueField;
import com.revolsys.swing.layout.GroupLayoutUtil;
import com.revolsys.swing.map.layer.record.table.model.RecordLayerTableModel;
import com.revolsys.swing.map.layer.record.table.model.RecordSaveErrorTableModel;
import com.revolsys.swing.parallel.Invoke;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;
import com.revolsys.util.Property;
import com.revolsys.util.enableable.Enabled;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

public class RecordStoreLayer extends AbstractRecordLayer {

  public static final String DATA_STORE = "dataStore";

  public static final MapObjectFactory FACTORY = new InvokeMethodMapObjectFactory(DATA_STORE,
    "Record Store", RecordStoreLayer.class, "create");

  public static AbstractRecordLayer create(final Map<String, Object> properties) {
    return new RecordStoreLayer(properties);
  }

  private BoundingBox boundingBox = new BoundingBox();

  private Map<String, LayerRecord> cachedRecords = new HashMap<String, LayerRecord>();

  private final Set<String> deletedRecordIds = new LinkedHashSet<String>();

  private final Set<String> formRecordIds = new LinkedHashSet<String>();

  private BoundingBox loadingBoundingBox = new BoundingBox();

  private SwingWorker<RecordQuadTree, Void> loadingWorker;

  private RecordStore recordStore;

  private final Object sync = new Object();

  private String typePath;

  public RecordStoreLayer(final Map<String, ? extends Object> properties) {
    super(properties);
    setType(DATA_STORE);
  }

  public RecordStoreLayer(final RecordStore recordStore, final String typePath,
    final boolean exists) {
    this.recordStore = recordStore;
    setExists(exists);
    setType(DATA_STORE);

    setTypePath(typePath);
    setRecordDefinition(recordStore.getRecordDefinition(typePath));
  }

  @SuppressWarnings("unchecked")
  protected <V extends LayerRecord> boolean addCachedRecord(final List<V> records, final V record) {
    final String id = getId(record);
    if (id == null) {
      records.add(record);
    } else if (record.getState() == RecordState.Deleted) {
      return false;
    } else {
      synchronized (this.cachedRecords) {
        final V cachedRecord = (V)this.cachedRecords.get(id);
        if (cachedRecord == null) {
          records.add(record);
        } else {
          if (cachedRecord.getState() == RecordState.Deleted) {
            return false;
          } else {
            records.add(cachedRecord);
          }
        }
      }
    }
    return true;
  }

  protected void addIds(final Set<String> ids, final Collection<? extends Record> records) {
    for (final Record record : records) {
      final String id = getId((LayerRecord)record);
      if (id != null) {
        ids.add(id);
      }
    }
  }

  @Override
  protected void addModifiedRecord(final LayerRecord record) {
    final LayerRecord cacheObject = getCacheRecord(record);
    if (cacheObject != null) {
      super.addModifiedRecord(cacheObject);
    }
  }

  @Override
  protected ValueField addPropertiesTabGeneralPanelSource(final BasePanel parent) {
    final ValueField panel = super.addPropertiesTabGeneralPanelSource(parent);
    final Map<String, String> connectionProperties = getProperty("connection");
    String connectionName = null;
    String url = null;
    String username = null;
    if (isExists()) {
      final RecordStore recordStore = getRecordStore();
      url = recordStore.getUrl();
      username = recordStore.getUsername();
    }
    if (connectionProperties != null) {
      connectionName = connectionProperties.get("name");
      if (!isExists()) {
        url = connectionProperties.get("url");
        username = connectionProperties.get("username");

      }
    }
    if (connectionName != null) {
      SwingUtil.addReadOnlyTextField(panel, "Data Store Name", connectionName);
    }
    if (url != null) {
      SwingUtil.addReadOnlyTextField(panel, "Data Store URL", url);
    }
    if (username != null) {
      SwingUtil.addReadOnlyTextField(panel, "Data Store Username", username);
    }
    SwingUtil.addReadOnlyTextField(panel, "Type Path", this.typePath);

    GroupLayoutUtil.makeColumns(panel, 2, true);
    return panel;
  }

  @Override
  protected void addSelectedRecord(final LayerRecord record) {
    final Record cachedObject = getCacheRecord(record);
    if (cachedObject != null) {
      super.addSelectedRecord(record);
    }
  }

  @Override
  public void addSelectedRecords(final BoundingBox boundingBox) {
    if (isSelectable()) {
      final List<LayerRecord> records = getRecords(boundingBox);
      for (final Iterator<LayerRecord> iterator = records.iterator(); iterator.hasNext();) {
        final LayerRecord layerRecord = iterator.next();
        if (!isVisible(layerRecord) || super.isDeleted(layerRecord)) {
          iterator.remove();
        }
      }
      if (!records.isEmpty()) {
        showRecordsTable(RecordLayerTableModel.MODE_SELECTED);
      }
      addSelectedRecords(records);
    }
  }

  protected void cacheRecords(final Collection<? extends Record> records) {
    for (final Record record : records) {
      if (record instanceof LayerRecord) {
        final LayerRecord layerRecord = (LayerRecord)record;
        getCacheRecord(layerRecord);
      }
    }
  }

  /**
   * Remove any cached records that are currently not used.
   */
  protected void cleanCachedRecords() {
    synchronized (this.cachedRecords) {
      final Set<String> ids = getIdsToCache();
      final Map<String, LayerRecord> cachedRecords = new HashMap<String, LayerRecord>();
      for (final String id : ids) {
        final LayerRecord record = this.cachedRecords.get(id);
        if (record != null) {
          cachedRecords.put(id, record);
        }
      }
      this.cachedRecords = cachedRecords;
    }
  }

  protected void clearLoading(final BoundingBox loadedBoundingBox) {
    synchronized (this.sync) {
      if (loadedBoundingBox == this.loadingBoundingBox) {
        firePropertyChange("loaded", false, true);
        this.boundingBox = this.loadingBoundingBox;
        this.loadingBoundingBox = new BoundingBox();
        this.loadingWorker = null;
      }

    }
  }

  @Override
  public void clearSelectedRecords() {
    synchronized (this.cachedRecords) {
      super.clearSelectedRecords();
      cleanCachedRecords();
    }
  }

  protected LoadingWorker createLoadingWorker(final BoundingBox boundingBox) {
    return new LoadingWorker(this, boundingBox);
  }

  @Override
  public void delete() {
    if (this.recordStore != null) {
      final Map<String, String> connectionProperties = getProperty("connection");
      if (connectionProperties != null) {
        final Map<String, Object> config = new HashMap<String, Object>();
        config.put("connection", connectionProperties);
        RecordStoreConnectionManager.releaseRecordStore(config);
      }
      this.recordStore = null;
    }
    final SwingWorker<RecordQuadTree, Void> loadingWorker = this.loadingWorker;
    this.boundingBox = new BoundingBox();
    this.cachedRecords = new HashMap<String, LayerRecord>();
    this.loadingBoundingBox = new BoundingBox();
    this.loadingWorker = null;
    this.typePath = null;
    super.delete();
    if (loadingWorker != null) {
      loadingWorker.cancel(true);
    }
  }

  @Override
  public void deleteRecord(final LayerRecord record) {
    if (isLayerRecord(record)) {
      record.setState(RecordState.Deleted);
      unSelectRecords(record);
      final String id = getId(record);
      if (Property.hasValue(id)) {
        final LayerRecord cacheRecord = removeCacheRecord(id, record);
        this.deletedRecordIds.add(id);
        deleteRecord(cacheRecord, true);
        removeFromIndex(record);
        removeFromIndex(cacheRecord);
      } else {
        removeFromIndex(record);
        super.deleteRecord(record);
      }
    }
  }

  @Override
  protected boolean doInitialize() {
    RecordStore recordStore = this.recordStore;
    if (recordStore == null) {
      final Map<String, String> connectionProperties = getProperty("connection");
      if (connectionProperties == null) {
        LoggerFactory.getLogger(getClass()).error(
          "A data store layer requires a connectionProperties entry with a name or url, username, and password: "
            + getPath());
        return false;
      } else {
        final Map<String, Object> config = new HashMap<String, Object>();
        config.put("connection", connectionProperties);
        recordStore = RecordStoreConnectionManager.getRecordStore(config);

        if (recordStore == null) {
          LoggerFactory.getLogger(getClass())
            .error("Unable to create data store for layer: " + getPath());
          return false;
        } else {
          try {
            recordStore.initialize();
          } catch (final Throwable e) {
            throw new RuntimeException("Unable to iniaitlize data store for layer " + getPath(), e);
          }

          setRecordStore(recordStore);
        }
      }
    }
    RecordDefinition recordDefinition = this.getRecordDefinition();
    if (recordDefinition == null) {
      recordDefinition = recordStore.getRecordDefinition(this.typePath);
      if (recordDefinition == null) {
        LoggerFactory.getLogger(getClass())
          .error("Cannot find table " + this.typePath + " for layer " + getPath());
        return false;
      } else {
        setRecordDefinition(recordDefinition);
        return true;
      }
    } else {
      return true;
    }

  }

  @SuppressWarnings({
    "unchecked", "rawtypes"
  })
  @Override
  protected List<LayerRecord> doQuery(final BoundingBox boundingBox) {
    try (
      Enabled enabled = eventsDisabled()) {
      final GeometryFactory geometryFactory = getGeometryFactory();
      final BoundingBox queryBoundingBox = boundingBox.convert(geometryFactory);
      if (this.boundingBox.contains(queryBoundingBox)) {
        return (List)getIndex().queryIntersects(queryBoundingBox);
      } else {
        final List<LayerRecord> readRecords = getRecordsFromRecordStore(queryBoundingBox);
        final List<LayerRecord> records = getCachedRecords(readRecords);
        return records;
      }
    }
  }

  @SuppressWarnings({
    "unchecked", "rawtypes"
  })
  @Override
  public List<LayerRecord> doQuery(final Geometry geometry, final double distance) {
    try (
      Enabled enabled = eventsDisabled()) {
      final GeometryFactory geometryFactory = getGeometryFactory();
      final Geometry queryGeometry = geometryFactory.copy(geometry);
      BoundingBox boundingBox = BoundingBox.getBoundingBox(queryGeometry);
      boundingBox = boundingBox.expand(distance);
      final String typePath = getTypePath();
      final RecordStore recordStore = getRecordStore();
      final Reader reader = recordStore.query(this, typePath, queryGeometry, distance);
      try {
        final List<LayerRecord> results = reader.read();
        final List<LayerRecord> records = getCachedRecords(results);
        return records;
      } finally {
        reader.close();
      }
    }
  }

  @SuppressWarnings({
    "rawtypes", "unchecked"
  })
  @Override
  protected List<LayerRecord> doQuery(final Query query) {
    if (isExists()) {
      final RecordStore recordStore = getRecordStore();
      if (recordStore != null) {
        try (
          Enabled enabled = eventsDisabled()) {
          final Statistics statistics = query.getProperty("statistics");
          query.setProperty("recordFactory", this);
          final Reader<LayerRecord> reader = (Reader)recordStore.query(query);
          try {
            final List<LayerRecord> records = new ArrayList<LayerRecord>();
            for (final LayerRecord record : reader) {
              final boolean added = addCachedRecord(records, record);
              if (added && statistics != null) {
                statistics.add(record);
              }
            }
            return records;

          } finally {
            reader.close();
          }
        }
      }
    }
    return Collections.emptyList();
  }

  @SuppressWarnings({
    "rawtypes", "unchecked"
  })
  @Override
  protected List<LayerRecord> doQueryBackground(final BoundingBox boundingBox) {
    if (boundingBox == null || boundingBox.isEmpty()) {
      return Collections.emptyList();
    } else {
      synchronized (this.sync) {
        final BoundingBox loadBoundingBox = boundingBox.expandPercent(0.2);
        if (!this.boundingBox.contains(boundingBox)
          && !this.loadingBoundingBox.contains(boundingBox)) {
          if (this.loadingWorker != null) {
            this.loadingWorker.cancel(true);
          }
          this.loadingBoundingBox = loadBoundingBox;
          this.loadingWorker = createLoadingWorker(loadBoundingBox);
          Invoke.worker(this.loadingWorker);
        }
      }
      final GeometryFactory geometryFactory = getGeometryFactory();
      final Polygon polygon = boundingBox.toPolygon(geometryFactory, 10, 10);

      final RecordQuadTree index = getIndex();

      final List<LayerRecord> records = (List)index.queryIntersects(polygon);

      final Predicate predicate = new OldRecordGeometryIntersectsFilter(boundingBox.toGeometry());
      for (final ListIterator<LayerRecord> iterator = records.listIterator(); iterator.hasNext();) {
        final LayerRecord record = iterator.next();
        final LayerRecord cachedRecord = getCacheRecord(record);
        if (predicate.test(cachedRecord)) {
          iterator.set(cachedRecord);
        } else {
          iterator.remove();
        }
      }
      return records;
    }
  }

  @Override
  public void doRefresh() {
    synchronized (this.sync) {
      if (this.loadingWorker != null) {
        this.loadingWorker.cancel(true);
      }
      this.boundingBox = new BoundingBox();
      this.loadingBoundingBox = this.boundingBox;
      setIndex(null);
      cleanCachedRecords();
    }
    final RecordStore recordStore = getRecordStore();
    final String typePath = getTypePath();
    final CodeTable codeTable = recordStore.getCodeTable(typePath);
    if (codeTable != null) {
      codeTable.refresh();
    }
    fireRecordsChanged();
  }

  @Override
  protected boolean doSaveChanges(final RecordSaveErrorTableModel errors,
    final LayerRecord record) {
    final boolean deleted = isDeleted(record);

    if (isExists()) {
      final PlatformTransactionManager transactionManager = this.recordStore
        .getTransactionManager();
      try (
        Transaction transaction = new Transaction(transactionManager, Propagation.REQUIRES_NEW)) {
        try {
          final RecordStore recordStore = getRecordStore();
          if (recordStore != null) {
            try (
              final Writer<Record> writer = recordStore.createWriter()) {
              final RecordDefinition recordDefinition = getRecordDefinition();
              final String idFieldName = recordDefinition.getIdFieldName();
              final String idString = record.getIdString();
              if (this.deletedRecordIds.contains(idString) || super.isDeleted(record)) {
                record.setState(RecordState.Deleted);
                writer.write(record);
              } else {
                final int fieldCount = recordDefinition.getFieldCount();
                for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                  record.validateField(fieldIndex);
                }
                if (super.isModified(record)) {
                  writer.write(record);
                } else if (isNew(record)) {
                  Object id = record.getIdValue();
                  if (id == null && Property.hasValue(idFieldName)) {
                    id = recordStore.createPrimaryIdValue(this.typePath);
                    record.setValue(idFieldName, id);
                  }

                  writer.write(record);
                }
              }
            }
            if (!deleted) {
              record.setState(RecordState.Persisted);
            }
            return true;
          }
        } catch (final Throwable e) {
          throw transaction.setRollbackOnly(e);
        }
      }
    }
    return false;
  }

  @Override
  public BoundingBox getBoundingBox() {
    return getCoordinateSystem().getAreaBoundingBox();
  }

  @SuppressWarnings({
    "unchecked", "rawtypes"
  })
  protected <V extends LayerRecord> List<V> getCachedRecords() {
    synchronized (this.cachedRecords) {
      final List<V> cachedRecords = new ArrayList(this.cachedRecords.values());
      return cachedRecords;

    }
  }

  protected <V extends LayerRecord> List<V> getCachedRecords(
    final Collection<? extends V> records) {
    final List<V> cachedRecords = new ArrayList<V>();
    for (final V record : records) {
      addCachedRecord(cachedRecords, record);
    }
    return cachedRecords;
  }

  public LayerRecord getCacheRecord(final LayerRecord record) {
    if (record == null) {
      return null;
    } else {
      final String id = getId(record);
      return getCacheRecord(id, record);
    }
  }

  private LayerRecord getCacheRecord(final String id, final LayerRecord record) {
    if (Property.hasValue(id) && record != null && isLayerRecord(record)) {
      if (record.getState() == RecordState.New) {
        return record;
      } else if (record.getState() == RecordState.Deleted) {
        return record;
      } else {
        synchronized (this.cachedRecords) {
          if (this.cachedRecords.containsKey(id)) {
            final LayerRecord cachedRecord = this.cachedRecords.get(id);
            if (cachedRecord.getState() == RecordState.Deleted) {
              this.cachedRecords.remove(id);
            }
            return cachedRecord;
          } else {
            this.cachedRecords.put(id, record);
            return record;
          }
        }
      }
    } else {
      return record;
    }
  }

  protected String getId(final LayerRecord record) {
    if (isLayerRecord(record)) {
      return StringConverterRegistry.toString(record.getIdValue());
    } else {
      return null;
    }
  }

  protected Set<String> getIdsToCache() {
    final Set<String> ids = new HashSet<String>();
    ids.addAll(this.deletedRecordIds);
    ids.addAll(this.formRecordIds);
    addIds(ids, getSelectedRecords());
    addIds(ids, getHighlightedRecords());
    addIds(ids, getModifiedRecords());
    addIds(ids, getIndex().queryAll());
    return ids;
  }

  public BoundingBox getLoadingBoundingBox() {
    return this.loadingBoundingBox;
  }

  @Override
  public LayerRecord getRecordById(final Object id) {
    final RecordDefinition recordDefinition = getRecordDefinition();
    final String idFieldName = recordDefinition.getIdFieldName();
    if (idFieldName == null) {
      LoggerFactory.getLogger(getClass()).error(this.typePath + " does not have a primary key");
      return null;
    } else {
      final String idString = StringConverterRegistry.toString(id);
      final LayerRecord record = this.cachedRecords.get(idString);
      if (record == null) {
        final Query query = Query.equal(recordDefinition, idFieldName, id);
        query.setProperty("recordFactory", this);
        final RecordStore recordStore = getRecordStore();
        return (LayerRecord)recordStore.queryFirst(query);
      } else {
        return record;
      }
    }

  }

  @SuppressWarnings({
    "unchecked", "rawtypes"
  })
  protected List<LayerRecord> getRecords(final BoundingBox boundingBox) {
    final BoundingBox convertedBoundingBox = boundingBox.convert(getGeometryFactory());
    BoundingBox loadedBoundingBox;
    RecordQuadTree index;
    synchronized (this.sync) {
      loadedBoundingBox = this.boundingBox;
      index = getIndex();
    }
    List<LayerRecord> queryObjects;
    if (loadedBoundingBox.contains(boundingBox)) {
      queryObjects = (List)index.query(convertedBoundingBox);
    } else {
      queryObjects = getRecordsFromRecordStore(convertedBoundingBox);
    }
    final List<LayerRecord> allObjects = new ArrayList<LayerRecord>();
    if (!queryObjects.isEmpty()) {
      final Polygon polygon = convertedBoundingBox.toPolygon();
      try {
        for (final LayerRecord record : queryObjects) {
          if (!record.getState().equals(RecordState.Deleted)) {
            final Geometry geometry = record.getGeometry();
            if (geometry.intersects(polygon)) {
              allObjects.add(record);
            }
          }
        }
      } catch (final ClassCastException e) {
        LoggerFactory.getLogger(getClass()).error("error", e);
      }
    }

    return allObjects;
  }

  @SuppressWarnings({
    "rawtypes", "unchecked"
  })
  protected List<LayerRecord> getRecordsFromRecordStore(final BoundingBox boundingBox) {
    if (isExists()) {
      final RecordStore recordStore = getRecordStore();
      if (recordStore != null) {
        final Query query = new Query(getTypePath());
        query.setBoundingBox(boundingBox);
        query.setProperty("recordFactory", this);
        final Reader reader = recordStore.query(query);
        try {
          return reader.read();
        } finally {
          reader.close();
        }
      }
    }
    return Collections.emptyList();
  }

  @Override
  public RecordStore getRecordStore() {
    return this.recordStore;
  }

  @Override
  public int getRowCount(final Query query) {
    if (isExists()) {
      final RecordStore recordStore = getRecordStore();
      if (recordStore != null) {
        return recordStore.getRowCount(query);
      }
    }
    return 0;
  }

  @Override
  public String getTypePath() {
    return this.typePath;
  }

  @Override
  protected LayerRecord internalCancelChanges(final LayerRecord record) {
    if (record.getState() == RecordState.Deleted) {
      final String id = getId(record);
      if (Property.hasValue(id)) {
        this.deletedRecordIds.remove(id);
      }
    }
    return super.internalCancelChanges(record);
  }

  @Override
  public boolean isLayerRecord(final Record record) {
    if (record instanceof LayerRecord) {
      final LayerRecord layerRecord = (LayerRecord)record;
      if (layerRecord.getLayer() == this) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void postSaveChanges(final RecordState originalState, final LayerRecord record) {
    super.postSaveChanges(originalState, record);
    if (originalState == RecordState.New) {
      getCacheRecord(record);
    }
  }

  @Override
  protected boolean postSaveDeletedRecord(final LayerRecord record) {
    final boolean deleted = super.postSaveDeletedRecord(record);
    if (deleted) {
      final String id = record.getIdString();
      this.deletedRecordIds.remove(id);
    }
    return deleted;
  }

  public List<LayerRecord> query(final Map<String, ? extends Object> filter) {
    final RecordDefinition recordDefinition = getRecordDefinition();
    final Query query = Query.and(recordDefinition, filter);
    return query(query);
  }

  private LayerRecord removeCacheRecord(final String id, final LayerRecord record) {
    if (Property.hasValue(id) && record != null && isLayerRecord(record)) {
      if (record.getState() == RecordState.New) {
        return record;
      } else if (record.getState() == RecordState.Deleted) {
        return record;
      } else {
        synchronized (this.cachedRecords) {
          if (this.cachedRecords.containsKey(id)) {
            final LayerRecord cachedRecord = this.cachedRecords.remove(id);
            if (cachedRecord.getState() == RecordState.Deleted) {
              this.cachedRecords.remove(id);
            }
            return cachedRecord;
          }
        }
      }
    }
    return record;
  }

  @Override
  protected void removeForm(final LayerRecord record) {
    synchronized (this.formRecordIds) {
      final String id = getId(record);
      if (id != null) {
        this.formRecordIds.remove(id);
        cleanCachedRecords();
      }
      super.removeForm(record);
    }
  }

  @Override
  protected void removeSelectedRecord(final LayerRecord record) {
    final Record cachedObject = getCacheRecord(record);
    if (cachedObject != null) {
      super.removeSelectedRecord(record);
    }
  }

  @Override
  public void revertChanges(final LayerRecord record) {
    final String id = getId(record);
    this.deletedRecordIds.remove(id);
    super.revertChanges(record);
  }

  protected void setIndex(final BoundingBox loadedBoundingBox, final RecordQuadTree index) {
    if (this.sync != null) {
      synchronized (this.sync) {
        if (loadedBoundingBox == this.loadingBoundingBox) {
          setIndex(index);
          cacheRecords(index.queryAll());
          final List<LayerRecord> newObjects = getNewRecords();
          index.insert(newObjects);
          clearLoading(loadedBoundingBox);
        }
      }
      firePropertyChange("refresh", false, true);
    }
  }

  protected void setRecordStore(final RecordStore recordStore) {
    this.recordStore = recordStore;
  }

  @Override
  public void setSelectedRecords(final BoundingBox boundingBox) {
    if (isSelectable()) {
      final List<LayerRecord> records = getRecords(boundingBox);
      for (final Iterator<LayerRecord> iterator = records.iterator(); iterator.hasNext();) {
        final LayerRecord layerRecord = iterator.next();
        if (!isVisible(layerRecord) || super.isDeleted(layerRecord)) {
          iterator.remove();
        }
      }
      if (!records.isEmpty()) {
        showRecordsTable(RecordLayerTableModel.MODE_SELECTED);
      }
      setSelectedRecords(records);
    }
  }

  @Override
  public void setSelectedRecords(final Collection<LayerRecord> records) {
    super.setSelectedRecords(records);
    cleanCachedRecords();
  }

  public void setTypePath(final String typePath) {
    this.typePath = typePath;
    if (!Property.hasValue(getName())) {
      setName(Path.getName(typePath));
    }
    if (Property.hasValue(typePath)) {
      if (isExists()) {
        final RecordStore recordStore = getRecordStore();
        if (recordStore != null) {
          final RecordDefinition recordDefinition = recordStore.getRecordDefinition(typePath);
          if (recordDefinition != null) {

            setRecordDefinition(recordDefinition);
            setQuery(new Query(recordDefinition));
            return;
          }
        }
      }
    }
    setRecordDefinition(null);
    setQuery(null);
  }

  @Override
  public <V extends JComponent> V showForm(final LayerRecord record) {
    synchronized (this.formRecordIds) {
      final String id = getId(record);
      if (id == null) {
        return super.showForm(record);
      } else {
        this.formRecordIds.add(id);
        final LayerRecord cachedObject = getCacheRecord(id, record);
        return super.showForm(cachedObject);
      }
    }
  }

  @Override
  public Map<String, Object> toMap() {
    final Map<String, Object> map = super.toMap();
    MapSerializerUtil.add(map, "typePath", this.typePath);
    return map;
  }

  @Override
  public void unSelectRecords(final BoundingBox boundingBox) {
    if (isSelectable()) {
      final List<LayerRecord> records = getRecords(boundingBox);
      for (final Iterator<LayerRecord> iterator = records.iterator(); iterator.hasNext();) {
        final LayerRecord layerRecord = iterator.next();
        if (!isVisible(layerRecord) || super.isDeleted(layerRecord)) {
          iterator.remove();
        }
      }
      if (!records.isEmpty()) {
        showRecordsTable(RecordLayerTableModel.MODE_SELECTED);
      }
      unSelectRecords(records);
    }
  }

  @Override
  public void unSelectRecords(final Collection<? extends LayerRecord> records) {
    super.unSelectRecords(records);
    cleanCachedRecords();
  }

}
