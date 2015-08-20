package com.revolsys.data.record.io;

import com.revolsys.io.AbstractIoFactoryWithCoordinateSystem;

public abstract class AbstractRecordWriterFactory extends AbstractIoFactoryWithCoordinateSystem
  implements RecordWriterFactory {

  private boolean singleFile = true;

  private final boolean geometrySupported;

  private final boolean customAttributionSupported;

  public AbstractRecordWriterFactory(final String name, final boolean geometrySupported,
    final boolean customAttributionSupported) {
    super(name);
    this.geometrySupported = geometrySupported;
    this.customAttributionSupported = customAttributionSupported;
  }

  @Override
  public boolean isCustomFieldsSupported() {
    return this.customAttributionSupported;
  }

  @Override
  public boolean isGeometrySupported() {
    return this.geometrySupported;
  }

  @Override
  public boolean isSingleFile() {
    return this.singleFile;
  }

  protected void setSingleFile(final boolean singleFile) {
    this.singleFile = singleFile;
  }
}
