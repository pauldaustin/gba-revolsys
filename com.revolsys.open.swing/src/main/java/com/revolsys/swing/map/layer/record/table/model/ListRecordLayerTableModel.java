package com.revolsys.swing.map.layer.record.table.model;

import com.revolsys.record.Record;
import com.revolsys.swing.EventQueue;
import com.revolsys.swing.map.layer.record.ListRecordLayer;
import com.revolsys.swing.map.layer.record.table.RecordLayerTable;

public class ListRecordLayerTableModel extends RecordLayerTableModel {
  private static final long serialVersionUID = 1L;

  public static RecordLayerTable newTable(final ListRecordLayer layer) {
    final RecordLayerTableModel model = new ListRecordLayerTableModel(layer);
    final RecordLayerTable table = new RecordLayerTable(model);

    EventQueue.addPropertyChange(layer, "hasSelectedRecords", () -> selectionChanged(table, model));

    return table;
  }

  public ListRecordLayerTableModel(final ListRecordLayer layer) {
    super(layer);
    setEditable(false);
    addFieldFilterMode(new ModeAllList(this));
  }

  @Override
  public void setValueAt(final Object value, final int rowIndex, final int columnIndex) {
    final Record record = getRecord(rowIndex);
    if (record != null) {
      final String name = getFieldName(columnIndex);
      final Object oldValue = record.getValueByPath(name);
      record.setValue(name, value);
      firePropertyChange(record, name, oldValue, value);
    }
  }
}