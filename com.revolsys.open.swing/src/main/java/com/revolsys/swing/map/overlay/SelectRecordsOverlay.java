package com.revolsys.swing.map.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.List;

import org.jdesktop.swingx.color.ColorUtil;

import com.revolsys.awt.WebColors;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.swing.Icons;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.map.MapPanel;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.map.layer.Layer;
import com.revolsys.swing.map.layer.LayerGroup;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.map.layer.record.renderer.AbstractRecordLayerRenderer;
import com.revolsys.swing.map.layer.record.renderer.TextStyleRenderer;
import com.revolsys.swing.parallel.Invoke;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class SelectRecordsOverlay extends AbstractOverlay {
  protected static final BasicStroke BOX_STROKE = new BasicStroke(2, BasicStroke.CAP_SQUARE,
    BasicStroke.JOIN_MITER, 2, new float[] {
      6, 6
  }, 0f);

  private static final Color COLOR_BOX = WebColors.Green;

  private static final Color COLOR_BOX_TRANSPARENT = ColorUtil.setAlpha(COLOR_BOX, 127);

  private static final Cursor CURSOR_SELECT_BOX = Icons.getCursor("cursor_select_box", 9, 9);

  private static final Cursor CURSOR_SELECT_BOX_ADD = Icons.getCursor("cursor_select_box_add", 9,
    9);

  private static final Cursor CURSOR_SELECT_BOX_DELETE = Icons.getCursor("cursor_select_box_delete",
    9, 9);

  public static final SelectedRecordsRenderer SELECT_RENDERER = new SelectedRecordsRenderer(
    WebColors.Black, WebColors.Lime);

  public static final SelectedRecordsRenderer HIGHLIGHT_RENDERER = new SelectedRecordsRenderer(
    WebColors.Black, WebColors.Yellow);

  private static final long serialVersionUID = 1L;

  private static final String ACTION_SELECT_RECORDS = "Select Records";

  private Double selectBox;

  private java.awt.Point selectBoxFirstPoint;

  private Cursor selectCursor;

  private int selectBoxButton;

  private Cursor selectBoxCursor;

  public SelectRecordsOverlay(final MapPanel map) {
    super(map);
  }

  public void addSelectedRecords(final BoundingBox boundingBox) {
    final LayerGroup project = getProject();
    addSelectedRecords(project, boundingBox);
    final LayerRendererOverlay overlay = getMap().getLayerOverlay();
    overlay.redraw();
  }

  private void addSelectedRecords(final LayerGroup group, final BoundingBox boundingBox) {

    final double scale = getViewport().getScale();
    final List<Layer> layers = group.getLayers();
    Collections.reverse(layers);
    for (final Layer layer : layers) {
      if (layer instanceof LayerGroup) {
        final LayerGroup childGroup = (LayerGroup)layer;
        addSelectedRecords(childGroup, boundingBox);
      } else if (layer instanceof AbstractRecordLayer) {
        final AbstractRecordLayer recordLayer = (AbstractRecordLayer)layer;
        if (recordLayer.isSelectable(scale)) {
          recordLayer.addSelectedRecords(boundingBox);
        }
      }
    }
  }

  protected void doSelectRecords(final InputEvent event, final BoundingBox boundingBox) {
    String methodName;
    if (SwingUtil.isShiftDown(event)) {
      methodName = "addSelectedRecords";
    } else if (SwingUtil.isAltDown(event)) {
      methodName = "unSelectRecords";
    } else {
      methodName = "selectRecords";
    }
    Invoke.background("Select records", this, methodName, boundingBox);
  }

  protected boolean isSelectable(final AbstractRecordLayer recordLayer) {
    return recordLayer.isSelectable();
  }

  public boolean isSelectEvent(final MouseEvent event) {
    if (event.getButton() == MouseEvent.BUTTON1) {
      final boolean keyPress = SwingUtil.isControlOrMetaDown(event);
      return keyPress;
    }
    return false;
  }

  @Override
  public void keyPressed(final KeyEvent event) {
    final int keyCode = event.getKeyCode();
    if (keyCode == KeyEvent.VK_ESCAPE) {
      selectBoxClear(event);
      repaint();
    } else if (keyCode == KeyEvent.VK_CONTROL || keyCode == KeyEvent.VK_META) {
      setSelectCursor(event);
    } else if (keyCode == KeyEvent.VK_SHIFT) {
      setSelectCursor(event);
    } else if (keyCode == KeyEvent.VK_ALT) {
      setSelectCursor(event);
    }
  }

  @Override
  public void keyReleased(final KeyEvent event) {
    final int keyCode = event.getKeyCode();
    if (keyCode == KeyEvent.VK_CONTROL || keyCode == KeyEvent.VK_META) {
      setSelectCursor(event);
    } else if (keyCode == KeyEvent.VK_SHIFT) {
      setSelectCursor(event);
    } else if (keyCode == KeyEvent.VK_ALT) {
      setSelectCursor(event);
    }
  }

  @Override
  public void keyTyped(final KeyEvent event) {
  }

  @Override
  public void mouseClicked(final MouseEvent event) {
    if (event.getClickCount() == 1 && this.selectCursor != null) {
      final int x = event.getX();
      final int y = event.getY();
      final double[] location = getViewport().toModelCoordinates(x, y);
      final GeometryFactory geometryFactory = getViewportGeometryFactory();
      BoundingBox boundingBox = new BoundingBox(geometryFactory, location[0], location[1]);
      final double modelUnitsPerViewUnit = getViewport().getModelUnitsPerViewUnit();
      boundingBox = boundingBox.expand(modelUnitsPerViewUnit * 5);
      doSelectRecords(event, boundingBox);
      event.consume();
    }
  }

  @Override
  public void mouseDragged(final MouseEvent event) {
    if (selectBoxDrag(event)) {
    }
  }

  @Override
  public void mouseEntered(final MouseEvent event) {
    setSelectCursor(event);
  }

  @Override
  public void mouseExited(final MouseEvent event) {
    this.selectCursor = null;
  }

  @Override
  public void mouseMoved(final MouseEvent event) {
    if (event.getButton() == 0) {
      if (SwingUtil.isControlOrMetaDown(event)) {
        setSelectCursor(event);
        event.consume();
      }
    }
  }

  @Override
  public void mousePressed(final MouseEvent event) {
    if (selectBoxStart(event)) {
    }
  }

  @Override
  public void mouseReleased(final MouseEvent event) {
    if (selectBoxFinish(event)) {
    }
  }

  @Override
  public void paintComponent(final Graphics2D graphics) {
    final LayerGroup layerGroup = getProject();
    paintSelected(graphics, layerGroup);
    paintHighlighted(graphics, layerGroup);
    paintSelectBox(graphics);
  }

  protected void paintHighlighted(final Graphics2D graphics2d, final LayerGroup layerGroup) {
    final Viewport2D viewport = getViewport();
    viewport.setProperty(TextStyleRenderer.DIRECT_DISPLAY, Boolean.TRUE);
    try {
      final GeometryFactory viewportGeometryFactory = getViewportGeometryFactory();
      for (final Layer layer : layerGroup.getLayers()) {
        if (layer instanceof LayerGroup) {
          final LayerGroup childGroup = (LayerGroup)layer;
          paintHighlighted(graphics2d, childGroup);
        } else if (layer instanceof AbstractRecordLayer) {
          final AbstractRecordLayer recordLayer = (AbstractRecordLayer)layer;
          for (final LayerRecord record : recordLayer.getHighlightedRecords()) {
            if (record != null && recordLayer.isVisible(record)) {
              final Geometry geometry = record.getGeometryValue();
              final AbstractRecordLayerRenderer layerRenderer = layer.getRenderer();
              layerRenderer.renderSelectedRecord(viewport, graphics2d, recordLayer, record);
              HIGHLIGHT_RENDERER.paintSelected(viewport, viewportGeometryFactory, graphics2d,
                geometry);
            }
          }
        }
      }
    } finally {
      viewport.setProperty(TextStyleRenderer.DIRECT_DISPLAY, Boolean.FALSE);
    }
  }

  protected void paintSelectBox(final Graphics2D graphics2d) {
    if (this.selectBox != null) {
      graphics2d.setColor(COLOR_BOX);
      graphics2d.setStroke(BOX_STROKE);
      graphics2d.draw(this.selectBox);
      graphics2d.setPaint(COLOR_BOX_TRANSPARENT);
      graphics2d.fill(this.selectBox);
    }
  }

  protected void paintSelected(final Graphics2D graphics2d, final LayerGroup layerGroup) {
    final Viewport2D viewport = getViewport();
    viewport.setProperty(TextStyleRenderer.DIRECT_DISPLAY, Boolean.TRUE);
    try {
      final GeometryFactory viewportGeometryFactory = getViewportGeometryFactory();
      for (final Layer layer : layerGroup.getLayers()) {
        if (layer instanceof LayerGroup) {
          final LayerGroup childGroup = (LayerGroup)layer;
          paintSelected(graphics2d, childGroup);
        } else if (layer instanceof AbstractRecordLayer) {
          final AbstractRecordLayer recordLayer = (AbstractRecordLayer)layer;
          final AbstractRecordLayerRenderer layerRenderer = layer.getRenderer();
          if (recordLayer.isSelectable()) {
            for (final LayerRecord record : recordLayer.getSelectedRecords()) {
              if (record != null && recordLayer.isVisible(record)) {
                if (!recordLayer.isHighlighted(record)) {
                  if (!recordLayer.isDeleted(record)) {
                    final Geometry geometry = record.getGeometryValue();
                    layerRenderer.renderSelectedRecord(viewport, graphics2d, recordLayer, record);
                    SELECT_RENDERER.paintSelected(viewport, viewportGeometryFactory, graphics2d,
                      geometry);
                  }
                }
              }
            }
          }
        }
      }
    } finally {
      viewport.setProperty(TextStyleRenderer.DIRECT_DISPLAY, Boolean.FALSE);
    }
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    final String propertyName = event.getPropertyName();
    if ("layers".equals(propertyName)) {
      repaint();
    } else if ("selectable".equals(propertyName)) {
      repaint();
    } else if ("visible".equals(propertyName)) {
      repaint();
    } else if ("editable".equals(propertyName)) {
      repaint();
    } else if ("updateRecord".equals(propertyName)) {
      repaint();
    } else if ("hasSelectedRecords".equals(propertyName)) {
      clearUndoHistory();
    }
  }

  private void selectBoxClear(final InputEvent event) {
    clearOverlayAction(ACTION_SELECT_RECORDS);
    this.selectBox = null;
    this.selectBoxCursor = null;
    this.selectBoxFirstPoint = null;
    setSelectCursor(event);
  }

  public boolean selectBoxDrag(final MouseEvent event) {
    if (isOverlayAction(ACTION_SELECT_RECORDS)) {
      final double width = Math.abs(event.getX() - this.selectBoxFirstPoint.getX());
      final double height = Math.abs(event.getY() - this.selectBoxFirstPoint.getY());
      final java.awt.Point topLeft = new java.awt.Point(); // java.awt.Point
      if (this.selectBoxFirstPoint.getX() < event.getX()) {
        topLeft.setLocation(this.selectBoxFirstPoint.getX(), 0);
      } else {
        topLeft.setLocation(event.getX(), 0);
      }

      if (this.selectBoxFirstPoint.getY() < event.getY()) {
        topLeft.setLocation(topLeft.getX(), this.selectBoxFirstPoint.getY());
      } else {
        topLeft.setLocation(topLeft.getX(), event.getY());
      }
      this.selectBox.setRect(topLeft.getX(), topLeft.getY(), width, height);
      event.consume();
      setSelectCursor(event);
      repaint();
      return true;
    }
    return false;
  }

  public boolean selectBoxFinish(final MouseEvent event) {
    if (event.getButton() == this.selectBoxButton) {
      if (clearOverlayAction(ACTION_SELECT_RECORDS)) {
        // Convert first point to envelope top left in map coords.
        final int minX = (int)this.selectBox.getMinX();
        final int minY = (int)this.selectBox.getMinY();
        final Point topLeft = getViewport().toModelPoint(minX, minY);

        // Convert second point to envelope bottom right in map coords.
        final int maxX = (int)this.selectBox.getMaxX();
        final int maxY = (int)this.selectBox.getMaxY();
        final Point bottomRight = getViewport().toModelPoint(maxX, maxY);

        final GeometryFactory geometryFactory = getMap().getGeometryFactory();
        final BoundingBox boundingBox = new BoundingBox(geometryFactory, topLeft.getX(),
          topLeft.getY(), bottomRight.getX(), bottomRight.getY());

        if (!boundingBox.isEmpty()) {
          doSelectRecords(event, boundingBox);
        }
        selectBoxClear(event);
        repaint();
        event.consume();
        return true;
      }
    }
    return false;
  }

  public boolean selectBoxStart(final MouseEvent event) {
    if (this.selectCursor != null || SwingUtil.isControlOrMetaDown(event)) {
      if (setOverlayAction(ACTION_SELECT_RECORDS)) {
        if (this.selectBoxCursor == null) {
          this.selectCursor = CURSOR_SELECT_BOX;
        }
        this.selectBoxCursor = this.selectCursor;
        this.selectBoxButton = event.getButton();
        this.selectBoxFirstPoint = event.getPoint();
        this.selectBox = new Rectangle2D.Double();
        event.consume();
        return true;
      }
    }
    return false;
  }

  public void selectRecords(final BoundingBox boundingBox) {
    final LayerGroup project = getProject();
    selectRecords(project, boundingBox);
    final LayerRendererOverlay overlay = getMap().getLayerOverlay();
    overlay.redraw();
  }

  private void selectRecords(final LayerGroup group, final BoundingBox boundingBox) {

    final double scale = getViewport().getScale();
    final List<Layer> layers = group.getLayers();
    Collections.reverse(layers);
    for (final Layer layer : layers) {
      if (layer instanceof LayerGroup) {
        final LayerGroup childGroup = (LayerGroup)layer;
        selectRecords(childGroup, boundingBox);
      } else if (layer instanceof AbstractRecordLayer) {
        final AbstractRecordLayer recordLayer = (AbstractRecordLayer)layer;
        if (recordLayer.isSelectable(scale)) {
          recordLayer.setSelectedRecords(boundingBox);
        } else {
          recordLayer.clearSelectedRecords();
        }
      }
    }
  }

  protected void setSelectCursor(final InputEvent event) {
    Cursor cursor = null;
    if (SwingUtil.isControlOrMetaDown(event) || isOverlayAction(ACTION_SELECT_RECORDS)) {
      if (SwingUtil.isShiftDown(event)) {
        cursor = CURSOR_SELECT_BOX_ADD;
      } else if (SwingUtil.isAltDown(event)) {
        cursor = CURSOR_SELECT_BOX_DELETE;
      } else {
        cursor = CURSOR_SELECT_BOX;
      }
    } else {
      cursor = null;
    }
    if (cursor == null) {
      clearMapCursor(this.selectCursor);
    } else {
      setMapCursor(cursor);
    }
    this.selectCursor = cursor;
  }

  public void unSelectRecords(final BoundingBox boundingBox) {
    final LayerGroup project = getProject();
    unSelectRecords(project, boundingBox);
    final LayerRendererOverlay overlay = getMap().getLayerOverlay();
    overlay.redraw();
  }

  private void unSelectRecords(final LayerGroup group, final BoundingBox boundingBox) {

    final double scale = getViewport().getScale();
    final List<Layer> layers = group.getLayers();
    Collections.reverse(layers);
    for (final Layer layer : layers) {
      if (layer instanceof LayerGroup) {
        final LayerGroup childGroup = (LayerGroup)layer;
        unSelectRecords(childGroup, boundingBox);
      } else if (layer instanceof AbstractRecordLayer) {
        final AbstractRecordLayer recordLayer = (AbstractRecordLayer)layer;
        if (recordLayer.isSelectable(scale)) {
          recordLayer.unSelectRecords(boundingBox);
        }
      }
    }
  }
}
