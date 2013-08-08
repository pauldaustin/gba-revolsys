package com.revolsys.swing.table.dataobject.row;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.swing.map.table.DataObjectLayerTableModel;
import com.revolsys.swing.table.BaseJxTable;
import com.vividsolutions.jts.geom.Geometry;

public class DataObjectRowTable extends BaseJxTable implements MouseListener {
  private static final long serialVersionUID = 1L;

  public DataObjectRowTable(final DataObjectRowTableModel model) {
    this(model, new DataObjectRowTableCellRenderer());
  }

  public DataObjectRowTable(final DataObjectRowTableModel model,
    final TableCellRenderer cellRenderer) {
    super(model);
    setSortable(false);

    final DataObjectMetaData metaData = model.getMetaData();

    final JTableHeader tableHeader = getTableHeader();

    final List<TableColumn> removeColumns = new ArrayList<TableColumn>();
    final TableColumnModel columnModel = getColumnModel();
    for (int i = 0; i < model.getColumnCount(); i++) {
      final TableColumn column = columnModel.getColumn(i);
      final Class<?> attributeClass = metaData.getAttributeClass(i);
      if (Geometry.class.isAssignableFrom(attributeClass)) {
        removeColumns.add(column);
      } else {
        column.setCellRenderer(cellRenderer);
      }
    }
    for (final TableColumn column : removeColumns) {
      removeColumn(column);
    }
    tableHeader.addMouseListener(this);
    model.setTable(this);
  }

  public DataObjectMetaData getMetaData() {
    final DataObjectRowTableModel model = (DataObjectRowTableModel)getModel();
    return model.getMetaData();
  }

  public DataObject getSelectedRecord() {
    final int row = getSelectedRow();
    if (row == -1) {
      return null;
    } else {
      final DataObjectRowTableModel tableModel = getTableModel();
      return tableModel.getObject(row);
    }
  }

  public DataObjectRowTableModel getTableModel() {
    return (DataObjectRowTableModel)getModel();
  }

  @Override
  protected void initializeColumnPreferredWidth(final TableColumn column) {
    super.initializeColumnPreferredWidth(column);
    final DataObjectRowTableModel model = (DataObjectRowTableModel)getModel();
    final DataObjectMetaData metaData = model.getMetaData();
    final int viewIndex = column.getModelIndex();
    final String attributeName = model.getAttributeName(viewIndex);
    final Attribute attribute = metaData.getAttribute(attributeName);
    Integer columnWidth = attribute.getProperty("tableColumnWidth");
    final String columnName = attribute.getTitle();
    if (columnWidth == null) {
      columnWidth = attribute.getMaxStringLength() * 7;
      columnWidth = Math.min(columnWidth, 200);
      attribute.setProperty("tableColumnWidth", columnWidth);
    }
    column.setMinWidth(columnName.length() * 7 + 15);
    column.setPreferredWidth(columnWidth);
  }

  @Override
  public void mouseClicked(final MouseEvent e) {
    if (e.getSource() == getTableHeader()) {
      final DataObjectRowTableModel model = (DataObjectRowTableModel)getModel();
      final DataObjectMetaData metaData = model.getMetaData();
      final int column = columnAtPoint(e.getPoint());
      if (column > -1 && SwingUtilities.isLeftMouseButton(e)) {
        final int index = convertColumnIndexToModel(column);
        final Class<?> attributeClass = metaData.getAttributeClass(index);
        if (!Geometry.class.isAssignableFrom(attributeClass)) {
          model.setSortOrder(index);
        }
      }
    }
  }

  @Override
  public void mouseEntered(final MouseEvent e) {
  }

  @Override
  public void mouseExited(final MouseEvent e) {
  }

  @Override
  public void mousePressed(final MouseEvent e) {
  }

  @Override
  public void mouseReleased(final MouseEvent e) {
  }

  @Override
  public void tableChanged(final TableModelEvent e) {
    final TableModel model = getModel();
    if (model instanceof DataObjectLayerTableModel) {
      final DataObjectLayerTableModel layerModel = (DataObjectLayerTableModel)model;
      final String mode = layerModel.getAttributeFilterMode();
      final List<String> sortableModes = layerModel.getSortableModes();
      if (sortableModes.contains(mode)) {
        setSortable(true);
      } else {
        setSortable(false);
      }
    }
    super.tableChanged(e);
    if (tableHeader != null) {
      tableHeader.resizeAndRepaint();
    }
  }
}
