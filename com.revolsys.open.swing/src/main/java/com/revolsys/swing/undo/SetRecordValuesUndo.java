package com.revolsys.swing.undo;

import java.util.HashMap;
import java.util.Map;

import com.revolsys.gis.model.data.equals.MapEquals;
import com.revolsys.swing.map.layer.record.LayerDataObject;

public class SetRecordValuesUndo extends AbstractUndoableEdit {
  private static final long serialVersionUID = 1L;

  private final LayerDataObject record;

  private final Map<String, Object> originalValues = new HashMap<String, Object>();

  private final Map<String, Object> newValues = new HashMap<String, Object>();

  public SetRecordValuesUndo(final LayerDataObject record, final Map<String, Object> newValues) {
    this.record = record;
    if (record != null) {
      this.originalValues.putAll(record);
    }
    if (newValues != null) {
      this.newValues.putAll(newValues);
    }
  }

  @Override
  public boolean canRedo() {
    if (super.canRedo()) {
      return MapEquals.equalMap1Keys(this.record, this.originalValues);
    }
    return false;
  }

  @Override
  public boolean canUndo() {
    if (super.canUndo()) {
      return MapEquals.equalMap1Keys(this.record, this.newValues);
    }
    return false;
  }

  @Override
  protected void doRedo() {
    if (this.record != null) {
      this.record.getLayer().replaceValues(this.record, this.newValues);
    }
  }

  @Override
  protected void doUndo() {
    if (this.record != null) {
      this.record.getLayer().replaceValues(this.record, this.originalValues);
    }
  }

  @Override
  public String toString() {
    return "Set record values";
  }
}
