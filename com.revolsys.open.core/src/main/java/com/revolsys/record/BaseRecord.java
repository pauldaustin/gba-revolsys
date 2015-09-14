package com.revolsys.record;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordDefinitionImpl;

public abstract class BaseRecord extends AbstractRecord {
  private static final long serialVersionUID = 1L;

  private transient RecordDefinition recordDefinition;

  protected RecordState state = RecordState.Initalizing;

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
    final int recordDefinitionInstanceId = ois.readInt();
    this.recordDefinition = RecordDefinitionImpl.getRecordDefinition(recordDefinitionInstanceId);
    ois.defaultReadObject();
  }

  @Override
  public void setState(final RecordState state) {
    this.state = state;
  }

  private void writeObject(final ObjectOutputStream oos) throws IOException {
    oos.writeInt(this.recordDefinition.getInstanceId());
    oos.defaultWriteObject();
  }
}
