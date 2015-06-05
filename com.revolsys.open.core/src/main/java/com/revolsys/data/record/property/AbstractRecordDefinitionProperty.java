package com.revolsys.data.record.property;

import com.revolsys.data.record.schema.RecordDefinition;

public abstract class AbstractRecordDefinitionProperty implements
  RecordDefinitionProperty {
  private RecordDefinition metaData;

  @Override
  public AbstractRecordDefinitionProperty clone() {
    try {
      final AbstractRecordDefinitionProperty clone = (AbstractRecordDefinitionProperty)super.clone();
      clone.metaData = null;
      return clone;
    } catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    return metaData;
  }

  @Override
  public void setRecordDefinition(final RecordDefinition metaData) {
    if (this.metaData != null) {
      this.metaData.setProperty(getPropertyName(), null);
    }
    this.metaData = metaData;
    if (metaData != null) {
      metaData.setProperty(getPropertyName(), this);
    }
  }

  public String getTypePath() {
    return getRecordDefinition().getPath();
  }

}
