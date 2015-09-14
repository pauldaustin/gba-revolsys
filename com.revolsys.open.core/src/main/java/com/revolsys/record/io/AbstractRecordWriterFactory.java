package com.revolsys.record.io;

import com.revolsys.io.AbstractIoFactoryWithCoordinateSystem;

public abstract class AbstractRecordWriterFactory extends AbstractIoFactoryWithCoordinateSystem
  implements RecordWriterFactory {

  private final boolean customAttributionSupported;

  private final boolean geometrySupported;

  private boolean singleFile = true;

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
