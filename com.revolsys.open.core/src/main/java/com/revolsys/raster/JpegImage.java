package com.revolsys.raster;

import org.springframework.core.io.Resource;

public class JpegImage extends JaiGeoreferencedImage {

  public JpegImage(final Resource imageResource) {
    super(imageResource);
  }

  @Override
  public String getWorldFileExtension() {
    return "jgw";
  }
}
