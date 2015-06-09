package com.revolsys.raster;

import org.springframework.core.io.Resource;

public class JpegImageFactory extends AbstractGeoreferencedImageFactory {

  public JpegImageFactory() {
    super("JPEG");
    addMediaTypeAndFileExtension("image/jpeg", "jpg");
    addMediaTypeAndFileExtension("image/jpeg", "jpeg");
  }

  @Override
  public GeoreferencedImage loadImage(final Resource resource) {
    return new JpegImage(resource);
  }

}
