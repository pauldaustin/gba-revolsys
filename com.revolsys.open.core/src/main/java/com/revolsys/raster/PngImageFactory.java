package com.revolsys.raster;

import org.springframework.core.io.Resource;

public class PngImageFactory extends AbstractGeoreferencedImageFactory {

  public PngImageFactory() {
    super("PNG");
    addMediaTypeAndFileExtension("image/png", "png");
  }

  @Override
  public GeoreferencedImage loadImage(final Resource resource) {
    return new PngImage(resource);
  }

}
