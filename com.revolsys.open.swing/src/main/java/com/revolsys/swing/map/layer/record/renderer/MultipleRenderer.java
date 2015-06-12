package com.revolsys.swing.map.layer.record.renderer;

import java.awt.Graphics2D;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.swing.Icons;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.map.layer.LayerRenderer;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.map.layer.record.style.GeometryStyle;
import com.revolsys.util.ExceptionUtil;
import com.vividsolutions.jts.geom.TopologyException;

/**
 * Use all the specified renderers to render the layer. All features are
 * rendered using the first renderer, then the second etc.
 */
public class MultipleRenderer extends AbstractMultipleRenderer {

  private static final Icon ICON = Icons.getIcon("style_multiple");

  public MultipleRenderer(final AbstractRecordLayer layer, final LayerRenderer<?> parent) {
    this(layer, parent, Collections.<String, Object> emptyMap());
  }

  public MultipleRenderer(final AbstractRecordLayer layer, final LayerRenderer<?> parent,
    final Map<String, Object> multipleStyle) {
    super("multipleStyle", layer, parent, multipleStyle);
    setIcon(ICON);
  }

  public void addStyle(final GeometryStyle style) {
    final GeometryStyleRenderer renderer = new GeometryStyleRenderer(getLayer(), this, style);
    addRenderer(renderer);
  }

  // Needed for filter styles
  @Override
  public void renderRecord(final Viewport2D viewport, final Graphics2D graphics,
    final BoundingBox visibleArea, final AbstractRecordLayer layer, final LayerRecord record) {
    if (isVisible(record)) {
      for (final AbstractRecordLayerRenderer renderer : getRenderers()) {
        final long scale = (long)viewport.getScale();
        if (renderer.isVisible(scale)) {
          try {
            renderer.renderRecord(viewport, graphics, visibleArea, layer, record);
          } catch (final TopologyException e) {
          } catch (final Throwable e) {
            ExceptionUtil.log(getClass(),
              "Unabled to render " + layer.getName() + " #" + record.getIdString(), e);
          }
        }
      }
    }
  }

  @Override
  protected void renderRecords(final Viewport2D viewport, final Graphics2D graphics,
    final AbstractRecordLayer layer, final List<LayerRecord> records) {
    final BoundingBox visibleArea = viewport.getBoundingBox();
    for (final AbstractRecordLayerRenderer renderer : getRenderers()) {
      final long scale = (long)viewport.getScale();
      if (renderer.isVisible(scale)) {
        for (final LayerRecord record : records) {
          if (isVisible(record) && renderer.isVisible(record) && !layer.isHidden(record)) {
            try {
              renderer.renderRecord(viewport, graphics, visibleArea, layer, record);
            } catch (final TopologyException e) {
            } catch (final Throwable e) {
              ExceptionUtil.log(getClass(),
                "Unabled to render " + layer.getName() + " #" + record.getIdString(), e);
            }
          }
        }
      }
    }
  }

  @Override
  public void renderSelectedRecord(final Viewport2D viewport, final Graphics2D graphics,
    final AbstractRecordLayer layer, final LayerRecord object) {
    if (isVisible(object)) {
      for (final AbstractRecordLayerRenderer renderer : getRenderers()) {
        final long scale = (long)viewport.getScale();
        if (renderer.isVisible(scale)) {
          try {
            renderer.renderSelectedRecord(viewport, graphics, layer, object);
          } catch (final Throwable e) {
            ExceptionUtil.log(getClass(),
              "Unabled to render " + layer.getName() + " #" + object.getIdString(), e);
          }
        }
      }
    }
  }
}
