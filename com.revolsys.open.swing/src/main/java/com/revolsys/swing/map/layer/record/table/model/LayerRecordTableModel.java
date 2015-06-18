package com.revolsys.swing.map.layer.record.table.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import com.revolsys.data.equals.EqualsRegistry;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.swing.map.form.RecordLayerForm;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.table.record.model.AbstractSingleRecordTableModel;
import com.revolsys.util.Property;

public class LayerRecordTableModel extends AbstractSingleRecordTableModel implements
  PropertyChangeListener {

  private static final long serialVersionUID = 1L;

  private LayerRecord object;

  private final AbstractRecordLayer layer;

  private final Reference<RecordLayerForm> form;

  public LayerRecordTableModel(final RecordLayerForm form) {
    super(form.getMetaData(), true);
    this.form = new WeakReference<>(form);
    this.layer = form.getLayer();
    this.object = form.getObject();
    Property.addListener(this.layer, this);
  }

  @Override
  public int getColumnCount() {
    return 4;
  }

  @Override
  public String getColumnName(final int column) {
    if (column == 3) {
      return "Original Value";
    } else {
      return super.getColumnName(column);
    }
  }

  @Override
  public String getFieldTitle(final String fieldName) {
    return this.layer.getFieldTitle(fieldName);
  }

  public LayerRecord getObject() {
    return this.object;
  }

  @Override
  public Object getObjectValue(final int rowIndex) {
    if (this.object == null) {
      return null;
    } else {
      return this.object.getValue(rowIndex);
    }
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    if (this.object == null) {
      return null;
    } else if (columnIndex == 3) {
      final String attributeName = getFieldName(rowIndex);
      return this.object.getOriginalValue(attributeName);
    } else {
      return super.getValueAt(rowIndex, columnIndex);
    }
  }

  @Override
  public boolean isCellEditable(final int rowIndex, final int columnIndex) {
    if (columnIndex == 2) {
      if (this.form.get().isEditable()) {
        final String idFieldName = getRecordDefinition().getIdFieldName();
        final String attributeName = getFieldName(rowIndex);
        if (attributeName.equals(idFieldName)) {
          return false;
        } else {
          return this.form.get().isEditable(attributeName);
        }
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  public boolean isModified(final int rowIndex) {
    final String attributeName = getFieldName(rowIndex);
    final Object originalValue = this.object.getOriginalValue(attributeName);
    final Object value = this.object.getValue(attributeName);
    return !EqualsRegistry.equal(originalValue, value);
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    final Object source = event.getSource();
    if (source == this.object) {
      final String propertyName = event.getPropertyName();
      final RecordDefinition metaData = getRecordDefinition();
      final int index = metaData.getFieldIndex(propertyName);
      if (index > -1) {
        try {
          fireTableRowsUpdated(index, index);
        } catch (final Throwable t) {
        }
      }
    }
  }

  public void removeListener() {
    Property.removeListener(this.layer, this);
  }

  public void setObject(final LayerRecord object) {
    this.object = object;
  }

  @Override
  protected Object setObjectValue(final int rowIndex, final Object value) {
    final Object oldValue = this.object.getValue(rowIndex);
    this.object.setValue(rowIndex, value);
    return oldValue;
  }
}
