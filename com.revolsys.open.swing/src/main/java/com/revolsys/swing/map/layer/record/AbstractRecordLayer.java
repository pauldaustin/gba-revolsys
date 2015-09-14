package com.revolsys.swing.map.layer.record;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoableEdit;

import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.revolsys.beans.InvokeMethodCallable;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.datatype.DataType;
import com.revolsys.datatype.DataTypes;
import com.revolsys.equals.Equals;
import com.revolsys.gis.algorithm.index.RecordQuadTree;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.data.io.ListRecordReader;
import com.revolsys.gis.jts.LineStringUtil;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.CoordinatesUtil;
import com.revolsys.io.map.MapSerializerUtil;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.record.ArrayRecord;
import com.revolsys.record.Record;
import com.revolsys.record.RecordFactory;
import com.revolsys.record.RecordState;
import com.revolsys.record.filter.OldRecordGeometryDistanceFilter;
import com.revolsys.record.filter.OldRecordGeometryIntersectsFilter;
import com.revolsys.record.io.RecordReader;
import com.revolsys.record.property.DirectionalFieldsOld;
import com.revolsys.record.query.Condition;
import com.revolsys.record.query.Query;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordStore;
import com.revolsys.spring.resource.ByteArrayResource;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.action.enablecheck.AndEnableCheck;
import com.revolsys.swing.action.enablecheck.EnableCheck;
import com.revolsys.swing.component.BaseDialog;
import com.revolsys.swing.component.BasePanel;
import com.revolsys.swing.component.TabbedValuePanel;
import com.revolsys.swing.dnd.ClipboardUtil;
import com.revolsys.swing.dnd.transferable.RecordReaderTransferable;
import com.revolsys.swing.dnd.transferable.StringTransferable;
import com.revolsys.swing.map.MapPanel;
import com.revolsys.swing.map.form.FieldNamesSetPanel;
import com.revolsys.swing.map.form.RecordLayerForm;
import com.revolsys.swing.map.form.SnapLayersPanel;
import com.revolsys.swing.map.layer.AbstractLayer;
import com.revolsys.swing.map.layer.Layer;
import com.revolsys.swing.map.layer.LayerGroup;
import com.revolsys.swing.map.layer.LayerRenderer;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.map.layer.record.component.MergeRecordsDialog;
import com.revolsys.swing.map.layer.record.renderer.AbstractRecordLayerRenderer;
import com.revolsys.swing.map.layer.record.renderer.GeometryStyleRenderer;
import com.revolsys.swing.map.layer.record.style.GeometryStyle;
import com.revolsys.swing.map.layer.record.style.panel.LayerStylePanel;
import com.revolsys.swing.map.layer.record.table.RecordLayerTable;
import com.revolsys.swing.map.layer.record.table.RecordLayerTablePanel;
import com.revolsys.swing.map.layer.record.table.model.RecordDefinitionTableModel;
import com.revolsys.swing.map.layer.record.table.model.RecordLayerTableModel;
import com.revolsys.swing.map.layer.record.table.model.RecordSaveErrorTableModel;
import com.revolsys.swing.map.overlay.AbstractOverlay;
import com.revolsys.swing.map.overlay.AddGeometryCompleteAction;
import com.revolsys.swing.map.overlay.CloseLocation;
import com.revolsys.swing.map.overlay.EditGeometryOverlay;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.parallel.Invoke;
import com.revolsys.swing.table.BaseJTable;
import com.revolsys.swing.tree.MenuSourcePropertyEnableCheck;
import com.revolsys.swing.tree.MenuSourceRunnable;
import com.revolsys.swing.tree.node.record.RecordStoreTableTreeNode;
import com.revolsys.swing.undo.SetObjectProperty;
import com.revolsys.util.CompareUtil;
import com.revolsys.util.ExceptionUtil;
import com.revolsys.util.Property;
import com.revolsys.util.enableable.Enabled;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public abstract class AbstractRecordLayer extends AbstractLayer
  implements RecordFactory, AddGeometryCompleteAction {

  public static final String ALL = "All";

  public static final String FORM_FACTORY_EXPRESSION = "formFactoryExpression";

  private static AtomicInteger formCount = new AtomicInteger();

  static {
    final Class<AbstractRecordLayer> clazz = AbstractRecordLayer.class;
    final MenuFactory menu = MenuFactory.getMenu(clazz);
    menu.setName("Layer");
    menu.addGroup(0, "table");
    menu.addGroup(2, "edit");
    menu.addGroup(3, "dnd");

    final EnableCheck exists = new MenuSourcePropertyEnableCheck("exists");

    menu.addMenuItem(clazz, "ViewRecords");

    final EnableCheck hasSelectedRecords = new MenuSourcePropertyEnableCheck("hasSelectedRecords");
    final EnableCheck hasGeometry = new MenuSourcePropertyEnableCheck("hasGeometry");
    menu.addMenuItem("zoom",
      MenuSourceRunnable.createAction("Zoom to Selected", "magnifier_zoom_selected",
        new AndEnableCheck(exists, hasGeometry, hasSelectedRecords), "zoomToSelected"));

    final EnableCheck editable = new MenuSourcePropertyEnableCheck("editable");
    final EnableCheck readonly = new MenuSourcePropertyEnableCheck("readOnly", false);
    final EnableCheck hasChanges = new MenuSourcePropertyEnableCheck("hasChanges");
    final EnableCheck canAdd = new MenuSourcePropertyEnableCheck("canAddRecords");
    final EnableCheck canDelete = new MenuSourcePropertyEnableCheck("canDeleteRecords");
    final EnableCheck canMergeRecords = new MenuSourcePropertyEnableCheck("canMergeRecords");
    final EnableCheck canPaste = new MenuSourcePropertyEnableCheck("canPaste");

    menu.addCheckboxMenuItem("edit",
      MenuSourceRunnable.createAction("Editable", "pencil", readonly, "toggleEditable"), editable);

    menu.addMenuItem("edit",
      MenuSourceRunnable.createAction("Save Changes", "table_save", hasChanges, "saveChanges"));

    menu.addMenuItem("edit", MenuSourceRunnable.createAction("Cancel Changes", "table_cancel",
      hasChanges, "cancelChanges"));

    menu.addMenuItem("edit", MenuSourceRunnable.createAction("Add New Record", "table_row_insert",
      canAdd, "addNewRecord"));

    menu.addMenuItem("edit",
      MenuSourceRunnable.createAction("Delete Selected Records", "table_row_delete",
        new AndEnableCheck(hasSelectedRecords, canDelete), "deleteSelectedRecords"));

    menu.addMenuItem("edit", MenuSourceRunnable.createAction("Merge Selected Records",
      "shape_group", canMergeRecords, "mergeSelectedRecords"));

    menu.addMenuItem("dnd", MenuSourceRunnable.createAction("Copy Selected Records", "page_copy",
      hasSelectedRecords, "copySelectedRecords"));

    menu.addMenuItem("dnd", MenuSourceRunnable.createAction("Paste New Records", "paste_plain",
      new AndEnableCheck(canAdd, canPaste), "pasteRecords"));

    menu.addMenuItem("layer", 0, MenuSourceRunnable.createAction("Layer Style", "palette",
      new AndEnableCheck(exists, hasGeometry), "showProperties", "Style"));

    // menu.addMenuItem("edit", 0, MenuSourceRunnable.createAction(
    // "Export Records", "disk", new AndEnableCheck(exists, hasSelectedRecords),
    // "exportRecords"));
  }

  public static void addVisibleLayers(final List<AbstractRecordLayer> layers,
    final LayerGroup group) {
    if (group.isExists() && group.isVisible()) {
      for (final Layer layer : group) {
        if (layer instanceof LayerGroup) {
          final LayerGroup layerGroup = (LayerGroup)layer;
          addVisibleLayers(layers, layerGroup);
        } else if (layer instanceof AbstractRecordLayer) {
          if (layer.isExists() && layer.isVisible()) {
            final AbstractRecordLayer recordLayer = (AbstractRecordLayer)layer;
            layers.add(recordLayer);
          }
        }
      }
    }
  }

  public static boolean containsSame(final Collection<? extends LayerRecord> records,
    final LayerRecord record) {
    if (record != null) {
      if (records != null) {
        synchronized (records) {
          for (final LayerRecord queryRecord : records) {
            if (queryRecord.isSame(record)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  public static LayerRecord getAndRemoveSame(final Collection<? extends LayerRecord> records,
    final LayerRecord record) {
    for (final Iterator<? extends LayerRecord> iterator = records.iterator(); iterator.hasNext();) {
      final LayerRecord queryRecord = iterator.next();
      if (queryRecord.isSame(record)) {
        iterator.remove();
        return queryRecord;
      }
    }
    return null;
  }

  public static List<AbstractRecordLayer> getVisibleLayers(final LayerGroup group) {
    final List<AbstractRecordLayer> layers = new ArrayList<AbstractRecordLayer>();
    addVisibleLayers(layers, group);
    return layers;
  }

  public static int removeSame(final Collection<? extends LayerRecord> records,
    final Collection<? extends LayerRecord> recordsToRemove) {
    int count = 0;
    for (final LayerRecord record : recordsToRemove) {
      if (removeSame(records, record)) {
        count++;
      }
    }
    return count;
  }

  public static boolean removeSame(final Collection<? extends LayerRecord> records,
    final LayerRecord record) {
    for (final Iterator<? extends LayerRecord> iterator = records.iterator(); iterator.hasNext();) {
      final LayerRecord queryRecord = iterator.next();
      if (queryRecord.isSame(record)) {
        iterator.remove();
        return true;
      }
    }
    return false;
  }

  private BoundingBox boundingBox = new BoundingBox();

  private boolean canAddRecords = true;

  private boolean canDeleteRecords = true;

  private boolean canEditRecords = true;

  private final List<String> columnNameOrder = Collections.emptyList();

  private final List<LayerRecord> deletedRecords = new ArrayList<>();

  private Object editSync;

  private List<String> fieldNames = Collections.emptyList();

  private String fieldNamesSetName = ALL;

  private final List<String> fieldNamesSetNames = new ArrayList<>();

  private final Map<String, List<String>> fieldNamesSets = new HashMap<>();

  private final Map<Record, Component> forms = new HashMap<>();

  private final Map<Record, Window> formWindows = new HashMap<>();

  private final List<LayerRecord> highlightedRecords = new ArrayList<>();

  private RecordQuadTree index = new RecordQuadTree();

  private final List<LayerRecord> modifiedRecords = new ArrayList<>();

  private final List<LayerRecord> newRecords = new ArrayList<>();

  private Query query = new Query();

  private RecordDefinition recordDefinition;

  private final List<LayerRecord> selectedRecords = new ArrayList<>();

  private RecordQuadTree selectedRecordsIndex;

  private boolean snapToAllLayers = false;

  private boolean useFieldTitles = false;

  private Set<String> userReadOnlyFieldNames = new LinkedHashSet<>();

  public AbstractRecordLayer() {
    this("");
  }

  public AbstractRecordLayer(final Map<String, ? extends Object> properties) {
    setReadOnly(false);
    setSelectSupported(true);
    setQuerySupported(true);
    setRenderer(new GeometryStyleRenderer(this));
    if (!properties.containsKey("style")) {
      final GeometryStyleRenderer renderer = getRenderer();
      renderer.setStyle(GeometryStyle.createStyle());
    }
    setProperties(properties);
    if (this.fieldNamesSets.isEmpty()) {
      setFieldNamesSets(null);
    }
  }

  public AbstractRecordLayer(final RecordDefinition recordDefinition) {
    this(recordDefinition.getName());
    setRecordDefinition(recordDefinition);
  }

  public AbstractRecordLayer(final String name) {
    this(name, GeometryFactory.floating3(4326));
    setReadOnly(false);
    setSelectSupported(true);
    setQuerySupported(true);
    setRenderer(new GeometryStyleRenderer(this));
  }

  public AbstractRecordLayer(final String name, final GeometryFactory geometryFactory) {
    super(name);
    setFieldNamesSets(null);
    setGeometryFactory(geometryFactory);
  }

  @Override
  public void addComplete(final AbstractOverlay overlay, final Geometry geometry) {
    if (geometry != null) {
      final RecordDefinition recordDefinition = getRecordDefinition();
      final String geometryFieldName = recordDefinition.getGeometryFieldName();
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put(geometryFieldName, geometry);
      showAddForm(parameters);
    }
  }

  protected void addHighlightedRecord(final LayerRecord record) {
    if (isLayerRecord(record)) {
      synchronized (this.highlightedRecords) {
        if (!containsSame(this.highlightedRecords, record)) {
          this.highlightedRecords.add(record);
        }
      }
    }
  }

  public void addHighlightedRecords(final Collection<? extends LayerRecord> records) {
    synchronized (this.highlightedRecords) {
      for (final LayerRecord record : records) {
        addHighlightedRecord(record);
      }
    }
    fireHighlighted();
  }

  public void addHighlightedRecords(final LayerRecord... records) {
    addHighlightedRecords(Arrays.asList(records));
  }

  protected void addModifiedRecord(final LayerRecord record) {
    synchronized (this.modifiedRecords) {
      if (!containsSame(this.modifiedRecords, record)) {
        this.modifiedRecords.add(record);
      }
    }
  }

  public void addNewRecord() {
    final RecordDefinition recordDefinition = getRecordDefinition();
    final FieldDefinition geometryField = recordDefinition.getGeometryField();
    if (geometryField == null) {
      showAddForm(null);
    } else {
      final MapPanel map = MapPanel.get(this);
      if (map != null) {
        final EditGeometryOverlay addGeometryOverlay = map.getMapOverlay(EditGeometryOverlay.class);
        synchronized (addGeometryOverlay) {
          clearSelectedRecords();
          addGeometryOverlay.addRecord(this, this);
        }
      }
    }
  }

  protected void addSelectedRecord(final LayerRecord record) {
    if (isLayerRecord(record)) {
      synchronized (this.selectedRecords) {
        if (!containsSame(this.selectedRecords, record)) {
          this.selectedRecords.add(record);
        }
      }
    }
  }

  public void addSelectedRecords(final BoundingBox boundingBox) {
    if (isSelectable()) {
      final List<LayerRecord> records = query(boundingBox);
      for (final Iterator<LayerRecord> iterator = records.iterator(); iterator.hasNext();) {
        final LayerRecord layerRecord = iterator.next();
        if (!isVisible(layerRecord) || internalIsDeleted(layerRecord)) {
          iterator.remove();
        }
      }
      addSelectedRecords(records);
      if (!this.selectedRecords.isEmpty()) {
        showRecordsTable(RecordLayerTableModel.MODE_SELECTED);
      }
    }
  }

  public void addSelectedRecords(final Collection<? extends LayerRecord> records) {
    for (final LayerRecord record : records) {
      addSelectedRecord(record);
    }
    clearSelectedRecordsIndex();
    fireSelected();
  }

  public void addSelectedRecords(final LayerRecord... records) {
    addSelectedRecords(Arrays.asList(records));
  }

  public void addToIndex(final Collection<? extends LayerRecord> records) {
    for (final LayerRecord record : records) {
      addToIndex(record);
    }
  }

  public void addToIndex(final LayerRecord record) {
    if (record != null) {
      final Geometry geometry = record.getGeometry();
      if (geometry != null && !geometry.isEmpty()) {
        final RecordQuadTree index = getIndex();
        index.insert(record);
      }
    }
  }

  public void addUserReadOnlyFieldNames(final Collection<String> userReadOnlyFieldNames) {
    if (userReadOnlyFieldNames != null) {
      this.userReadOnlyFieldNames.addAll(userReadOnlyFieldNames);
    }
  }

  public void cancelChanges() {
    synchronized (this.getEditSync()) {
      boolean cancelled = true;
      try (
        Enabled enabled = eventsDisabled()) {
        cancelled &= internalCancelChanges(this.newRecords);
        cancelled &= internalCancelChanges(this.deletedRecords);
        cancelled &= internalCancelChanges(this.modifiedRecords);
      } finally {
        fireRecordsChanged();
      }
      if (!cancelled) {
        JOptionPane.showMessageDialog(MapPanel.get(this),
          "<html><p>There was an error cancelling changes for one or more records.</p>" + "<p>"
            + getPath() + "</p>" + "<p>Check the logging panel for details.</html>",
          "Error Cancelling Changes", JOptionPane.ERROR_MESSAGE);
      }

    }
  }

  public boolean canPasteRecordGeometry(final LayerRecord record) {
    final Geometry geometry = getPasteRecordGeometry(record, false);

    return geometry != null;
  }

  public void clearSelectedRecords() {
    synchronized (this.selectedRecords) {
      this.selectedRecords.clear();
      clearSelectedRecordsIndex();
    }
    synchronized (this.highlightedRecords) {
      this.highlightedRecords.clear();
    }
    fireSelected();
  }

  protected void clearSelectedRecordsIndex() {
    this.selectedRecordsIndex = null;
  }

  @SuppressWarnings("unchecked")
  public <V extends LayerRecord> V copyRecord(final V record) {
    final LayerRecord copy = createRecord(record);
    return (V)copy;
  }

  public void copyRecordGeometry(final LayerRecord record) {
    final Geometry geometry = record.getGeometry();
    if (geometry != null) {
      final StringTransferable transferable = new StringTransferable(DataFlavor.stringFlavor,
        geometry.toString());
      ClipboardUtil.setContents(transferable);
    }
  }

  public void copyRecordsToClipboard(final List<LayerRecord> records) {
    if (!records.isEmpty()) {
      final RecordDefinition recordDefinition = getRecordDefinition();
      final List<Record> copies = new ArrayList<Record>();
      for (final LayerRecord record : records) {
        copies.add(new ArrayRecord(record));
      }
      final RecordReader reader = new ListRecordReader(recordDefinition, copies);
      final RecordReaderTransferable transferable = new RecordReaderTransferable(reader);
      ClipboardUtil.setContents(transferable);
    }
  }

  public void copySelectedRecords() {
    final List<LayerRecord> selectedRecords = getSelectedRecords();
    copyRecordsToClipboard(selectedRecords);
  }

  protected RecordLayerForm createDefaultForm(final LayerRecord record) {
    return new RecordLayerForm(this, record);
  }

  public RecordLayerForm createForm(final LayerRecord record) {
    final String formFactoryExpression = getProperty(FORM_FACTORY_EXPRESSION);
    if (Property.hasValue(formFactoryExpression)) {
      try {
        final SpelExpressionParser parser = new SpelExpressionParser();
        final Expression expression = parser.parseExpression(formFactoryExpression);
        final EvaluationContext context = new StandardEvaluationContext(this);
        context.setVariable("object", record);
        return expression.getValue(context, RecordLayerForm.class);
      } catch (final Throwable e) {
        LoggerFactory.getLogger(getClass()).error("Unable to create form for " + this, e);
        return null;
      }
    } else {
      return createDefaultForm(record);
    }
  }

  @Override
  public TabbedValuePanel createPropertiesPanel() {
    final TabbedValuePanel propertiesPanel = super.createPropertiesPanel();
    createPropertiesPanelFields(propertiesPanel);
    createPropertiesPanelFieldNamesSet(propertiesPanel);
    createPropertiesPanelStyle(propertiesPanel);
    createPropertiesPanelSnapping(propertiesPanel);
    return propertiesPanel;
  }

  protected void createPropertiesPanelFieldNamesSet(final TabbedValuePanel propertiesPanel) {
    final FieldNamesSetPanel panel = new FieldNamesSetPanel(this);
    propertiesPanel.addTab("Field Sets", panel);
  }

  protected void createPropertiesPanelFields(final TabbedValuePanel propertiesPanel) {
    final RecordDefinition recordDefinition = getRecordDefinition();
    final BaseJTable fieldTable = RecordDefinitionTableModel.createTable(recordDefinition);

    final BasePanel fieldPanel = new BasePanel(new BorderLayout());
    fieldPanel.setPreferredSize(new Dimension(500, 400));
    final JScrollPane fieldScroll = new JScrollPane(fieldTable);
    fieldPanel.add(fieldScroll, BorderLayout.CENTER);
    propertiesPanel.addTab("Fields", fieldPanel);
  }

  protected void createPropertiesPanelSnapping(final TabbedValuePanel propertiesPanel) {
    final SnapLayersPanel panel = new SnapLayersPanel(this);
    propertiesPanel.addTab("Snapping", panel);
  }

  protected void createPropertiesPanelStyle(final TabbedValuePanel propertiesPanel) {
    if (getRenderer() != null) {
      final LayerStylePanel stylePanel = new LayerStylePanel(this);
      propertiesPanel.addTab("Style", stylePanel);
    }
  }

  public UndoableEdit createPropertyEdit(final LayerRecord record, final String propertyName,
    final Object oldValue, final Object newValue) {
    return new SetObjectProperty(record, propertyName, oldValue, newValue);
  }

  public LayerRecord createRecord() {
    final Map<String, Object> values = Collections.emptyMap();
    return createRecord(values);
  }

  public LayerRecord createRecord(final Map<String, Object> values) {
    if (!isReadOnly() && isEditable() && isCanAddRecords()) {
      final LayerRecord record = createRecord(getRecordDefinition());
      record.setState(RecordState.Initalizing);
      try {
        if (values != null && !values.isEmpty()) {
          record.setValuesByPath(values);
          record.setIdValue(null);
        }
      } finally {
        record.setState(RecordState.New);
      }
      synchronized (this.newRecords) {
        this.newRecords.add(record);
      }
      fireRecordInserted(record);
      return record;
    } else {
      return null;
    }
  }

  @Override
  public LayerRecord createRecord(final RecordDefinition recordDefinition) {
    if (recordDefinition.equals(getRecordDefinition())) {
      return new LayerRecord(this);
    } else {
      throw new IllegalArgumentException("Cannot create records for " + recordDefinition);
    }
  }

  public RecordLayerTablePanel createTablePanel() {
    final RecordLayerTable table = RecordLayerTableModel.createTable(this);
    if (table == null) {
      return null;
    } else {
      return new RecordLayerTablePanel(this, table);
    }
  }

  @Override
  protected Component createTableViewComponent() {
    return createTablePanel();
  }

  @Override
  public void delete() {
    super.delete();
    if (this.forms != null) {
      for (final Window window : this.formWindows.values()) {
        if (window != null) {
          Invoke.later(window, "dispose");
        }
      }
      for (final Component form : this.forms.values()) {
        if (form != null) {
          if (form instanceof RecordLayerForm) {
            final RecordLayerForm recordForm = (RecordLayerForm)form;
            Invoke.later(recordForm, "destroy");
          }
        }
      }
    }
    this.fieldNamesSetNames.clear();
    this.fieldNamesSets.clear();
    this.deletedRecords.clear();
    this.forms.clear();
    this.formWindows.clear();
    this.highlightedRecords.clear();
    this.index.clear();
    this.modifiedRecords.clear();
    this.newRecords.clear();
    this.selectedRecords.clear();
    if (this.selectedRecordsIndex != null) {
      this.selectedRecordsIndex.clear();
    }
  }

  public void deleteRecord(final LayerRecord record) {
    final boolean trackDeletions = true;
    deleteRecord(record, trackDeletions);
    fireRecordDeleted(record);
  }

  protected void deleteRecord(final LayerRecord record, final boolean trackDeletions) {
    if (isLayerRecord(record)) {
      unSelectRecords(record);
      clearSelectedRecordsIndex();
      synchronized (this.newRecords) {
        if (!removeSame(this.newRecords, record)) {
          synchronized (this.modifiedRecords) {
            removeSame(this.modifiedRecords, record);
          }
          if (trackDeletions) {
            synchronized (this.deletedRecords) {
              if (!containsSame(this.deletedRecords, record)) {
                this.deletedRecords.add(record);
              }
            }
          }
        }
      }
      record.setState(RecordState.Deleted);
      unSelectRecords(record);
      removeFromIndex(record);
    }
  }

  public void deleteRecords(final Collection<? extends LayerRecord> records) {
    if (isCanDeleteRecords()) {
      synchronized (this.getEditSync()) {
        unSelectRecords(records);
        for (final LayerRecord record : records) {
          deleteRecord(record);
        }
      }
    } else {
      synchronized (this.getEditSync()) {
        for (final LayerRecord record : records) {
          synchronized (this.newRecords) {
            if (removeSame(this.newRecords, record)) {
              unSelectRecords(record);
              record.setState(RecordState.Deleted);
            }
          }
        }
      }
    }
  }

  public void deleteRecords(final LayerRecord... records) {
    deleteRecords(Arrays.asList(records));
  }

  public void deleteSelectedRecords() {
    final List<LayerRecord> selectedRecords = getSelectedRecords();
    deleteRecords(selectedRecords);
  }

  protected List<LayerRecord> doQuery(final BoundingBox boundingBox) {
    return Collections.emptyList();
  }

  protected List<LayerRecord> doQuery(final Geometry geometry, final double maxDistance) {
    return Collections.emptyList();
  }

  protected abstract List<LayerRecord> doQuery(final Query query);

  protected List<LayerRecord> doQueryBackground(final BoundingBox boundingBox) {
    return doQuery(boundingBox);
  }

  @Override
  protected boolean doSaveChanges() {
    throw new UnsupportedOperationException();
  }

  protected boolean doSaveChanges(final RecordSaveErrorTableModel errors) {
    if (isExists()) {
      boolean saved = true;
      try {
        final Collection<LayerRecord> deletedRecords = getDeletedRecords();
        saved &= doSaveChanges(errors, deletedRecords);

        final Collection<LayerRecord> modifiedRecords = getModifiedRecords();
        saved &= doSaveChanges(errors, modifiedRecords);

        final List<LayerRecord> newRecords = getNewRecords();
        saved &= doSaveChanges(errors, newRecords);
      } finally {
        fireRecordsChanged();
      }
      return saved;
    } else {
      return false;
    }
  }

  private boolean doSaveChanges(final RecordSaveErrorTableModel errors,
    final Collection<LayerRecord> records) {
    boolean saved = true;
    for (final LayerRecord record : new ArrayList<>(records)) {
      try {
        if (!internalSaveChanges(errors, record)) {
          errors.addRecord(record, "Unknown error");
          saved = false;
        }
      } catch (final Throwable t) {
        errors.addRecord(record, t);
      }
    }
    return saved;
  }

  protected boolean doSaveChanges(final RecordSaveErrorTableModel errors,
    final LayerRecord record) {
    return false;
  }

  @SuppressWarnings("unchecked")
  protected <V extends LayerRecord> List<V> filterQueryResults(final List<V> results,
    final Predicate<Map<String, Object>> filter) {
    final List<LayerRecord> modifiedRecords = new ArrayList<LayerRecord>(getModifiedRecords());
    for (final ListIterator<V> iterator = results.listIterator(); iterator.hasNext();) {
      final LayerRecord record = iterator.next();
      if (internalIsDeleted(record)) {
        iterator.remove();
      } else {
        final V modifiedRecord = (V)getAndRemoveSame(modifiedRecords, record);
        if (modifiedRecord != null) {
          if (Condition.test(filter, modifiedRecord)) {
            iterator.set(modifiedRecord);
          } else {
            iterator.remove();
          }
        }
      }
    }
    for (final LayerRecord record : modifiedRecords) {
      if (Condition.test(filter, record)) {
        results.add((V)record);
      }
    }
    for (final LayerRecord record : getNewRecords()) {
      if (Condition.test(filter, record)) {
        results.add((V)record);
      }
    }
    return results;
  }

  protected <V extends LayerRecord> List<V> filterQueryResults(final List<V> results,
    final Query query) {
    final Condition filter = query.getWhereCondition();
    return filterQueryResults(results, filter);
  }

  protected void fireHighlighted() {
    final int highlightedCount = getHighlightedCount();
    final boolean highlighted = highlightedCount > 0;
    firePropertyChange("hasHighlightedRecords", !highlighted, highlighted);
    firePropertyChange("highlightedCount", -1, highlightedCount);
  }

  public void fireRecordDeleted(final LayerRecord record) {
    firePropertyChange("recordDeleted", null, record);
  }

  protected void fireRecordInserted(final LayerRecord record) {
    firePropertyChange("recordInserted", null, record);
  }

  protected void fireRecordsChanged() {
    firePropertyChange("recordsChanged", false, true);
  }

  protected void fireRecordUpdated(final LayerRecord record) {
    firePropertyChange("recordUpdated", null, record);
  }

  protected void fireSelected() {
    final int selectionCount = this.selectedRecords.size();
    final boolean selected = selectionCount > 0;
    firePropertyChange("hasSelectedRecords", !selected, selected);
    firePropertyChange("selectionCount", -1, selectionCount);
  }

  @Override
  public BoundingBox getBoundingBox() {
    return this.boundingBox;
  }

  public int getChangeCount() {
    int changeCount = 0;
    synchronized (this.newRecords) {
      changeCount += this.newRecords.size();
    }
    synchronized (this.modifiedRecords) {
      changeCount += this.modifiedRecords.size();
    }
    synchronized (this.deletedRecords) {
      changeCount += this.deletedRecords.size();
    }
    return changeCount;
  }

  public List<LayerRecord> getChanges() {
    synchronized (this.getEditSync()) {
      final List<LayerRecord> records = new ArrayList<LayerRecord>();
      synchronized (this.newRecords) {
        records.addAll(this.newRecords);
      }
      synchronized (this.modifiedRecords) {
        records.addAll(this.modifiedRecords);
      }
      synchronized (this.deletedRecords) {
        records.addAll(this.deletedRecords);
      }
      return records;
    }
  }

  public CoordinateSystem getCoordinateSystem() {
    return getGeometryFactory().getCoordinateSystem();
  }

  public int getDeletedRecordCount() {
    synchronized (this.deletedRecords) {
      return this.deletedRecords.size();
    }
  }

  public Collection<LayerRecord> getDeletedRecords() {
    synchronized (this.deletedRecords) {
      return new ArrayList<LayerRecord>(this.deletedRecords);
    }
  }

  public synchronized Object getEditSync() {
    if (this.editSync == null) {
      this.editSync = new Object();
    }
    return this.editSync;
  }

  public List<String> getFieldNames() {
    return new ArrayList<>(this.fieldNames);
  }

  public List<String> getFieldNamesSet() {
    return getFieldNamesSet(this.fieldNamesSetName);
  }

  public List<String> getFieldNamesSet(final String fieldNamesSetName) {
    if (Property.hasValue(fieldNamesSetName)) {
      List<String> fieldNames = this.fieldNamesSets.get(fieldNamesSetName.toUpperCase());
      if (Property.hasValue(fieldNames)) {
        fieldNames = new ArrayList<>(fieldNames);
        if (Property.hasValue(this.fieldNames)) {
          fieldNames.retainAll(this.fieldNames);
        }
        return fieldNames;
      }
    }
    return getFieldNames();
  }

  public String getFieldNamesSetName() {
    return this.fieldNamesSetName;
  }

  public List<String> getFieldNamesSetNames() {
    return new ArrayList<>(this.fieldNamesSetNames);
  }

  public Map<String, List<String>> getFieldNamesSets() {
    final Map<String, List<String>> fieldNamesSets = new LinkedHashMap<>();
    for (final String fieldNamesSetName : getFieldNamesSetNames()) {
      final List<String> fieldNames = getFieldNamesSet(fieldNamesSetName);
      fieldNamesSets.put(fieldNamesSetName, fieldNames);
    }
    return fieldNamesSets;
  }

  public String getFieldTitle(final String fieldName) {
    if (isUseFieldTitles()) {
      final RecordDefinition recordDefinition = getRecordDefinition();
      return recordDefinition.getFieldTitle(fieldName);
    } else {
      return fieldName;
    }
  }

  public String getGeometryFieldName() {
    if (this.recordDefinition == null) {
      return "";
    } else {
      return getRecordDefinition().getGeometryFieldName();
    }
  }

  public DataType getGeometryType() {
    final RecordDefinition recordDefinition = getRecordDefinition();
    if (recordDefinition == null) {
      return null;
    } else {
      final FieldDefinition geometryField = recordDefinition.getGeometryField();
      if (geometryField == null) {
        return null;
      } else {
        return geometryField.getType();
      }
    }
  }

  public BoundingBox getHighlightedBoundingBox() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    BoundingBox boundingBox = geometryFactory.boundingBox();
    for (final Record record : getHighlightedRecords()) {
      final Geometry geometry = record.getGeometry();
      boundingBox = boundingBox.expandToInclude(geometry);
    }
    return boundingBox;
  }

  public int getHighlightedCount() {
    return this.highlightedRecords.size();
  }

  public Collection<LayerRecord> getHighlightedRecords() {
    synchronized (this.highlightedRecords) {
      return new ArrayList<LayerRecord>(this.highlightedRecords);
    }
  }

  public String getIdFieldName() {
    return getRecordDefinition().getIdFieldName();
  }

  public RecordQuadTree getIndex() {
    return this.index;
  }

  public List<LayerRecord> getMergeableSelectedRecords() {
    final List<LayerRecord> selectedRecords = getSelectedRecords();
    for (final ListIterator<LayerRecord> iterator = selectedRecords.listIterator(); iterator
      .hasNext();) {
      final LayerRecord record = iterator.next();
      if (record.isDeleted()) {
        iterator.remove();
      }
    }
    return selectedRecords;
  }

  /**
   * Get a record containing the values of the two records if they can be merged. The
   * new record is not a layer data object so would need to be added, likewise the old records
   * are not removed so they would need to be deleted.
   *
   * @param point
   * @param record1
   * @param record2
   * @return
   */
  public Record getMergedRecord(final Coordinates point, final Record record1,
    final Record record2) {
    if (record1 == record2) {
      return record1;
    } else {
      final String sourceIdFieldName = getIdFieldName();
      final Object id1 = record1.getValue(sourceIdFieldName);
      final Object id2 = record2.getValue(sourceIdFieldName);
      int compare = 0;
      if (id1 == null) {
        if (id2 != null) {
          compare = 1;
        }
      } else if (id2 == null) {
        compare = -1;
      } else {
        compare = CompareUtil.compare(id1, id2);
      }
      if (compare == 0) {
        final Geometry geometry1 = record1.getGeometry();
        final Geometry geometry2 = record2.getGeometry();
        final double length1 = geometry1.getLength();
        final double length2 = geometry2.getLength();
        if (length1 > length2) {
          compare = -1;
        } else {
          compare = 1;
        }
      }
      if (compare > 0) {
        return getMergedRecord(point, record2, record1);
      } else {
        final DirectionalFieldsOld property = DirectionalFieldsOld
          .getProperty(getRecordDefinition());
        final Map<String, Object> newValues = property.getMergedMap(point, record1, record2);
        newValues.remove(getIdFieldName());
        return new ArrayRecord(getRecordDefinition(), newValues);
      }
    }
  }

  public Collection<LayerRecord> getModifiedRecords() {
    return new ArrayList<LayerRecord>(this.modifiedRecords);
  }

  public int getNewRecordCount() {
    return this.newRecords.size();
  }

  public List<LayerRecord> getNewRecords() {
    synchronized (this.newRecords) {
      return new ArrayList<LayerRecord>(this.newRecords);
    }
  }

  protected Geometry getPasteRecordGeometry(final LayerRecord record, final boolean alert) {
    try {
      if (record == null) {
        return null;
      } else {
        RecordReader reader = ClipboardUtil
          .getContents(RecordReaderTransferable.DATA_OBJECT_READER_FLAVOR);
        if (reader == null) {
          final String string = ClipboardUtil.getContents(DataFlavor.stringFlavor);
          if (Property.hasValue(string)) {
            final Resource resource = new ByteArrayResource("t.csv", string);
            reader = RecordReader.create(resource);
          } else {
            return null;
          }
        }
        if (reader != null) {
          final MapPanel parentComponent = MapPanel.get(getProject());
          final RecordDefinition recordDefinition = getRecordDefinition();
          final FieldDefinition geometryAttribute = recordDefinition.getGeometryField();
          if (geometryAttribute != null) {
            DataType geometryDataType = null;
            Class<?> layerGeometryClass = null;
            final GeometryFactory geometryFactory = getGeometryFactory();
            geometryDataType = geometryAttribute.getType();
            layerGeometryClass = geometryDataType.getJavaClass();

            Geometry geometry = null;
            for (final Record sourceRecord : reader) {
              if (geometry == null) {
                final Geometry sourceGeometry = sourceRecord.getGeometry();
                if (sourceGeometry == null) {
                  if (alert) {
                    JOptionPane.showMessageDialog(parentComponent,
                      "Clipboard does not contain a record with a geometry.", "Paste Geometry",
                      JOptionPane.ERROR_MESSAGE);
                  }
                  return null;
                }
                geometry = geometryFactory.createGeometry(layerGeometryClass, sourceGeometry);
                if (geometry == null) {
                  if (alert) {
                    JOptionPane.showMessageDialog(parentComponent,
                      "Clipboard should contain a record with a " + geometryDataType + " not a "
                        + sourceGeometry.getGeometryType() + ".",
                      "Paste Geometry", JOptionPane.ERROR_MESSAGE);
                  }
                  return null;
                }
              } else {
                if (alert) {
                  JOptionPane.showMessageDialog(parentComponent,
                    "Clipboard contains more than one record. Copy a single record.",
                    "Paste Geometry", JOptionPane.ERROR_MESSAGE);
                }
                return null;
              }
            }
            if (geometry == null) {
              if (alert) {
                JOptionPane.showMessageDialog(parentComponent,
                  "Clipboard does not contain a record with a geometry.", "Paste Geometry",
                  JOptionPane.ERROR_MESSAGE);
              }
            } else if (geometry.isEmpty()) {
              if (alert) {
                JOptionPane.showMessageDialog(parentComponent,
                  "Clipboard contains an empty geometry.", "Paste Geometry",
                  JOptionPane.ERROR_MESSAGE);
              }
              return null;
            } else {
              return geometry;
            }
          }
        }
        return null;
      }
    } catch (final Throwable t) {
      return null;
    }
  }

  public Query getQuery() {
    if (this.query == null) {
      return null;
    } else {
      return this.query.clone();
    }
  }

  public LayerRecord getRecord(final int row) {
    throw new UnsupportedOperationException();
  }

  public LayerRecord getRecordById(final Object id) {
    return null;
  }

  public RecordDefinition getRecordDefinition() {
    return this.recordDefinition;
  }

  public List<LayerRecord> getRecords() {
    throw new UnsupportedOperationException();
  }

  public RecordStore getRecordStore() {
    return getRecordDefinition().getRecordStore();
  }

  public int getRowCount() {
    final RecordDefinition recordDefinition = getRecordDefinition();
    final Query query = new Query(recordDefinition);
    return getRowCount(query);
  }

  public int getRowCount(final Query query) {
    LoggerFactory.getLogger(getClass()).error("Get row count not implemented");
    return 0;
  }

  @Override
  public BoundingBox getSelectedBoundingBox() {
    BoundingBox boundingBox = super.getSelectedBoundingBox();
    for (final Record record : getSelectedRecords()) {
      final Geometry geometry = record.getGeometry();
      boundingBox = boundingBox.expandToInclude(geometry);
    }
    return boundingBox;
  }

  public List<LayerRecord> getSelectedRecords() {
    synchronized (this.selectedRecords) {
      return new ArrayList<LayerRecord>(this.selectedRecords);
    }
  }

  @SuppressWarnings({
    "rawtypes", "unchecked"
  })
  public List<LayerRecord> getSelectedRecords(final BoundingBox boundingBox) {
    final RecordQuadTree index = getSelectedRecordsIndex();
    return (List)index.queryIntersects(boundingBox);
  }

  protected RecordQuadTree getSelectedRecordsIndex() {
    if (this.selectedRecordsIndex == null) {
      final List<LayerRecord> selectedRecords = getSelectedRecords();
      final RecordQuadTree index = new RecordQuadTree(getProject().getGeometryFactory(),
        selectedRecords);
      this.selectedRecordsIndex = index;
    }
    return this.selectedRecordsIndex;
  }

  public int getSelectionCount() {
    return this.selectedRecords.size();
  }

  public Collection<String> getSnapLayerPaths() {
    return getProperty("snapLayers", Collections.<String> emptyList());
  }

  public String getTypePath() {
    final RecordDefinition recordDefinition = getRecordDefinition();
    if (recordDefinition == null) {
      return null;
    } else {
      return recordDefinition.getPath();
    }
  }

  public Collection<String> getUserReadOnlyFieldNames() {
    return Collections.unmodifiableSet(this.userReadOnlyFieldNames);
  }

  public boolean hasGeometryAttribute() {
    return getRecordDefinition().getGeometryField() != null;
  }

  protected boolean hasPermission(final String permission) {
    if (this.recordDefinition == null) {
      return true;
    } else {
      final Collection<String> permissions = this.recordDefinition.getProperty("permissions");
      if (permissions == null) {
        return true;
      } else {
        final boolean hasPermission = permissions.contains(permission);
        return hasPermission;
      }
    }
  }

  /**
   * Cancel changes for one of the lists of changes {@link #deletedRecords},
   *  {@link #newRecords}, {@link #modifiedRecords}.
   * @param records
   */
  private boolean internalCancelChanges(final Collection<LayerRecord> records) {
    boolean cancelled = true;
    for (final LayerRecord record : new ArrayList<LayerRecord>(records)) {
      final boolean selected = isSelected(record);
      removeForm(record);
      removeFromIndex(record);
      try {
        final LayerRecord originalRecord = internalCancelChanges(record);
        if (originalRecord == null) {
          unSelectRecords(record);
        } else {
          addToIndex(originalRecord);
          if (selected) {
            if (originalRecord != record) {
              unSelectRecords(record);
              addSelectedRecords(record);
            }
          }
        }
      } catch (final Throwable e) {
        ExceptionUtil.log(getClass(), "Unable to cancel changes.\n" + record, e);
        cancelled = false;
      } finally {
        records.remove(record);
      }
    }
    return cancelled;
  }

  /**
   * Revert the values of the record to the last values loaded from the database
   * @param record
   */
  protected LayerRecord internalCancelChanges(final LayerRecord record) {
    if (record != null) {
      final boolean isNew = record.getState() == RecordState.New;
      record.cancelChanges();
      if (!isNew) {
        return record;
      }
    }
    return null;
  }

  protected boolean internalIsDeleted(final LayerRecord record) {
    if (record == null) {
      return true;
    } else if (record.getState() == RecordState.Deleted) {
      return true;
    } else {
      return containsSame(this.deletedRecords, record);
    }
  }

  /**
   * Revert the values of the record to the last values loaded from the database
   * @param record
   */
  protected LayerRecord internalPostSaveChanges(final LayerRecord record) {
    if (record != null) {

      return record;
    }
    return null;
  }

  protected boolean internalSaveChanges(final RecordSaveErrorTableModel errors,
    final LayerRecord record) {
    final RecordState originalState = record.getState();
    final boolean saved = doSaveChanges(errors, record);
    if (saved) {
      postSaveChanges(originalState, record);
    }
    return saved;
  }

  public boolean isCanAddRecords() {
    return !super.isReadOnly() && isEditable() && this.canAddRecords && hasPermission("INSERT");
  }

  public boolean isCanDeleteRecords() {
    return !super.isReadOnly() && isEditable() && this.canDeleteRecords && hasPermission("DELETE");
  }

  public boolean isCanEditRecords() {
    return !super.isReadOnly() && isEditable() && this.canEditRecords && hasPermission("UPDATE");
  }

  public boolean isCanMergeRecords() {
    if (isCanAddRecords()) {
      if (isCanDeleteRecords()) {
        if (isCanDeleteRecords()) {
          final DataType geometryType = getGeometryType();
          if (DataTypes.LINE_STRING.equals(geometryType)) {
            if (getMergeableSelectedRecords().size() > 1) {
              return true;
            }
          } // TODO allow merging other type
        }
      }
    }
    return false;
  }

  public boolean isCanPaste() {
    return ClipboardUtil.isDataFlavorAvailable(RecordReaderTransferable.DATA_OBJECT_READER_FLAVOR)
      || ClipboardUtil.isDataFlavorAvailable(DataFlavor.stringFlavor);
  }

  public boolean isDeleted(final LayerRecord record) {
    return internalIsDeleted(record);
  }

  public boolean isEmpty() {
    return getRowCount() + getNewRecordCount() <= 0;
  }

  public boolean isFieldUserReadOnly(final String fieldName) {
    return getUserReadOnlyFieldNames().contains(fieldName);
  }

  @Override
  public boolean isHasChanges() {
    if (isEditable()) {
      synchronized (this.getEditSync()) {
        if (!this.newRecords.isEmpty()) {
          return true;
        } else if (!this.modifiedRecords.isEmpty()) {
          return true;
        } else if (!this.deletedRecords.isEmpty()) {
          return true;
        } else {
          return false;
        }
      }
    } else {
      return false;
    }
  }

  @Override
  public boolean isHasGeometry() {
    return getGeometryFieldName() != null;
  }

  public boolean isHasSelectedRecords() {
    return getSelectionCount() > 0;
  }

  public boolean isHidden(final LayerRecord record) {
    if (isCanDeleteRecords() && isDeleted(record)) {
      return true;
    } else if (isSelectable() && isSelected(record)) {
      return true;
    } else {
      return false;
    }
  }

  public boolean isHighlighted(final LayerRecord record) {
    synchronized (this.highlightedRecords) {
      return containsSame(this.highlightedRecords, record);
    }
  }

  public boolean isLayerRecord(final Record record) {
    if (record == null) {
      return false;
    } else if (record.getRecordDefinition() == getRecordDefinition()) {
      return true;
    } else {
      return false;
    }
  }

  public boolean isModified(final LayerRecord record) {
    synchronized (this.modifiedRecords) {
      return containsSame(this.modifiedRecords, record);
    }
  }

  public boolean isNew(final LayerRecord record) {
    synchronized (this.newRecords) {
      return this.newRecords.contains(record);
    }
  }

  @Override
  public boolean isReadOnly() {
    if (super.isReadOnly()) {
      return true;
    } else {
      if (this.canAddRecords && hasPermission("INSERT")) {
        return false;
      } else if (this.canDeleteRecords && hasPermission("DELETE")) {
        return false;
      } else if (this.canEditRecords && hasPermission("UPDATE")) {
        return false;
      } else {
        return true;
      }
    }
  }

  public boolean isSelected(final LayerRecord record) {
    if (record == null) {
      return false;
    } else {
      synchronized (this.selectedRecords) {
        return containsSame(this.selectedRecords, record);
      }
    }
  }

  public boolean isSnapToAllLayers() {
    return this.snapToAllLayers;
  }

  public boolean isUseFieldTitles() {
    return this.useFieldTitles;
  }

  public boolean isVisible(final LayerRecord record) {
    if (isExists() && isVisible()) {
      final AbstractRecordLayerRenderer renderer = getRenderer();
      if (renderer == null || renderer.isVisible(record)) {
        return true;
      }
    }
    return false;
  }

  public void mergeSelectedRecords() {
    if (isCanMergeRecords()) {
      Invoke.later(MergeRecordsDialog.class, "showDialog", this);
    }
  }

  public void pasteRecordGeometry(final LayerRecord record) {
    final Geometry geometry = getPasteRecordGeometry(record, true);
    if (geometry != null) {
      record.setGeometryValue(geometry);
    }
  }

  public void pasteRecords() {
    final List<LayerRecord> newRecords = new ArrayList<>();
    try (
      Enabled enabled = eventsDisabled()) {
      RecordReader reader = ClipboardUtil
        .getContents(RecordReaderTransferable.DATA_OBJECT_READER_FLAVOR);
      if (reader == null) {
        final String string = ClipboardUtil.getContents(DataFlavor.stringFlavor);
        if (Property.hasValue(string)) {
          if (string.contains("\t")) {
            final Resource tsvResource = new ByteArrayResource("t.tsv", string);
            reader = RecordReader.create(tsvResource);
          } else {
            final Resource resource = new ByteArrayResource("t.csv", string);
            reader = RecordReader.create(resource);
          }
        }
      }
      final List<Record> regectedRecords = new ArrayList<>();
      if (reader != null) {
        final RecordDefinition recordDefinition = getRecordDefinition();
        final FieldDefinition geometryAttribute = recordDefinition.getGeometryField();
        DataType geometryDataType = null;
        Class<?> layerGeometryClass = null;
        final GeometryFactory geometryFactory = getGeometryFactory();
        if (geometryAttribute != null) {
          geometryDataType = geometryAttribute.getType();
          layerGeometryClass = geometryDataType.getJavaClass();
        }
        Collection<String> ignorePasteFields = getProperty("ignorePasteFields");
        if (ignorePasteFields == null) {
          ignorePasteFields = Collections.emptySet();
        }
        for (final Record sourceRecord : reader) {
          final Map<String, Object> newValues = new LinkedHashMap<String, Object>(sourceRecord);

          Geometry sourceGeometry = sourceRecord.getGeometry();
          for (final Iterator<String> iterator = newValues.keySet().iterator(); iterator
            .hasNext();) {
            final String fieldName = iterator.next();
            final FieldDefinition attribute = recordDefinition.getField(fieldName);
            if (attribute == null) {
              iterator.remove();
            } else if (ignorePasteFields != null) {
              if (ignorePasteFields.contains(attribute.getName())) {
                iterator.remove();
              }
            }
          }
          if (geometryDataType != null) {
            if (sourceGeometry == null) {
              final Object value = sourceRecord.getValue(geometryAttribute.getName());
              sourceGeometry = StringConverterRegistry.toObject(Geometry.class, value);
            }
            final Geometry geometry = geometryFactory.createGeometry(layerGeometryClass,
              sourceGeometry);
            if (geometry == null) {
              newValues.clear();
            } else {
              final String geometryFieldName = geometryAttribute.getName();
              newValues.put(geometryFieldName, geometry);
            }
          }
          LayerRecord newRecord = null;
          if (newValues.isEmpty()) {
            regectedRecords.add(sourceRecord);
          } else {
            newRecord = createRecord(newValues);
          }
          if (newRecord == null) {
            regectedRecords.add(sourceRecord);
          } else {
            newRecords.add(newRecord);
          }
        }
      }
      saveChanges(newRecords);
      if (!newRecords.isEmpty()) {
        zoomToRecords(newRecords);
        showRecordsTable(RecordLayerTableModel.MODE_SELECTED);
      }
    }
    firePropertyChange("recordsInserted", null, newRecords);
    addSelectedRecords(newRecords);
  }

  protected void postSaveChanges(final RecordState originalState, final LayerRecord record) {
    postSaveDeletedRecord(record);
    postSaveModifiedRecord(record);
    postSaveNewRecord(record);
  }

  protected boolean postSaveDeletedRecord(final LayerRecord record) {
    boolean deleted;
    synchronized (this.deletedRecords) {
      deleted = this.deletedRecords.remove(record);
    }
    if (deleted) {
      unSelectRecords(record);
      removeFromIndex(record);
      return true;
    } else {
      return false;
    }
  }

  protected boolean postSaveModifiedRecord(final LayerRecord record) {
    synchronized (this.modifiedRecords) {
      return this.modifiedRecords.remove(record);
    }
  }

  protected boolean postSaveNewRecord(final LayerRecord record) {
    boolean removed;
    synchronized (this.newRecords) {
      removed = this.newRecords.remove(record);
    }
    if (removed) {
      addToIndex(record);
      if (isSelected(record)) {
        unSelectRecords(record);
        addSelectedRecords(record);
      }
      return true;
    }
    return false;
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    super.propertyChange(event);
    if (isExists()) {
      final Object source = event.getSource();
      final String propertyName = event.getPropertyName();
      if (!"qaMessagesUpdated".equals(propertyName)) {
        if (source instanceof LayerRecord) {
          final LayerRecord record = (LayerRecord)source;
          if (record.getLayer() == this) {
            if (Equals.equal(propertyName, getGeometryFieldName())) {
              final Geometry oldGeometry = (Geometry)event.getOldValue();
              updateSpatialIndex(record, oldGeometry);
            }
            clearSelectedRecordsIndex();
          }
        }
      }
    }
  }

  @SuppressWarnings({
    "rawtypes", "unchecked"
  })
  public final List<LayerRecord> query(final BoundingBox boundingBox) {
    if (hasGeometryAttribute()) {
      final List<LayerRecord> results = doQuery(boundingBox);
      final Predicate predicate = new OldRecordGeometryIntersectsFilter(boundingBox.toGeometry());
      return filterQueryResults(results, predicate);
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings({
    "rawtypes", "unchecked"
  })
  public List<LayerRecord> query(final Geometry geometry, final double maxDistance) {
    if (hasGeometryAttribute()) {
      final List<LayerRecord> results = doQuery(geometry, maxDistance);
      final Predicate predicate = new OldRecordGeometryDistanceFilter(geometry, maxDistance);
      return filterQueryResults(results, predicate);
    } else {
      return Collections.emptyList();
    }
  }

  public List<LayerRecord> query(final Query query) {
    final List<LayerRecord> results = doQuery(query);
    final Condition condition = query.getWhereCondition();
    // TODO sorting
    return filterQueryResults(results, condition);
  }

  public final List<LayerRecord> queryBackground(final BoundingBox boundingBox) {
    if (hasGeometryAttribute()) {
      final List<LayerRecord> results = doQueryBackground(boundingBox);
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  protected void removeForm(final LayerRecord record) {
    if (record != null) {
      if (SwingUtilities.isEventDispatchThread()) {
        final Component form = this.forms.remove(record);
        if (form != null) {
          if (form instanceof RecordLayerForm) {
            final RecordLayerForm recordForm = (RecordLayerForm)form;
            recordForm.destroy();
          }
        }
        final Window window = this.formWindows.remove(record);
        if (window != null) {
          window.dispose();
        }

      } else {
        Invoke.later(this, "removeForm", record);
      }
    }
  }

  public void removeForms(final Collection<LayerRecord> records) {
    if (records != null && !records.isEmpty()) {
      if (SwingUtilities.isEventDispatchThread()) {
        for (final LayerRecord record : records) {
          removeForm(record);
        }
      } else {
        Invoke.later(this, "removeForms", records);
      }
    }
  }

  @SuppressWarnings({
    "unchecked", "rawtypes"
  })
  public boolean removeFromIndex(final BoundingBox boundingBox, final LayerRecord record) {
    boolean removed = false;
    final RecordQuadTree index = getIndex();
    final List<LayerRecord> records = (List)index.query(boundingBox);
    for (final LayerRecord indexRecord : records) {
      if (indexRecord.isSame(record)) {
        index.remove(indexRecord);
        removed = true;
      }
    }
    return removed;
  }

  public void removeFromIndex(final Collection<? extends LayerRecord> records) {
    for (final LayerRecord record : records) {
      removeFromIndex(record);
    }
  }

  public void removeFromIndex(final LayerRecord record) {
    final Geometry geometry = record.getGeometry();
    if (geometry != null && !geometry.isEmpty()) {
      final BoundingBox boundingBox = BoundingBox.getBoundingBox(geometry);
      removeFromIndex(boundingBox, record);
    }
  }

  protected void removeHighlightedRecord(final LayerRecord record) {
    synchronized (this.highlightedRecords) {
      for (final Iterator<LayerRecord> iterator = this.highlightedRecords.iterator(); iterator
        .hasNext();) {
        final LayerRecord highlightedRecord = iterator.next();
        if (highlightedRecord.isSame(record)) {
          iterator.remove();
        }
      }
    }
  }

  protected void removeSelectedRecord(final LayerRecord record) {
    synchronized (this.selectedRecords) {
      for (final Iterator<LayerRecord> iterator = this.selectedRecords.iterator(); iterator
        .hasNext();) {
        final LayerRecord selectedRecord = iterator.next();
        if (selectedRecord != null && selectedRecord.isSame(record)) {
          iterator.remove();
        }
      }
    }
    removeHighlightedRecord(record);
  }

  public void replaceValues(final LayerRecord record, final Map<String, Object> values) {
    record.setValues(values);
  }

  public void revertChanges(final LayerRecord record) {
    synchronized (this.modifiedRecords) {
      if (isLayerRecord(record)) {
        postSaveModifiedRecord(record);
        synchronized (this.deletedRecords) {
          for (final Iterator<LayerRecord> iterator = this.deletedRecords.iterator(); iterator
            .hasNext();) {
            final LayerRecord deletedRecord = iterator.next();
            if (deletedRecord.isSame(deletedRecord)) {
              iterator.remove();
            }
          }
        }
      }
    }
  }

  @Override
  public boolean saveChanges() {
    synchronized (this.getEditSync()) {
      boolean allSaved = true;
      if (isHasChanges()) {
        final RecordSaveErrorTableModel errors = new RecordSaveErrorTableModel(this);
        try (
          Enabled enabled = eventsDisabled()) {
          doSaveChanges(errors);
        } finally {
          fireRecordsChanged();
          allSaved = errors.showErrorDialog();
        }
      }
      return allSaved;
    }
  }

  public final boolean saveChanges(final Collection<? extends LayerRecord> records) {
    synchronized (this.getEditSync()) {
      boolean allSaved;
      final RecordSaveErrorTableModel errors = new RecordSaveErrorTableModel(this);
      try (
        Enabled enabled = eventsDisabled()) {
        for (final LayerRecord record : records) {
          try {
            if (isLayerRecord(record)) {
              if (!internalSaveChanges(errors, record)) {
                errors.addRecord(record, "Unknown error");
              }
            }
          } catch (final Throwable t) {
            errors.addRecord(record, t);
          }
        }
        fireRecordsChanged();
      } finally {
        allSaved = errors.showErrorDialog();
      }
      return allSaved;
    }
  }

  public final boolean saveChanges(final LayerRecord record) {
    synchronized (this.getEditSync()) {
      boolean allSaved;
      final RecordSaveErrorTableModel errors = new RecordSaveErrorTableModel(this);
      try (
        Enabled enabled = eventsDisabled()) {
        try {
          final boolean saved = internalSaveChanges(errors, record);
          if (!saved) {
            errors.addRecord(record, "Unknown error");
          }
        } catch (final Throwable t) {
          errors.addRecord(record, t);
        }
        fireRecordUpdated(record);
      } finally {
        allSaved = errors.showErrorDialog();
      }
      return allSaved;
    }
  }

  public void setBoundingBox(final BoundingBox boundingBox) {
    this.boundingBox = boundingBox;
  }

  public void setCanAddRecords(final boolean canAddRecords) {
    this.canAddRecords = canAddRecords;
    firePropertyChange("canAddRecords", !isCanAddRecords(), isCanAddRecords());
  }

  public void setCanDeleteRecords(final boolean canDeleteRecords) {
    this.canDeleteRecords = canDeleteRecords;
    firePropertyChange("canDeleteRecords", !isCanDeleteRecords(), isCanDeleteRecords());
  }

  public void setCanEditRecords(final boolean canEditRecords) {
    this.canEditRecords = canEditRecords;
    firePropertyChange("canEditRecords", !isCanEditRecords(), isCanEditRecords());
  }

  @Override
  public void setEditable(final boolean editable) {
    if (SwingUtilities.isEventDispatchThread()) {
      Invoke.background("Set editable", this, "setEditable", editable);
    } else {
      synchronized (this.getEditSync()) {
        if (editable == false) {
          firePropertyChange("preEditable", false, true);
          if (isHasChanges()) {
            final Integer result = InvokeMethodCallable.invokeAndWait(JOptionPane.class,
              "showConfirmDialog", JOptionPane.getRootFrame(),
              "The layer has unsaved changes. Click Yes to save changes. Click No to discard changes. Click Cancel to continue editing.",
              "Save Changes", JOptionPane.YES_NO_CANCEL_OPTION);

            if (result == JOptionPane.YES_OPTION) {
              if (!saveChanges()) {
                return;
              }
            } else if (result == JOptionPane.NO_OPTION) {
              cancelChanges();
            } else {
              // Don't allow state change if cancelled
              return;
            }

          }
        }
        super.setEditable(editable);
        setCanAddRecords(this.canAddRecords);
        setCanDeleteRecords(this.canDeleteRecords);
        setCanEditRecords(this.canEditRecords);
      }
    }
  }

  public void setFieldNamesSetName(final String fieldNamesSetName) {
    final String oldValue = this.fieldNamesSetName;
    if (Property.hasValue(fieldNamesSetName)) {
      this.fieldNamesSetName = fieldNamesSetName;
    } else {
      this.fieldNamesSetName = ALL;
    }
    firePropertyChange("fieldNamesSetName", oldValue, this.fieldNamesSetName);
  }

  public void setFieldNamesSets(final Map<String, List<String>> fieldNamesSets) {
    final List<String> allFieldNames = this.fieldNames;
    this.fieldNamesSetNames.clear();
    this.fieldNamesSetNames.add("All");
    this.fieldNamesSets.clear();
    if (fieldNamesSets != null) {
      for (final Entry<String, List<String>> entry : fieldNamesSets.entrySet()) {
        final String name = entry.getKey();
        if (Property.hasValue(name)) {
          final String upperName = name.toUpperCase();
          final Collection<String> names = entry.getValue();
          if (Property.hasValue(names)) {
            final Set<String> fieldNames = new LinkedHashSet<>(names);
            if (ALL.equalsIgnoreCase(name)) {
              if (Property.hasValue(allFieldNames)) {
                fieldNames.addAll(allFieldNames);
              }
            } else {
              boolean found = false;
              for (final String name2 : this.fieldNamesSetNames) {
                if (name.equalsIgnoreCase(name2)) {
                  found = true;
                  LoggerFactory.getLogger(getClass()).error(
                    "Duplicate field set name " + name + "=" + name2 + " for layer " + getPath());
                }
              }
              if (!found) {
                this.fieldNamesSetNames.add(name);
              }
            }
            if (Property.hasValue(allFieldNames)) {
              fieldNames.retainAll(allFieldNames);
            }
            this.fieldNamesSets.put(upperName, new ArrayList<>(fieldNames));
          }
        }
      }
    }
    getFieldNamesSet(ALL);
    firePropertyChange("fieldNamesSets", null, this.fieldNamesSets);
  }

  @Override
  protected void setGeometryFactory(final GeometryFactory geometryFactory) {
    super.setGeometryFactory(geometryFactory);
    if (geometryFactory != null && this.boundingBox.isEmpty()) {
      final CoordinateSystem coordinateSystem = geometryFactory.getCoordinateSystem();
      if (coordinateSystem != null) {
        this.boundingBox = coordinateSystem.getAreaBoundingBox();
      }
    }
  }

  public void setHighlightedRecords(final Collection<LayerRecord> highlightedRecords) {
    synchronized (this.highlightedRecords) {
      this.highlightedRecords.clear();
      this.highlightedRecords.addAll(highlightedRecords);

    }
    fireHighlighted();
  }

  public void setIndex(final RecordQuadTree index) {
    if (index == null || !isExists()) {
      this.index = new RecordQuadTree();
    } else {
      this.index = index;
    }
  }

  @Override
  public void setProperty(final String name, final Object value) {
    if ("style".equals(name)) {
      if (value instanceof Map) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> style = (Map<String, Object>)value;
        final LayerRenderer<AbstractRecordLayer> renderer = AbstractRecordLayerRenderer
          .getRenderer(this, style);
        if (renderer != null) {
          setRenderer(renderer);
        }
      }
    } else {
      super.setProperty(name, value);
    }
  }

  public void setQuery(final Query query) {
    final Query oldValue = this.query;
    if (query == null) {
      final RecordDefinition recordDefinition = getRecordDefinition();
      if (recordDefinition == null) {
        this.query = null;
      } else {
        this.query = new Query(recordDefinition);
      }
    } else {
      this.query = query;
    }
    firePropertyChange("query", oldValue, this.query);
  }

  protected void setRecordDefinition(final RecordDefinition recordDefinition) {
    this.recordDefinition = recordDefinition;
    if (recordDefinition != null) {
      final FieldDefinition geometryField = recordDefinition.getGeometryField();
      String geometryType = null;
      GeometryFactory geometryFactory;
      if (geometryField == null) {
        geometryFactory = null;
        setVisible(false);
        setSelectSupported(false);
        setRenderer(null);
      } else {
        geometryFactory = recordDefinition.getGeometryFactory();
        geometryType = geometryField.getType().toString();
      }
      setGeometryFactory(geometryFactory);
      final Icon icon = RecordStoreTableTreeNode.getIcon(geometryType);
      setIcon(icon);
      this.fieldNames = recordDefinition.getFieldNames();
      List<String> allFieldNames = this.fieldNamesSets.get(ALL.toUpperCase());
      if (Property.hasValue(allFieldNames)) {
        final Set<String> mergedFieldNames = new LinkedHashSet<>(allFieldNames);
        mergedFieldNames.addAll(this.fieldNames);
        mergedFieldNames.retainAll(this.fieldNames);
        allFieldNames = new ArrayList<>(mergedFieldNames);
      } else {
        allFieldNames = new ArrayList<>(this.fieldNames);
      }
      this.fieldNamesSets.put(ALL.toUpperCase(), allFieldNames);
      if (recordDefinition.getGeometryFieldIndex() == -1) {
        setVisible(false);
        setSelectSupported(false);
        setRenderer(null);
      }
      if (this.query == null) {
        setQuery(null);
      }
    }
  }

  public void setSelectedRecords(final BoundingBox boundingBox) {
    if (isSelectable()) {
      final List<LayerRecord> records = query(boundingBox);
      for (final Iterator<LayerRecord> iterator = records.iterator(); iterator.hasNext();) {
        final LayerRecord layerRecord = iterator.next();
        if (!isVisible(layerRecord) || internalIsDeleted(layerRecord)) {
          iterator.remove();
        }
      }
      setSelectedRecords(records);
      if (!this.selectedRecords.isEmpty()) {
        showRecordsTable(RecordLayerTableModel.MODE_SELECTED);
      }
    }
  }

  public void setSelectedRecords(final Collection<LayerRecord> selectedRecords) {
    synchronized (this.selectedRecords) {
      clearSelectedRecordsIndex();
      this.selectedRecords.clear();
      for (final LayerRecord record : selectedRecords) {
        addSelectedRecord(record);
      }
    }
    synchronized (this.highlightedRecords) {
      this.highlightedRecords.retainAll(selectedRecords);
    }
    fireSelected();
  }

  public void setSelectedRecords(final LayerRecord... selectedRecords) {
    setSelectedRecords(Arrays.asList(selectedRecords));
  }

  public void setSelectedRecordsById(final Object id) {
    final RecordDefinition recordDefinition = getRecordDefinition();
    if (recordDefinition != null) {
      final String idFieldName = recordDefinition.getIdFieldName();
      if (idFieldName == null) {
        clearSelectedRecords();
      } else {
        final Query query = Query.equal(recordDefinition, idFieldName, id);
        final List<LayerRecord> records = query(query);
        setSelectedRecords(records);
      }
    }
  }

  public void setSnapLayerPaths(final Collection<String> snapLayerPaths) {
    if (snapLayerPaths == null || snapLayerPaths.isEmpty()) {
      removeProperty("snapLayers");
    } else {
      setProperty("snapLayers", new TreeSet<String>(snapLayerPaths));
    }
  }

  public void setSnapToAllLayers(final boolean snapToAllLayers) {
    this.snapToAllLayers = snapToAllLayers;
  }

  public void setUseFieldTitles(final boolean useFieldTitles) {
    this.useFieldTitles = useFieldTitles;
  }

  public void setUserReadOnlyFieldNames(final Collection<String> userReadOnlyFieldNames) {
    this.userReadOnlyFieldNames = new LinkedHashSet<String>(userReadOnlyFieldNames);
  }

  public LayerRecord showAddForm(final Map<String, Object> parameters) {
    if (isCanAddRecords()) {
      final LayerRecord newRecord = createRecord(parameters);
      final RecordLayerForm form = createForm(newRecord);
      if (form == null) {
        return null;
      } else {
        form.setAddRecord(newRecord);
        if (form.showAddDialog()) {
          return form.getAddRecord();
        } else {
          return null;
        }
      }
    } else {
      final Window window = SwingUtil.getActiveWindow();
      JOptionPane.showMessageDialog(window,
        "Adding records is not enabled for the " + getPath()
          + " layer. If possible make the layer editable",
        "Cannot Add Record", JOptionPane.ERROR_MESSAGE);
      return null;
    }

  }

  @SuppressWarnings("unchecked")
  public <V extends JComponent> V showForm(final LayerRecord record) {
    if (record == null) {
      return null;
    } else {
      if (SwingUtilities.isEventDispatchThread()) {
        Window window = this.formWindows.get(record);
        if (window == null) {
          final Component form = createForm(record);
          final Object id = record.getIdValue();
          if (form == null) {
            return null;
          } else {
            String title;
            if (record.getState() == RecordState.New) {
              title = "Add NEW " + getName();
            } else if (isCanEditRecords()) {
              title = "Edit " + getName() + " #" + id;
            } else {
              title = "View " + getName() + " #" + id;
              if (form instanceof RecordLayerForm) {
                final RecordLayerForm recordForm = (RecordLayerForm)form;
                recordForm.setEditable(false);
              }
            }
            final Window parent = SwingUtil.getActiveWindow();
            window = new BaseDialog(parent, title);
            window.add(form);
            window.pack();
            if (form instanceof RecordLayerForm) {
              final RecordLayerForm recordForm = (RecordLayerForm)form;
              window.addWindowListener(recordForm);
            }
            SwingUtil.autoAdjustPosition(window);
            final int offset = Math.abs(formCount.incrementAndGet() % 10);
            window.setLocation(50 + offset * 20, 100 + offset * 20);
            this.forms.put(record, form);
            this.formWindows.put(record, window);
            window.addWindowListener(new WindowAdapter() {

              @Override
              public void windowClosing(final WindowEvent e) {
                removeForm(record);
              }
            });
            SwingUtil.setVisible(window, true);

            window.requestFocus();
            return (V)form;
          }
        } else {
          SwingUtil.setVisible(window, true);

          window.requestFocus();
          final Component component = window.getComponent(0);
          if (component instanceof JScrollPane) {
            final JScrollPane scrollPane = (JScrollPane)component;
            return (V)scrollPane.getComponent(0);
          }
          return null;
        }
      } else {
        Invoke.later(this, "showForm", record);
        return null;
      }
    }
  }

  public void showRecordsTable() {
    showRecordsTable(null);
  }

  public void showRecordsTable(final String fieldFilterMode) {
    Invoke.later(() -> {
      final Map<String, Object> config = new LinkedHashMap<>();
      if (!Property.hasValue(fieldFilterMode)) {
        final String mode = Property.getString(this, "fieldFilterMode",
          RecordLayerTableModel.MODE_ALL);
        config.put("fieldFilterMode", mode);
      } else {
        config.put("fieldFilterMode", fieldFilterMode);
      }
      final String geometryFilterMode = Property.getString(this, "geometryFilterMode",
        RecordLayerTableModel.MODE_ALL);
      config.put("geometryFilterMode", geometryFilterMode);

      final RecordLayerTablePanel panel = showTableView();
      panel.setFieldFilterMode(fieldFilterMode);
    });
  }

  public List<LayerRecord> splitRecord(final LayerRecord record,
    final CloseLocation mouseLocation) {

    final Geometry geometry = mouseLocation.getGeometry();
    if (geometry instanceof LineString) {
      final LineString line = (LineString)geometry;
      final int[] vertexIndex = mouseLocation.getVertexIndex();
      final Point point = mouseLocation.getPoint();
      final Point convertedPoint = getGeometryFactory().copy(point);
      final Coordinates coordinates = CoordinatesUtil.get(convertedPoint);
      final LineString line1;
      final LineString line2;

      final int numPoints = line.getNumPoints();
      if (vertexIndex == null) {
        final int pointIndex = mouseLocation.getSegmentIndex()[0];
        line1 = LineStringUtil.subLineString(line, null, 0, pointIndex + 1, coordinates);
        line2 = LineStringUtil.subLineString(line, coordinates, pointIndex + 1,
          numPoints - pointIndex - 1, null);
      } else {
        final int pointIndex = vertexIndex[0];
        if (numPoints - pointIndex < 2) {
          return Collections.singletonList(record);
        } else {
          line1 = LineStringUtil.subLineString(line, pointIndex + 1);
          line2 = LineStringUtil.subLineString(line, null, pointIndex, numPoints - pointIndex,
            null);
        }

      }
      if (line1 == null || line2 == null) {
        return Collections.singletonList(record);
      }

      return splitRecord(record, line, coordinates, line1, line2);
    }
    return Arrays.asList(record);
  }

  /** Perform the actual split. */
  protected List<LayerRecord> splitRecord(final LayerRecord record, final LineString line,
    final Coordinates point, final LineString line1, final LineString line2) {
    final DirectionalFieldsOld property = DirectionalFieldsOld.getProperty(record);

    final LayerRecord record1 = copyRecord(record);
    final LayerRecord record2 = copyRecord(record);
    record1.setGeometryValue(line1);
    record2.setGeometryValue(line2);

    property.setSplitFieldValues(line, point, record1);
    property.setSplitFieldValues(line, point, record2);
    deleteRecord(record);
    saveChanges(record);

    saveChanges(record1);
    saveChanges(record2);

    addSelectedRecords(record1, record2);
    return Arrays.asList(record1, record2);
  }

  public List<LayerRecord> splitRecord(final LayerRecord record, final Point point) {
    final LineString line = record.getGeometry();
    final Coordinates coordinates = CoordinatesUtil.get(point);
    final List<LineString> lines = LineStringUtil.split(line, coordinates);
    if (lines.size() == 2) {
      final LineString line1 = lines.get(0);
      final LineString line2 = lines.get(1);
      return splitRecord(record, line, coordinates, line1, line2);
    } else {
      return Collections.singletonList(record);
    }
  }

  @Override
  public Map<String, Object> toMap() {
    final Map<String, Object> map = super.toMap();
    if (!super.isReadOnly()) {
      MapSerializerUtil.add(map, "canAddRecords", this.canAddRecords);
      MapSerializerUtil.add(map, "canDeleteRecords", this.canDeleteRecords);
      MapSerializerUtil.add(map, "canEditRecords", this.canEditRecords);
      MapSerializerUtil.add(map, "snapToAllLayers", this.snapToAllLayers);
    }
    MapSerializerUtil.add(map, "fieldNamesSetName", this.fieldNamesSetName, ALL);
    MapSerializerUtil.add(map, "fieldNamesSets", getFieldNamesSets());
    MapSerializerUtil.add(map, "useFieldTitles", this.useFieldTitles);
    map.remove("TableView");
    return map;
  }

  public void unHighlightRecords(final Collection<? extends LayerRecord> records) {
    synchronized (this.highlightedRecords) {
      for (final LayerRecord record : records) {
        removeHighlightedRecord(record);
      }
    }
    fireHighlighted();
  }

  public void unHighlightRecords(final LayerRecord... records) {
    unHighlightRecords(Arrays.asList(records));
  }

  public void unSelectRecords(final BoundingBox boundingBox) {
    if (isSelectable()) {
      final List<LayerRecord> records = query(boundingBox);
      for (final Iterator<LayerRecord> iterator = records.iterator(); iterator.hasNext();) {
        final LayerRecord record = iterator.next();
        if (!isVisible(record) || internalIsDeleted(record)) {
          iterator.remove();
        }
      }
      unSelectRecords(records);
      if (!this.selectedRecords.isEmpty()) {
        showRecordsTable(RecordLayerTableModel.MODE_SELECTED);
      }
    }
  }

  public void unSelectRecords(final Collection<? extends LayerRecord> records) {
    for (final LayerRecord record : records) {
      removeSelectedRecord(record);
    }
    clearSelectedRecordsIndex();
    fireSelected();
    unHighlightRecords(records);
  }

  public void unSelectRecords(final LayerRecord... records) {
    unSelectRecords(Arrays.asList(records));
  }

  protected void updateRecordState(final LayerRecord record) {
    final RecordState state = record.getState();
    if (state == RecordState.Modified) {
      addModifiedRecord(record);
    } else if (state == RecordState.Persisted) {
      postSaveModifiedRecord(record);
    }
  }

  protected void updateSpatialIndex(final LayerRecord record, final Geometry oldGeometry) {
    if (oldGeometry != null) {
      final BoundingBox oldBoundingBox = BoundingBox.getBoundingBox(oldGeometry);
      if (removeFromIndex(oldBoundingBox, record)) {
        addToIndex(record);
      }
    }

  }

  public void zoomTo(final Geometry geometry) {
    if (geometry != null) {
      final BoundingBox boundingBox = BoundingBox.getBoundingBox(geometry);
      zoomToBoundingBox(boundingBox);
    }
  }

  public void zoomToBoundingBox(BoundingBox boundingBox) {
    if (boundingBox != null && !boundingBox.isEmpty()) {
      final Project project = getProject();
      final GeometryFactory geometryFactory = project.getGeometryFactory();
      boundingBox = boundingBox.convert(geometryFactory);
      boundingBox = boundingBox.expandPercent(0.1);
      project.setViewBoundingBox(boundingBox);
    }
  }

  public void zoomToHighlighted() {
    final BoundingBox boundingBox = getHighlightedBoundingBox();
    zoomToBoundingBox(boundingBox);
  }

  public void zoomToRecord(final Record record) {
    if (record != null) {
      final Geometry geometry = record.getGeometry();
      zoomTo(geometry);
    }
  }

  public void zoomToRecords(final List<? extends LayerRecord> records) {
    BoundingBox boundingBox = new BoundingBox();
    for (final Record record : records) {
      boundingBox = boundingBox.expandToInclude(record);
    }
    zoomToBoundingBox(boundingBox);
  }

  public void zoomToSelected() {
    final Project project = getProject();
    final GeometryFactory geometryFactory = project.getGeometryFactory();
    final BoundingBox boundingBox = getSelectedBoundingBox().convert(geometryFactory)
      .expandPercent(0.1);
    project.setViewBoundingBox(boundingBox);
  }
}
