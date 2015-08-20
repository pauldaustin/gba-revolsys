package com.revolsys.gis.geometry.io;

import org.springframework.core.io.Resource;

import com.revolsys.gis.data.io.GeometryReader;
import com.revolsys.io.IoFactoryWithCoordinateSystem;

public interface GeometryReaderFactory extends IoFactoryWithCoordinateSystem {
  GeometryReader createGeometryReader(final Resource resource);

  boolean isBinary();
}
