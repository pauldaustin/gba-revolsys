package com.revolsys.swing.map.layer.bing;

import java.awt.image.BufferedImage;

import com.revolsys.swing.map.layer.MapTile;

public class BingMapTile extends MapTile {

  private final String quadKey;

  private final BingLayer layer;

  public BingMapTile(final BingLayer layer, final int zoomLevel,
    final double resolution, final int tileX, final int tileY) {
    super(layer.getClient().getBoundingBox(zoomLevel, tileX, tileY), 256, 256,
      resolution);
    this.layer = layer;
    this.quadKey = layer.getClient().getQuadKey(zoomLevel, tileX, tileY);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof BingMapTile) {
      final BingMapTile tile = (BingMapTile)obj;
      if (tile.layer == layer) {
        if (tile.quadKey.equals(quadKey)) {
          return true;
        }
      }
    }
    return false;
  }

  public String getQuadKey() {
    return quadKey;
  }

  @Override
  public int hashCode() {
    return quadKey.hashCode();
  }

  @Override
  public BufferedImage loadBuffferedImage() {
    try {
      final BingClient client = layer.getClient();
      final ImagerySet imagerySet = layer.getImagerySet();
      final MapLayer mapLayer = layer.getMapLayer();
      final BufferedImage image = client.getMapImage(imagerySet, mapLayer,
        quadKey);
      return image;
    } catch (final Throwable t) {
      layer.setError(t);
      return null;
    }
  }

  @Override
  public String toString() {
    return layer + " " + quadKey;
  }
}
