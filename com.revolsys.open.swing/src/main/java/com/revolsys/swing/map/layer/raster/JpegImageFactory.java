package com.revolsys.swing.map.layer.raster;

import org.springframework.core.io.Resource;

public class JpegImageFactory extends AbstractGeoReferencedImageFactory {

  public JpegImageFactory() {
    super("JPEG");
    addMediaTypeAndFileExtension("image/jpeg", "jpg");
    addMediaTypeAndFileExtension("image/jpeg", "jpeg");
  }

  @Override
  public GeoReferencedImage loadImage(Resource resource) {
    return new JpegImage(resource);
  }

}
