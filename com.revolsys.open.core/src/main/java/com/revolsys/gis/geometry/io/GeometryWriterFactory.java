package com.revolsys.gis.geometry.io;

import java.io.OutputStream;
import java.nio.charset.Charset;

import org.springframework.core.io.Resource;

import com.revolsys.io.IoFactoryWithCoordinateSystem;
import com.revolsys.io.Writer;
import com.vividsolutions.jts.geom.Geometry;

public interface GeometryWriterFactory extends IoFactoryWithCoordinateSystem {
  Writer<Geometry> createGeometryWriter(Resource resource);

  Writer<Geometry> createGeometryWriter(String baseName, OutputStream out);

  Writer<Geometry> createGeometryWriter(String baseName, OutputStream out, Charset charset);
}
