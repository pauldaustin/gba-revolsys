package com.revolsys.swing.map.layer.record.renderer;

import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.slf4j.LoggerFactory;

import com.revolsys.data.record.Record;
import com.revolsys.filter.AcceptAllFilter;
import com.revolsys.filter.Filter;
import com.revolsys.gis.data.model.filter.MultipleAttributeValuesFilter;
import com.revolsys.io.map.MapSerializerUtil;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.swing.Icons;
import com.revolsys.swing.action.InvokeMethodAction;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.map.layer.AbstractLayerRenderer;
import com.revolsys.swing.map.layer.LayerRenderer;
import com.revolsys.swing.map.layer.menu.TreeItemScaleMenu;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.map.layer.record.SqlLayerFilter;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.tree.TreeItemPropertyEnableCheck;
import com.revolsys.swing.tree.TreeItemRunnable;
import com.revolsys.swing.tree.model.ObjectTreeModel;
import com.revolsys.util.ExceptionUtil;
import com.revolsys.util.JavaBeanUtil;
import com.revolsys.util.Property;
import com.vividsolutions.jts.geom.TopologyException;

public abstract class AbstractDataObjectLayerRenderer extends
AbstractLayerRenderer<AbstractRecordLayer> {

  static {
    final MenuFactory menu = ObjectTreeModel.getMenu(AbstractDataObjectLayerRenderer.class);
    menu.addMenuItem("layer", TreeItemRunnable.createAction("View/Edit Style", "palette",
      new TreeItemPropertyEnableCheck("editing", false), "showProperties"));
    menu.addMenuItem("layer", TreeItemRunnable.createAction("Delete", "delete",
      new TreeItemPropertyEnableCheck("parent", null, true), "delete"));

    menu.addComponentFactory("scale", new TreeItemScaleMenu(true));
    menu.addComponentFactory("scale", new TreeItemScaleMenu(false));

    for (final String type : Arrays.asList("Multiple", "Filter", "Scale")) {
      final String iconName = ("style_" + type + "_wrap").toLowerCase();
      final ImageIcon icon = Icons.getIcon(iconName);
      final InvokeMethodAction action = TreeItemRunnable.createAction("Wrap With " + type
        + " Style", icon, null, "wrapWith" + type + "Style");
      menu.addMenuItem("wrap", action);
    }
  }

  private static final AcceptAllFilter<Record> DEFAULT_FILTER = new AcceptAllFilter<Record>();

  public static Filter<Record> getFilter(final AbstractRecordLayer layer,
    final Map<String, Object> style) {
    @SuppressWarnings("unchecked")
    Map<String, Object> filterDefinition = (Map<String, Object>)style.get("filter");
    if (filterDefinition != null) {
      filterDefinition = new LinkedHashMap<String, Object>(filterDefinition);
      final String type = (String)filterDefinition.remove("type");
      if ("valueFilter".equals(type)) {
        return new MultipleAttributeValuesFilter(filterDefinition);
      } else if ("queryFilter".equals(type)) {
        String query = (String)filterDefinition.remove("query");
        if (Property.hasValue(query)) {
          query = query.replaceAll("!= null", "IS NOT NULL");
          query = query.replaceAll("== null", "IS NULL");
          query = query.replaceAll("==", "=");
          query = query.replaceAll("!=", "<>");
          query = query.replaceAll("\\{(.*)\\}.contains\\((.*)\\)", "$2 IN ($1)");
          query = query.replaceAll("\\[(.*)\\]", "$1");
          query = query.replaceAll("(.*).startsWith\\('(.*)'\\)", "$1 LIKE '$2%'");
          query = query.replaceAll("#systemProperties\\['user.name'\\]", "'{gbaUsername}'");
          return new SqlLayerFilter(layer, query);
        }
      } else if ("sqlFilter".equals(type)) {
        final String query = (String)filterDefinition.remove("query");
        if (Property.hasValue(query)) {
          return new SqlLayerFilter(layer, query);
        }
      } else {
        LoggerFactory.getLogger(AbstractDataObjectLayerRenderer.class).error(
          "Unknown filter type " + type);
      }
    }
    return DEFAULT_FILTER;
  }

  public static AbstractDataObjectLayerRenderer getRenderer(final AbstractRecordLayer layer,
    final LayerRenderer<?> parent, final Map<String, Object> style) {
    final String type = (String)style.remove("type");
    if ("geometryStyle".equals(type)) {
      return new GeometryStyleRenderer(layer, parent, style);
    } else if ("textStyle".equals(type)) {
      return new TextStyleRenderer(layer, parent, style);
    } else if ("markerStyle".equals(type)) {
      return new MarkerStyleRenderer(layer, parent, style);
    } else if ("multipleStyle".equals(type)) {
      return new MultipleRenderer(layer, parent, style);
    } else if ("scaleStyle".equals(type)) {
      return new ScaleMultipleRenderer(layer, parent, style);
    } else if ("filterStyle".equals(type)) {
      return new FilterMultipleRenderer(layer, parent, style);
    }
    LoggerFactory.getLogger(AbstractDataObjectLayerRenderer.class).error(
      "Unknown style type: " + style);
    return null;
  }

  public static LayerRenderer<AbstractRecordLayer> getRenderer(final AbstractRecordLayer layer,
    final Map<String, Object> style) {
    return getRenderer(layer, null, style);
  }

  private Filter<Record> filter = DEFAULT_FILTER;

  public AbstractDataObjectLayerRenderer(final String type, final String name,
    final AbstractRecordLayer layer, final LayerRenderer<?> parent) {
    this(type, name, layer, parent, Collections.<String, Object> emptyMap());
  }

  public AbstractDataObjectLayerRenderer(final String type, final String name,
    final AbstractRecordLayer layer, final LayerRenderer<?> parent, final Map<String, Object> style) {
    super(type, name, layer, parent, style);
    this.filter = getFilter(layer, style);
  }

  @Override
  public AbstractDataObjectLayerRenderer clone() {
    final AbstractDataObjectLayerRenderer clone = (AbstractDataObjectLayerRenderer)super.clone();
    clone.filter = JavaBeanUtil.clone(this.filter);
    return clone;
  }

  public void delete() {
    final LayerRenderer<?> parent = getParent();
    if (parent instanceof AbstractMultipleRenderer) {
      final AbstractMultipleRenderer multiple = (AbstractMultipleRenderer)parent;
      multiple.removeRenderer(this);
    }
  }

  public String getQueryFilter() {
    if (this.filter instanceof SqlLayerFilter) {
      final SqlLayerFilter layerFilter = (SqlLayerFilter)this.filter;
      return layerFilter.getQuery();
    } else {
      return null;
    }
  }

  protected boolean isFilterAccept(final LayerRecord record) {
    try {
      return this.filter.accept(record);
    } catch (final Throwable e) {
      return false;
    }
  }

  public boolean isVisible(final LayerRecord record) {
    if (isVisible() && !record.isDeleted()) {
      return isFilterAccept(record);
    } else {
      return false;
    }
  }

  @Override
  public void render(final Viewport2D viewport, final Graphics2D graphics,
    final AbstractRecordLayer layer) {
    if (layer.hasGeometryAttribute()) {
      final boolean saved = viewport.setUseModelCoordinates(true, graphics);
      try {
        final BoundingBox boundingBox = viewport.getBoundingBox();
        final List<LayerRecord> dataObjects = layer.queryBackground(boundingBox);
        renderRecords(viewport, graphics, layer, dataObjects);
      } finally {
        viewport.setUseModelCoordinates(saved, graphics);
      }
    }
  }

  public void renderRecord(final Viewport2D viewport, final Graphics2D graphics,
    final BoundingBox visibleArea, final AbstractRecordLayer layer, final LayerRecord record) {
  }

  protected void renderRecords(final Viewport2D viewport, final Graphics2D graphics,
    final AbstractRecordLayer layer, final List<LayerRecord> records) {
    final BoundingBox visibleArea = viewport.getBoundingBox();
    for (final LayerRecord record : records) {
      if (record != null) {
        if (isVisible(record) && !layer.isHidden(record)) {
          try {
            renderRecord(viewport, graphics, visibleArea, layer, record);
          } catch (final TopologyException e) {
          } catch (final Throwable e) {
            ExceptionUtil.log(getClass(),
              "Unabled to render " + layer.getName() + " #" + record.getIdString(), e);
          }
        }
      }
    }
  }

  public void renderSelectedRecord(final Viewport2D viewport, final Graphics2D graphics,
    final AbstractRecordLayer layer, final LayerRecord record) {
    final BoundingBox boundingBox = viewport.getBoundingBox();
    if (isVisible(record)) {
      try {
        renderRecord(viewport, graphics, boundingBox, layer, record);
      } catch (final TopologyException e) {
      }
    }
  }

  protected void replace(final AbstractRecordLayer layer, final AbstractMultipleRenderer parent,
    final AbstractMultipleRenderer newRenderer) {
    if (parent == null) {
      if (isEditing()) {
        newRenderer.setEditing(true);
        firePropertyChange("replaceRenderer", this, newRenderer);
      } else {
        layer.setRenderer(newRenderer);
      }
    } else {
      final int index = parent.removeRenderer(this);
      parent.addRenderer(index, newRenderer);
    }
  }

  @Override
  public void setName(final String name) {
    final AbstractMultipleRenderer parent = (AbstractMultipleRenderer)getParent();
    String newName = name;
    if (parent != null) {
      int i = 1;
      while (parent.hasRendererWithSameName(this, newName)) {
        newName = name + i;
        i++;
      }
    }
    super.setName(newName);
  }

  public void setQueryFilter(final String query) {
    if (this.filter instanceof SqlLayerFilter || this.filter instanceof AcceptAllFilter) {
      if (Property.hasValue(query)) {
        final AbstractRecordLayer layer = getLayer();
        this.filter = new SqlLayerFilter(layer, query);
      } else {
        this.filter = new AcceptAllFilter<Record>();
      }
    }
  }

  @Override
  public Map<String, Object> toMap() {
    final Map<String, Object> map = super.toMap();
    if (!(this.filter instanceof AcceptAllFilter)) {
      MapSerializerUtil.add(map, "filter", this.filter);
    }
    return map;
  }

  protected void wrap(final AbstractRecordLayer layer, final AbstractMultipleRenderer parent,
    final AbstractMultipleRenderer newRenderer) {
    newRenderer.addRenderer(this.clone());
    replace(layer, parent, newRenderer);
  }

  public FilterMultipleRenderer wrapWithFilterStyle() {
    final AbstractRecordLayer layer = getLayer();
    final AbstractMultipleRenderer parent = (AbstractMultipleRenderer)getParent();
    final FilterMultipleRenderer newRenderer = new FilterMultipleRenderer(layer, parent);
    wrap(layer, parent, newRenderer);
    return newRenderer;
  }

  public MultipleRenderer wrapWithMultipleStyle() {
    final AbstractRecordLayer layer = getLayer();
    final AbstractMultipleRenderer parent = (AbstractMultipleRenderer)getParent();
    final MultipleRenderer newRenderer = new MultipleRenderer(layer, parent);
    wrap(layer, parent, newRenderer);
    return newRenderer;
  }

  public ScaleMultipleRenderer wrapWithScaleStyle() {
    final AbstractRecordLayer layer = getLayer();
    final AbstractMultipleRenderer parent = (AbstractMultipleRenderer)getParent();
    final ScaleMultipleRenderer newRenderer = new ScaleMultipleRenderer(layer, parent);
    wrap(layer, parent, newRenderer);
    return newRenderer;
  }
}
