package com.revolsys.raster;

import org.springframework.core.io.Resource;

public class PdfImageFactory extends AbstractGeoreferencedImageFactory {

  public PdfImageFactory() {
    super("PDF");
    addMediaTypeAndFileExtension("application/pdf", "pdf");
  }

  @Override
  public GeoreferencedImage loadImage(final Resource resource) {
    return new PdfImage(resource);
  }

}
