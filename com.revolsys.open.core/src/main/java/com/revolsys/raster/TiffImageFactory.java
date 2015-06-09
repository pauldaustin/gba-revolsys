package com.revolsys.raster;

import org.springframework.core.io.Resource;

public class TiffImageFactory extends AbstractGeoreferencedImageFactory {

  public TiffImageFactory() {
    super("TIFF/GeoTIFF");
    addMediaTypeAndFileExtension("image/tiff", "tif");
    addMediaTypeAndFileExtension("image/tiff", "tiff");
  }

  @Override
  public GeoreferencedImage loadImage(final Resource resource) {
    return new TiffImage(resource);
  }

}
