package com.revolsys.swing.table.record.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.swing.SortOrder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

import com.revolsys.datatype.DataType;
import com.revolsys.record.Record;
import com.revolsys.record.RecordState;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.table.SortableTableModel;
import com.revolsys.swing.table.record.row.RecordRowTable;
import com.revolsys.util.Property;
import com.vividsolutions.jts.geom.Geometry;

public abstract class RecordRowTableModel extends AbstractRecordTableModel
  implements SortableTableModel, CellEditorListener {

  public static final String LOADING_VALUE = "\u2026";

  private static final long serialVersionUID = 1L;

  private List<String> fieldNames = new ArrayList<>();

  /** The columnIndex that the attribute start. Allows for extra columns in subclasses.*/
  private int fieldsOffset;

  private final List<String> fieldTitles = new ArrayList<>();

  private Map<Integer, SortOrder> sortedColumns = new LinkedHashMap<>();

  private RecordRowTable table;

  public RecordRowTableModel(final RecordDefinition recordDefinition,
    final Collection<String> fieldNames) {
    super(recordDefinition);
    setFieldNames(fieldNames);
    setFieldTitles(Collections.<String> emptyList());
    final String idFieldName = recordDefinition.getIdFieldName();
    setSortOrder(idFieldName);
  }

  public void clearSortedColumns() {
    synchronized (this.sortedColumns) {
      this.sortedColumns.clear();
      fireTableDataChanged();
    }
  }

  @Override
  @PreDestroy
  public void dispose() {
    super.dispose();
    this.sortedColumns = null;
  }

  @Override
  public void editingCanceled(final ChangeEvent event) {
  }

  @Override
  public void editingStopped(final ChangeEvent event) {
  }

  public int getAttributesOffset() {
    return this.fieldsOffset;
  }

  public List<String> getAttributeTitles() {
    return this.fieldTitles;
  }

  public FieldDefinition getColumnAttribute(final int columnIndex) {

    if (columnIndex < this.fieldsOffset) {
      return null;
    } else {
      final String name = getFieldName(columnIndex);
      final RecordDefinition recordDefinition = getRecordDefinition();
      return recordDefinition.getField(name);
    }
  }

  @Override
  public Class<?> getColumnClass(final int columnIndex) {
    if (columnIndex < this.fieldsOffset) {
      return Object.class;
    } else {
      final String name = getFieldName(columnIndex);
      final RecordDefinition recordDefinition = getRecordDefinition();
      final DataType type = recordDefinition.getFieldType(name);
      if (type == null) {
        return Object.class;
      } else {
        return type.getJavaClass();
      }
    }
  }

  @Override
  public int getColumnCount() {
    final int numColumns = this.fieldsOffset + this.fieldNames.size();
    return numColumns;
  }

  @Override
  public String getColumnName(final int columnIndex) {
    if (columnIndex < this.fieldsOffset) {
      return null;
    } else {
      return this.fieldTitles.get(columnIndex - this.fieldsOffset);
    }
  }

  @Override
  public String getFieldName(final int columnIndex) {
    if (columnIndex < this.fieldsOffset) {
      return null;
    } else {
      final String fieldName = this.fieldNames.get(columnIndex - this.fieldsOffset);
      if (fieldName == null) {
        return null;
      } else {
        final int index = fieldName.indexOf('.');
        if (index == -1) {
          return fieldName;
        } else {
          return fieldName.substring(0, index);
        }
      }
    }
  }

  @Override
  public String getFieldName(final int rowIndex, final int columnIndex) {
    return getFieldName(columnIndex);
  }

  public abstract <V extends Record> V getRecord(final int row);

  public List<LayerRecord> getRecords(final int[] rows) {
    final List<LayerRecord> objects = new ArrayList<LayerRecord>();
    for (final int row : rows) {
      final LayerRecord record = getRecord(row);
      if (record != null) {
        objects.add(record);
      }
    }
    return objects;
  }

  public Map<Integer, SortOrder> getSortedColumns() {
    return this.sortedColumns;
  }

  @Override
  public SortOrder getSortOrder(final int columnIndex) {
    synchronized (this.sortedColumns) {
      return this.sortedColumns.get(columnIndex);
    }
  }

  public RecordRowTable getTable() {
    return this.table;
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    if (columnIndex < this.fieldsOffset) {
      return null;
    } else {
      final Record record = getRecord(rowIndex);
      if (record == null) {
        return LOADING_VALUE;
      } else if (record.getState() == RecordState.Initalizing) {
        return LOADING_VALUE;
      } else {
        final String name = getFieldName(columnIndex);
        final Object value = record.getValue(name);
        return value;
      }
    }
  }

  @Override
  public boolean isCellEditable(final int rowIndex, final int columnIndex) {
    if (isEditable()) {
      final Record record = getRecord(rowIndex);
      if (record != null && record.getState() != RecordState.Initalizing) {
        final String fieldName = getFieldName(rowIndex, columnIndex);
        if (fieldName != null) {
          if (!isReadOnly(fieldName)) {
            final Class<?> attributeClass = getRecordDefinition().getFieldClass(fieldName);
            if (!Geometry.class.isAssignableFrom(attributeClass)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  @Override
  public boolean isSelected(boolean selected, final int rowIndex, final int columnIndex) {
    final int[] selectedRows = this.table.getSelectedRows();
    selected = false;
    for (final int selectedRow : selectedRows) {
      if (rowIndex == selectedRow) {
        return true;
      }
    }
    return false;
  }

  public void setFieldNames(final Collection<String> fieldNames) {
    if (fieldNames == null || fieldNames.isEmpty()) {
      final RecordDefinition recordDefinition = getRecordDefinition();
      this.fieldNames = new ArrayList<String>(recordDefinition.getFieldNames());
    } else {
      this.fieldNames = new ArrayList<String>(fieldNames);
    }
  }

  public void setFieldsOffset(final int fieldsOffset) {
    this.fieldsOffset = fieldsOffset;
  }

  public void setFieldTitles(final List<String> attributeTitles) {
    this.fieldTitles.clear();
    for (int i = 0; i < this.fieldNames.size(); i++) {
      String title;
      if (i < attributeTitles.size()) {
        title = attributeTitles.get(i);
      } else {
        final String fieldName = getFieldName(i);
        final RecordDefinition recordDefinition = getRecordDefinition();
        final FieldDefinition attribute = recordDefinition.getField(fieldName);
        title = attribute.getTitle();
      }
      this.fieldTitles.add(title);
    }
  }

  @Override
  public SortOrder setSortOrder(final int columnIndex) {
    synchronized (this.sortedColumns) {
      SortOrder sortOrder = this.sortedColumns.get(columnIndex);
      this.sortedColumns.clear();
      if (sortOrder == SortOrder.ASCENDING) {
        sortOrder = SortOrder.DESCENDING;
      } else {
        sortOrder = SortOrder.ASCENDING;
      }
      this.sortedColumns.put(columnIndex, sortOrder);

      fireTableDataChanged();
      return sortOrder;
    }
  }

  public void setSortOrder(final String idFieldName) {
    if (Property.hasValue(idFieldName)) {
      final int index = this.fieldNames.indexOf(idFieldName);
      if (index != -1) {
        setSortOrder(index);
      }
    }
  }

  public void setTable(final RecordRowTable table) {
    this.table = table;
    addTableModelListener(table);
  }

  @Override
  public void setValueAt(final Object value, final int rowIndex, final int columnIndex) {
    if (isCellEditable(rowIndex, columnIndex)) {

      if (columnIndex >= this.fieldsOffset) {
        final Record object = getRecord(rowIndex);
        if (object != null) {
          final String name = getFieldName(columnIndex);
          final Object objectValue = toObjectValue(columnIndex, value);
          object.setValue(name, objectValue);
        }
      }
    }

  }

  @Override
  public final String toDisplayValue(final int rowIndex, final int attributeIndex,
    final Object objectValue) {
    int rowHeight = this.table.getRowHeight();
    String displayValue;
    final Record record = getRecord(rowIndex);
    if (record == null) {
      rowHeight = 1;
      displayValue = null;
    } else {
      if (record.getState() == RecordState.Initalizing) {
        displayValue = LOADING_VALUE;
      } else {
        displayValue = toDisplayValueInternal(rowIndex, attributeIndex, objectValue);
      }
    }
    if (rowHeight != this.table.getRowHeight(rowIndex)) {
      this.table.setRowHeight(rowIndex, rowHeight);
    }
    return displayValue;
  }

  protected String toDisplayValueInternal(final int rowIndex, final int attributeIndex,
    final Object objectValue) {
    return super.toDisplayValue(rowIndex, attributeIndex, objectValue);
  }
}
