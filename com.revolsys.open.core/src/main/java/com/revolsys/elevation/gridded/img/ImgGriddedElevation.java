package com.revolsys.elevation.gridded.img;

import com.revolsys.elevation.gridded.GriddedElevationModelReaderFactory;
import com.revolsys.collection.map.MapEx;
import com.revolsys.elevation.gridded.GriddedElevationModelReader;
import com.revolsys.io.AbstractIoFactoryWithCoordinateSystem;
import com.revolsys.spring.resource.Resource;

public class ImgGriddedElevation extends AbstractIoFactoryWithCoordinateSystem
  implements GriddedElevationModelReaderFactory {

  public static final String FILE_EXTENSION = "img";

  public ImgGriddedElevation() {
    super("IMG DEM");
    addMediaTypeAndFileExtension("image/x-img-dem", FILE_EXTENSION);
  }

  @Override
  public GriddedElevationModelReader newGriddedElevationModelReader(final Resource resource,
    final MapEx properties) {
    return new ImgGriddedElevationReader(resource, properties);
  }
}