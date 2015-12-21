package com.revolsys.swing.map.layer.record;

import com.revolsys.record.RecordState;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;

public class UnmodifiableRecordStoreLayerRecord extends RecordStoreLayerRecord {
  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public UnmodifiableRecordStoreLayerRecord(final RecordStoreLayer layer,
    final RecordDefinition recordDefinition) {
    super(layer, recordDefinition);
  }

  @Override
  public void setState(final RecordState state) {
    boolean setState = false;
    final RecordState currentState = getState();
    switch (state) {
      case DELETED:
      break;
      case INITIALIZING:
        if (currentState == RecordState.NEW || currentState == RecordState.INITIALIZING) {
          setState = true;
        }
      break;
      case MODIFIED:
      break;
      case NEW:
        if (currentState == RecordState.INITIALIZING) {
          setState = true;
        }
      break;
      case PERSISTED:
        if (currentState == RecordState.NEW || currentState == RecordState.INITIALIZING) {
          setState = true;
        }
      break;
      default:
      break;
    }
    if (setState) {
      super.setState(state);
    } else {
      throw new UnsupportedOperationException(
        "Cannot set record state=" + state + " (" + currentState + "):\t" + this);
    }
  }

  @Override
  protected boolean setValue(final FieldDefinition fieldDefinition, final Object value) {
    final RecordState state = getState();
    if (state.equals(RecordState.INITIALIZING)) {
      return super.setValue(fieldDefinition, value);
    } else {
      return false;
    }
  }
}