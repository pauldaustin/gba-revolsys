package com.revolsys.raster;

import org.springframework.core.io.Resource;

public class GifImageFactory extends AbstractGeoreferencedImageFactory {

  public GifImageFactory() {
    super("GIF");
    addMediaTypeAndFileExtension("image/gif", "gif");
  }

  @Override
  public GeoreferencedImage loadImage(final Resource resource) {
    return new GifImage(resource);
  }

}
