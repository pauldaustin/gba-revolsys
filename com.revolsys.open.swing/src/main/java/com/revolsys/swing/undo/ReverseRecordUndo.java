package com.revolsys.swing.undo;

import com.revolsys.equals.Equals;
import com.revolsys.record.Record;
import com.revolsys.record.property.DirectionalFieldsOld;
import com.vividsolutions.jts.geom.Geometry;

public class ReverseRecordUndo extends AbstractUndoableEdit {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private final Record object;

  private final Geometry oldValue;

  public ReverseRecordUndo(final Record object) {
    this.object = object;
    this.oldValue = object.getGeometry();
  }

  @Override
  public boolean canRedo() {
    if (super.canRedo()) {
      final Geometry value = this.object.getGeometry();
      if (Equals.equal(value, this.oldValue)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean canUndo() {
    if (super.canUndo()) {
      final Geometry value = this.object.getGeometry();
      if (Equals.equal(value.reverse(), this.oldValue)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void doRedo() {
    DirectionalFieldsOld.reverse(this.object);
  }

  @Override
  protected void doUndo() {
    DirectionalFieldsOld.reverse(this.object);
  }

  @Override
  public String toString() {
    return "Reverse record";
  }
}
