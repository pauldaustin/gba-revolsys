package com.revolsys.swing.map.layer.record.table;

import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.io.RecordIo;
import com.revolsys.data.record.io.RecordReader;
import com.revolsys.data.record.io.RecordWriterFactory;
import com.revolsys.data.record.property.DirectionalFields;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.types.DataType;
import com.revolsys.data.types.DataTypes;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.action.enablecheck.AndEnableCheck;
import com.revolsys.swing.action.enablecheck.EnableCheck;
import com.revolsys.swing.action.enablecheck.InvokeMethodEnableCheck;
import com.revolsys.swing.action.enablecheck.ObjectPropertyEnableCheck;
import com.revolsys.swing.action.enablecheck.OrEnableCheck;
import com.revolsys.swing.dnd.ClipboardUtil;
import com.revolsys.swing.map.action.AddFileLayerAction;
import com.revolsys.swing.map.form.RecordLayerForm;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.map.layer.record.component.FieldFilterPanel;
import com.revolsys.swing.map.layer.record.table.model.RecordLayerTableModel;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.parallel.Invoke;
import com.revolsys.swing.table.TablePanel;
import com.revolsys.swing.table.TableRowCount;
import com.revolsys.swing.table.record.editor.RecordTableCellEditor;
import com.revolsys.swing.table.record.model.RecordRowTableModel;
import com.revolsys.swing.table.record.row.RecordRowPropertyEnableCheck;
import com.revolsys.swing.table.record.row.RecordRowRunnable;
import com.revolsys.swing.toolbar.ToolBar;
import com.revolsys.util.PreferencesUtil;
import com.revolsys.util.Property;
import com.vividsolutions.jts.geom.Geometry;

public class RecordLayerTablePanel extends TablePanel implements PropertyChangeListener {
  private static final long serialVersionUID = 1L;

  public static final String FILTER_GEOMETRY = "filter_geometry";

  public static final String FILTER_FIELD = "filter_attribute";

  private final AbstractRecordLayer layer;

  private final RecordLayerTableModel tableModel;

  private final RecordTableCellEditor tableCellEditor;

  private final Map<String, JToggleButton> buttonByMode = new HashMap<>();

  public RecordLayerTablePanel(final AbstractRecordLayer layer, final RecordLayerTable table) {
    super(table);
    this.layer = layer;
    this.tableCellEditor = table.getTableCellEditor();
    this.tableCellEditor.setPopupMenu(getMenu());
    table.getTableCellEditor().addMouseListener(this);
    table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    this.tableModel = getTableModel();
    final RecordDefinition metaData = layer.getRecordDefinition();
    final boolean hasGeometry = metaData.getGeometryFieldIndex() != -1;
    final EnableCheck deletableEnableCheck = new RecordRowPropertyEnableCheck("deletable");

    final EnableCheck modifiedEnableCheck = new RecordRowPropertyEnableCheck("modified");
    final EnableCheck deletedEnableCheck = new RecordRowPropertyEnableCheck("deleted");
    final EnableCheck notEnableCheck = new RecordRowPropertyEnableCheck("deleted", false);
    final OrEnableCheck modifiedOrDeleted = new OrEnableCheck(modifiedEnableCheck,
      deletedEnableCheck);

    final EnableCheck editableEnableCheck = new ObjectPropertyEnableCheck(layer, "editable");

    final EnableCheck cellEditingEnableCheck = new ObjectPropertyEnableCheck(this,
      "editingCurrentCell");

    // Right click Menu
    final MenuFactory menu = getMenu();

    final MenuFactory layerMenuFactory = MenuFactory.findMenu(layer);

    menu.addMenuItemTitleIcon("record", "View/Edit Record", "table_edit", notEnableCheck, this,
      "editRecord");

    if (hasGeometry) {
      menu.addMenuItemTitleIcon("record", "Zoom to Record", "magnifier_zoom_selected", this,
        "zoomToRecord");
    }
    menu.addMenuItemTitleIcon("record", "Delete Record", "table_row_delete", deletableEnableCheck,
      this, "deleteRecord");

    menu.addMenuItem("record", RecordRowRunnable.createAction("Revert Record", "arrow_revert",
      modifiedOrDeleted, "revertChanges"));

    menu.addMenuItem("record", RecordRowRunnable.createAction("Revert Empty Fields",
      "field_empty_revert", modifiedEnableCheck, "revertEmptyFields"));

    menu.addMenuItemTitleIcon("dnd", "Copy Record", "page_copy", this, "copyRecord");

    menu.addMenuItemTitleIcon("dataTransfer", "Cut Field Value", "cut", cellEditingEnableCheck,
      this, "cutFieldValue");
    menu.addMenuItemTitleIcon("dataTransfer", "Copy Field Value", "page_copy", this,
      "copyFieldValue");
    menu.addMenuItemTitleIcon("dataTransfer", "Paste Field Value", "paste_plain",
      cellEditingEnableCheck, this, "pasteFieldValue");

    if (hasGeometry) {
      menu.addMenuItemTitleIcon("dnd", "Paste Geometry", "geometry_paste",
        new AndEnableCheck(editableEnableCheck,
          new InvokeMethodEnableCheck(this, "canPasteRecordGeometry")),
        this, "pasteGeometry");

      final MenuFactory editMenu = new MenuFactory("Edit Record Operations");
      final DataType geometryDataType = metaData.getGeometryField().getType();
      if (geometryDataType == DataTypes.LINE_STRING
        || geometryDataType == DataTypes.MULTI_LINE_STRING) {
        if (DirectionalFields.getProperty(metaData).hasDirectionalFields()) {
          editMenu.addMenuItemTitleIcon("geometry", RecordLayerForm.FLIP_RECORD_NAME,
            RecordLayerForm.FLIP_RECORD_ICON, editableEnableCheck, this, "flipRecordOrientation");
          editMenu.addMenuItemTitleIcon("geometry", RecordLayerForm.FLIP_LINE_ORIENTATION_NAME,
            RecordLayerForm.FLIP_LINE_ORIENTATION_ICON, editableEnableCheck, this,
            "flipLineOrientation");
          editMenu.addMenuItemTitleIcon("geometry", RecordLayerForm.FLIP_FIELDS_NAME,
            RecordLayerForm.FLIP_FIELDS_ICON, editableEnableCheck, this, "flipFields");
        } else {
          editMenu.addMenuItemTitleIcon("geometry", "Flip Line Orientation", "flip_line",
            editableEnableCheck, this, "flipLineOrientation");
        }
      }
      if (editMenu.getItemCount() > 0) {
        menu.addComponentFactory("record", 2, editMenu);
      }
    }

    // Toolbar
    final ToolBar toolBar = getToolBar();

    if (layerMenuFactory != null) {
      toolBar.addButtonTitleIcon("menu", "Layer Menu", "menu", layerMenuFactory, "show", layer,
        this, 10, 10);
    }

    if (hasGeometry) {
      final EnableCheck hasSelectedRecords = new ObjectPropertyEnableCheck(layer,
        "hasSelectedRecords");
      toolBar.addButton("layer", "Zoom to Selected", "magnifier_zoom_selected", hasSelectedRecords,
        layer, "zoomToSelected");
    }

    toolBar.addComponent("count", new TableRowCount(this.tableModel));

    toolBar.addButtonTitleIcon("table", "Refresh", "table_refresh", this, "refresh");
    toolBar.addButtonTitleIcon("table", "Export Records", "table_save", () -> exportRecords());

    final FieldFilterPanel fieldFilterPanel = new FieldFilterPanel(this, this.tableModel);
    toolBar.addComponent("search", fieldFilterPanel);

    toolBar.addButtonTitleIcon("search", "Advanced Search", "filter_edits", fieldFilterPanel,
      "showAdvancedFilter");

    final EnableCheck hasFilter = new ObjectPropertyEnableCheck(this.tableModel, "hasFilter");

    toolBar.addButton("search", "Clear Search", "filter_delete", hasFilter, fieldFilterPanel,
      "clear");

    // Filter buttons

    final JToggleButton clearFilter = addFieldFilterToggleButton(toolBar, -1, "Show All Records",
      "table_filter", RecordLayerTableModel.MODE_ALL, null);
    clearFilter.doClick();

    addFieldFilterToggleButton(toolBar, -1, "Show Only Changed Records", "change_table_filter",
      RecordLayerTableModel.MODE_EDITS, editableEnableCheck);

    addFieldFilterToggleButton(toolBar, -1, "Show Only Selected Records", "filter_selected",
      RecordLayerTableModel.MODE_SELECTED, null);

    if (hasGeometry) {
      final JToggleButton showAllGeometries = addGeometryFilterToggleButton(toolBar, -1,
        "Show All Records ", "world_filter", "all", null);
      showAllGeometries.doClick();

      addGeometryFilterToggleButton(toolBar, -1, "Show Records on Map", "map_filter", "boundingBox",
        null);
    }
    Property.addListener(layer, this);
  }

  protected JToggleButton addFieldFilterToggleButton(final ToolBar toolBar, final int index,
    final String title, final String icon, final String mode, final EnableCheck enableCheck) {
    final JToggleButton button = toolBar.addToggleButtonTitleIcon(FILTER_FIELD, index, title, icon,
      () -> setFieldFilterMode(mode));
    this.buttonByMode.put(FILTER_FIELD + "_" + mode, button);
    return button;
  }

  protected JToggleButton addGeometryFilterToggleButton(final ToolBar toolBar, final int index,
    final String title, final String icon, final String mode, final EnableCheck enableCheck) {
    final JToggleButton button = toolBar.addToggleButtonTitleIcon(FILTER_GEOMETRY, index, title,
      icon, () -> setGeometryFilterMode(mode));
    this.buttonByMode.put(FILTER_GEOMETRY + "_" + mode, button);
    return button;
  }

  public boolean canPasteRecordGeometry() {
    final LayerRecord record = getEventRowObject();
    return this.layer.canPasteRecordGeometry(record);
  }

  public void copyFieldValue() {
    if (isEditingCurrentCell()) {
      final JComponent editorComponent = this.tableCellEditor.getEditorComponent();
      SwingUtil.dndCopy(editorComponent);
    } else {
      final RecordRowTableModel model = getTableModel();
      final int row = getEventRow();
      final int column = getEventColumn();
      final Object value = model.getValueAt(row, column);

      final String displayValue = model.toDisplayValue(row, column, value);
      final StringSelection transferable = new StringSelection(displayValue);
      ClipboardUtil.setContents(transferable);
    }
  }

  public void copyRecord() {
    final LayerRecord record = getEventRowObject();
    this.layer.copyRecordsToClipboard(Collections.singletonList(record));
  }

  public void cutFieldValue() {
    if (isEditingCurrentCell()) {
      final JComponent editorComponent = this.tableCellEditor.getEditorComponent();
      SwingUtil.dndCut(editorComponent);
    }
  }

  public void deleteRecord() {
    final LayerRecord object = getEventRowObject();
    this.layer.deleteRecords(object);
  }

  public void editRecord() {
    final LayerRecord object = getEventRowObject();
    if (object != null && !object.isDeleted()) {
      this.layer.showForm(object);
    }
  }

  private void exportRecords() {
    final RecordDefinition recordDefinition = this.layer.getRecordDefinition();
    final JFileChooser fileChooser = SwingUtil.createFileChooser("Export Records",
      "com.revolsys.swing.map.table.export", "directory");
    final String defaultFileExtension = PreferencesUtil
      .getUserString("com.revolsys.swing.map.table.export", "fileExtension", "tsv");

    final List<FileNameExtensionFilter> recordFileFilters = new ArrayList<>();
    for (final RecordWriterFactory factory : IoFactoryRegistry.getInstance()
      .getFactories(RecordWriterFactory.class)) {
      if (recordDefinition.hasGeometryField() || factory.isCustomFieldsSupported()) {
        recordFileFilters.add(AddFileLayerAction.createFilter(factory));
      }
    }
    AddFileLayerAction.sortFilters(recordFileFilters);

    fileChooser.setAcceptAllFileFilterUsed(false);
    fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), this.layer.getName()));
    for (final FileNameExtensionFilter fileFilter : recordFileFilters) {
      fileChooser.addChoosableFileFilter(fileFilter);
      if (Arrays.asList(fileFilter.getExtensions()).contains(defaultFileExtension)) {
        fileChooser.setFileFilter(fileFilter);
      }
    }

    fileChooser.setMultiSelectionEnabled(false);
    final int returnVal = fileChooser.showSaveDialog(SwingUtil.getActiveWindow());
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      final FileNameExtensionFilter fileFilter = (FileNameExtensionFilter)fileChooser
        .getFileFilter();
      File file = fileChooser.getSelectedFile();
      if (file != null) {
        final String fileExtension = FileUtil.getFileNameExtension(file);
        final String expectedExtension = fileFilter.getExtensions()[0];
        if (!fileExtension.equals(expectedExtension)) {
          file = FileUtil.getFileWithExtension(file, expectedExtension);
        }
        final File targetFile = file;
        PreferencesUtil.setUserString("com.revolsys.swing.map.table.export", "fileExtension",
          expectedExtension);
        PreferencesUtil.setUserString("com.revolsys.swing.map.table.export", "directory",
          file.getParent());
        final String description = "Export " + this.layer.getPath() + " to "
          + targetFile.getAbsolutePath();
        Invoke.background(description, () -> {
          try (
            final RecordReader reader = this.tableModel.getReader()) {
            RecordIo.copyRecords(reader, targetFile);
          }
        });
      }
    }
  }

  public void flipFields() {
    final Record record = getEventRowObject();
    final DirectionalFields property = DirectionalFields.getProperty(record);
    property.reverseAttributes(record);
  }

  public void flipLineOrientation() {
    final Record record = getEventRowObject();
    final DirectionalFields property = DirectionalFields.getProperty(record);
    property.reverseGeometry(record);
  }

  public void flipRecordOrientation() {
    final Record record = getEventRowObject();
    DirectionalFields.reverse(record);
  }

  public Collection<? extends String> getColumnNames() {
    return this.layer.getFieldNames();
  }

  protected LayerRecord getEventRowObject() {
    final RecordRowTableModel model = getTableModel();
    final int row = getEventRow();
    if (row > -1) {
      final LayerRecord object = model.getRecord(row);
      return object;
    } else {
      return null;
    }
  }

  public AbstractRecordLayer getLayer() {
    return this.layer;
  }

  @SuppressWarnings("unchecked")
  @Override
  public RecordLayerTableModel getTableModel() {
    final JTable table = getTable();
    return (RecordLayerTableModel)table.getModel();
  }

  @Override
  public void mouseClicked(final MouseEvent e) {
    super.mouseClicked(e);
    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
      this.tableCellEditor.stopCellEditing();
      editRecord();
    }
  }

  public void pasteFieldValue() {
    if (isEditingCurrentCell()) {
      final JComponent editorComponent = this.tableCellEditor.getEditorComponent();
      SwingUtil.dndPaste(editorComponent);
    }
  }

  public void pasteGeometry() {
    final LayerRecord record = getEventRowObject();
    this.layer.pasteRecordGeometry(record);
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    final Object source = event.getSource();
    if (source instanceof LayerRecord) {
      repaint();
    } else if (source == this.layer) {
      final String propertyName = event.getPropertyName();
      if ("recordsChanged".equals(propertyName)) {
        this.tableModel.refresh();
      }
      repaint();
    }
  }

  public void refresh() {
    this.tableModel.refresh();
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    Property.removeListener(this.layer, this);
  }

  public void setFieldFilterMode(final String mode) {
    final JToggleButton button = this.buttonByMode.get(FILTER_FIELD + "_" + mode);
    if (button != null) {
      if (!button.isSelected()) {
        button.doClick();
      }
      this.tableModel.setFieldFilterMode(mode);
      this.layer.setProperty("fieldFilterMode", mode);
    }
  }

  public void setGeometryFilterMode(final String mode) {
    final JToggleButton button = this.buttonByMode.get(FILTER_GEOMETRY + "_" + mode);
    if (button != null) {
      if (!button.isSelected()) {
        button.doClick();
      }
      this.tableModel.setFilterByBoundingBox("boundingBox".equals(mode));
      this.layer.setProperty("geometryFilterMode", mode);
    }
  }

  public void zoomToRecord() {
    final Record object = getEventRowObject();
    final Project project = this.layer.getProject();
    final Geometry geometry = object.getGeometry();
    if (geometry != null) {
      final GeometryFactory geometryFactory = project.getGeometryFactory();
      final BoundingBox boundingBox = BoundingBox.getBoundingBox(geometry)
        .convert(geometryFactory)
        .expandPercent(0.1);
      project.setViewBoundingBox(boundingBox);
    }
  }
}
