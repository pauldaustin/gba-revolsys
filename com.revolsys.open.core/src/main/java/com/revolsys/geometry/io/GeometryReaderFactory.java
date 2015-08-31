package com.revolsys.geometry.io;

import com.revolsys.io.FileIoFactory;
import com.revolsys.io.IoFactoryWithCoordinateSystem;
import com.revolsys.spring.resource.Resource;

public interface GeometryReaderFactory extends FileIoFactory, IoFactoryWithCoordinateSystem {
  default GeometryReader createGeometryReader(final Object source) {
    final Resource resource = (Resource)Resource.getResource(source);
    return createGeometryReader(resource);
  }

  GeometryReader createGeometryReader(final Resource resource);

}
