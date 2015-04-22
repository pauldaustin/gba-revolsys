package com.revolsys.gis.data.model;

import com.revolsys.data.record.schema.RecordDefinition;

public abstract class AbstractDataObjectMetaDataProperty implements
  DataObjectMetaDataProperty {
  private RecordDefinition metaData;

  @Override
  public AbstractDataObjectMetaDataProperty clone() {
    try {
      final AbstractDataObjectMetaDataProperty clone = (AbstractDataObjectMetaDataProperty)super.clone();
      clone.metaData = null;
      return clone;
    } catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public RecordDefinition getMetaData() {
    return metaData;
  }

  @Override
  public void setMetaData(final RecordDefinition metaData) {
    if (this.metaData != null) {
      this.metaData.setProperty(getPropertyName(), null);
    }
    this.metaData = metaData;
    if (metaData != null) {
      metaData.setProperty(getPropertyName(), this);
    }
  }

  public String getTypePath() {
    return getMetaData().getPath();
  }

}
