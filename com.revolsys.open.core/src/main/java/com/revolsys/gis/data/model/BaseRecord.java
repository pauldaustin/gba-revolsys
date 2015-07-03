package com.revolsys.gis.data.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.revolsys.data.record.RecordState;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;

public abstract class BaseRecord extends AbstractRecord {
  private static final long serialVersionUID = 1L;

  /** The recordDefinition defining the object type. */
  private transient RecordDefinition recordDefinition;

  protected RecordState state = RecordState.Initalizing;

  /**
   * Construct a new empty BaseRecord using the recordDefinition.
   *
   * @param recordDefinition The recordDefinition defining the object type.
   */
  public BaseRecord(final RecordDefinition recordDefinition) {
    this.recordDefinition = recordDefinition;
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    return this.recordDefinition;
  }

  @Override
  public RecordState getState() {
    return this.state;
  }

  private void readObject(final ObjectInputStream ois) throws ClassNotFoundException, IOException {
    final int metaDataInstanceId = ois.readInt();
    this.recordDefinition = RecordDefinitionImpl.getRecordDefinition(metaDataInstanceId);
    ois.defaultReadObject();
  }

  @Override
  public void setState(final RecordState state) {
    // TODO make this more secure
    this.state = state;
  }

  private void writeObject(final ObjectOutputStream oos) throws IOException {
    oos.writeInt(this.recordDefinition.getInstanceId());
    oos.defaultWriteObject();
  }
}
