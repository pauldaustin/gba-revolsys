package com.revolsys.swing.map.layer.record.table;

import javax.swing.RowSorter;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.table.model.RecordLayerTableModel;
import com.revolsys.swing.table.dataobject.row.DataObjectRowTable;

public class DataObjectLayerTable extends DataObjectRowTable {

  private static final long serialVersionUID = 1L;

  public DataObjectLayerTable(final RecordLayerTableModel model) {
    super(model);
  }

  public DataObjectLayerTable(final RecordLayerTableModel model,
    final TableCellRenderer cellRenderer) {
    super(model, cellRenderer);
  }

  @Override
  protected RowSorter<? extends TableModel> createDefaultRowSorter() {
    final AbstractRecordLayer layer = getLayer();
    final RecordLayerTableModel model = (RecordLayerTableModel)getTableModel();
    return new DataObjectLayerTableRowSorter(layer, model);
  }

  @SuppressWarnings("unchecked")
  public <V extends AbstractRecordLayer> V getLayer() {
    final RecordLayerTableModel model = (RecordLayerTableModel)getTableModel();
    return (V)model.getLayer();
  }
}
