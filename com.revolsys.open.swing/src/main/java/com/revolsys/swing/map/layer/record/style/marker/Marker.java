package com.revolsys.swing.map.layer.record.style.marker;

import java.awt.Graphics2D;

import javax.swing.Icon;

import com.revolsys.io.map.MapSerializer;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.map.layer.record.style.MarkerStyle;

public interface Marker extends MapSerializer {
  default String getMarkerType() {
    return null;
  }

  default boolean isUseMarkerType() {
    return false;
  }

  Icon newIcon(MarkerStyle style);

  void render(Viewport2D viewport, Graphics2D graphics, MarkerStyle style, double modelX,
    double modelY, double orientation);
}
