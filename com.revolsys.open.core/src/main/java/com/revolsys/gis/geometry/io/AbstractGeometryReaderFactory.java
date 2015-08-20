package com.revolsys.gis.geometry.io;

import org.springframework.core.io.Resource;

import com.revolsys.gis.data.io.GeometryReader;
import com.revolsys.io.AbstractIoFactoryWithCoordinateSystem;
import com.revolsys.io.IoFactoryRegistry;

public abstract class AbstractGeometryReaderFactory extends AbstractIoFactoryWithCoordinateSystem
  implements GeometryReaderFactory {
  public static GeometryReader geometryReader(final Resource resource) {
    final IoFactoryRegistry ioFactoryRegistry = IoFactoryRegistry.getInstance();
    final GeometryReaderFactory readerFactory = ioFactoryRegistry
      .getFactory(GeometryReaderFactory.class, resource);
    if (readerFactory == null) {
      return null;
    } else {
      final GeometryReader reader = readerFactory.createGeometryReader(resource);
      return reader;
    }
  }

  private final boolean binary;

  public AbstractGeometryReaderFactory(final String name, final boolean binary) {
    super(name);
    this.binary = binary;
  }

  @Override
  public boolean isBinary() {
    return this.binary;
  }
}
