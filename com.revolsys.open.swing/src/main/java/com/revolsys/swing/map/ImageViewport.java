package com.revolsys.swing.map;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.raster.BufferedGeoreferencedImage;
import com.revolsys.swing.map.layer.Project;

public class ImageViewport extends Viewport2D implements Closeable {

  private final Graphics2D graphics;

  private final BufferedImage image;

  public ImageViewport(final Project project, final int width, final int height,
    final BoundingBox boundingBox) {
    super(project, width, height, boundingBox);
    this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
    this.graphics = (Graphics2D)this.image.getGraphics();
  }

  public ImageViewport(final Viewport2D parentViewport) {
    this(parentViewport.getProject(), parentViewport.getViewWidthPixels(),
      parentViewport.getViewHeightPixels(), parentViewport.getBoundingBox());
  }

  @Override
  public void close() {
    if (this.graphics != null) {
      this.graphics.dispose();
    }
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    close();
  }

  public BufferedGeoreferencedImage getGeoreferencedImage() {
    final BoundingBox boundingBox = getBoundingBox();
    return new BufferedGeoreferencedImage(boundingBox, this.image);
  }

  @Override
  public Graphics2D getGraphics() {
    return this.graphics;
  }

  public BufferedImage getImage() {
    return this.image;
  }
}
