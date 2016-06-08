package com.revolsys.swing.map.layer.arcgisrest;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;

import com.revolsys.collection.map.MapEx;
import com.revolsys.collection.map.Maps;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.io.PathName;
import com.revolsys.io.map.MapObjectFactoryRegistry;
import com.revolsys.logging.Logs;
import com.revolsys.record.Record;
import com.revolsys.record.io.RecordReader;
import com.revolsys.record.io.format.esri.rest.ArcGisRestCatalog;
import com.revolsys.record.io.format.esri.rest.map.FeatureLayer;
import com.revolsys.record.query.Query;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.swing.map.layer.AbstractLayer;
import com.revolsys.swing.map.layer.LayerGroup;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.map.layer.record.renderer.AbstractMultipleRenderer;
import com.revolsys.swing.map.layer.record.renderer.AbstractRecordLayerRenderer;
import com.revolsys.swing.map.layer.record.renderer.FilterMultipleRenderer;
import com.revolsys.swing.map.layer.record.renderer.GeometryStyleRenderer;
import com.revolsys.swing.map.layer.record.renderer.MarkerStyleRenderer;
import com.revolsys.swing.map.layer.record.renderer.MultipleRenderer;
import com.revolsys.swing.map.layer.record.renderer.TextStyleRenderer;
import com.revolsys.swing.map.layer.record.style.GeometryStyle;
import com.revolsys.swing.map.layer.record.style.MarkerStyle;
import com.revolsys.swing.map.layer.record.style.TextStyle;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.menu.Menus;
import com.revolsys.util.OS;
import com.revolsys.util.Property;

public class ArcGisRestServerRecordLayer extends AbstractRecordLayer {
  public static final String J_TYPE = "arcGisRestServerRecordLayer";

  private static final Map<String, List<Double>> LINE_STYLE_PATTERNS = Maps
    .<String, List<Double>> buildHash() //
    .add("esriSLSDash", GeometryStyle.DASH_5) //
    .add("esriSLSDashDot", GeometryStyle.DASH_DOT) //
    .add("esriSLSDashDotDot", GeometryStyle.DASH_DOT_DOT) //
    .add("esriSLSDot", GeometryStyle.DOT) //
    .add("esriSLSNull", null) //
    .add("esriSLSSolid", Collections.emptyList()) //
    .getMap();

  private static void actionAddLayer(final FeatureLayer layerDescription) {
    final Project project = Project.get();
    if (project != null) {

      LayerGroup layerGroup = project;
      final PathName layerPath = layerDescription.getPathName();
      for (final String groupName : layerPath.getParent().getElements()) {
        layerGroup = layerGroup.addLayerGroup(groupName);
      }
      final ArcGisRestServerRecordLayer layer = new ArcGisRestServerRecordLayer(layerDescription);
      layerGroup.addLayer(layer);
      if (OS.getPreferenceBoolean("com.revolsys.gis", AbstractLayer.PREFERENCE_PATH,
        AbstractLayer.PREFERENCE_NEW_LAYERS_SHOW_TABLE_VIEW, false)) {
        layer.showTableView();
      }
    }
  }

  public static Color getColor(final MapEx properties) {
    final String fieldName = "color";
    return getColor(properties, fieldName);
  }

  public static Color getColor(final MapEx properties, final String fieldName) {
    final List<Number> colorValues = properties.getValue(fieldName);
    if (colorValues != null && colorValues.size() == 4) {
      final int red = colorValues.get(0).intValue();
      final int green = colorValues.get(1).intValue();
      final int blue = colorValues.get(2).intValue();
      final int alpha = colorValues.get(3).intValue();
      return new Color(red, green, blue, alpha);
    }
    return null;
  }

  public static void mapObjectFactoryInit() {
    MapObjectFactoryRegistry.newFactory(J_TYPE, "Arc GIS REST Server Record Layer",
      ArcGisRestServerRecordLayer::new);

    final MenuFactory recordLayerDescriptionMenu = MenuFactory.getMenu(FeatureLayer.class);

    Menus.addMenuItem(recordLayerDescriptionMenu, "default", "Add Layer", "map_add",
      ArcGisRestServerRecordLayer::actionAddLayer, false);
  }

  private FeatureLayer layerDescription;

  private String url;

  private PathName layerPath;

  public ArcGisRestServerRecordLayer() {
    super(J_TYPE);
    setReadOnly(true);
  }

  public ArcGisRestServerRecordLayer(final FeatureLayer layerDescription) {
    this();
    setLayerDescription(layerDescription);
    setProperties(Collections.emptyMap());
  }

  public ArcGisRestServerRecordLayer(final Map<String, ? extends Object> properties) {
    this();
    setProperties(properties);
  }

  private void addTextRenderer(final AbstractMultipleRenderer renderers,
    final MapEx labelProperties) {
    final TextStyle textStyle = new TextStyle();
    final String alignment = labelProperties.getString("labelPlacement");
    if (alignment.endsWith("Left")) {
      textStyle.setTextHorizontalAlignment("right");
    } else if (alignment.endsWith("Right")) {
      textStyle.setTextHorizontalAlignment("left");
    } else if (alignment.endsWith("Before")) {
      textStyle.setTextHorizontalAlignment("right");
      textStyle.setTextPlacementType("vertex(0)");
    } else if (alignment.endsWith("Start")) {
      textStyle.setTextHorizontalAlignment("left");
      textStyle.setTextPlacementType("vertex(0)");
    } else if (alignment.endsWith("After")) {
      textStyle.setTextHorizontalAlignment("left");
      textStyle.setTextPlacementType("vertex(n)");
    } else if (alignment.endsWith("End")) {
      textStyle.setTextHorizontalAlignment("right");
      textStyle.setTextPlacementType("vertex(n)");
    } else if (alignment.endsWith("Along")) {
      textStyle.setTextHorizontalAlignment("center");
      textStyle.setTextPlacementType("auto");
    } else {
      textStyle.setTextHorizontalAlignment("center");
    }
    if (alignment.contains("Above")) {
      textStyle.setTextVerticalAlignment("bottom");
    } else if (alignment.endsWith("Below")) {
      textStyle.setTextVerticalAlignment("top");
    } else {
      textStyle.setTextVerticalAlignment("center");
    }

    String textName = labelProperties.getString("labelExpression");
    textName = textName.replace(" CONCAT ", "");
    textName = textName.replaceAll("\"([^\"]+)\"", "$1");
    textStyle.setTextName(textName);
    final MapEx symbol = labelProperties.getValue("symbol");
    if ("esriTS".equals(symbol.getString("type"))) {
      final Color textFill = getColor(symbol);
      textStyle.setTextFill(textFill);

      final Color backgroundColor = getColor(symbol, "backgroundColor");
      textStyle.setTextBoxColor(backgroundColor);

      // "useCodedValues": false,
      // "borderLineColor": null,
      // "verticalAlignment": "bottom",
      // "horizontalAlignment": "left",
      // "rightToLeft": false,
      // "kerning": true,

      final double angle = symbol.getDouble("angle", 0);
      textStyle.setTextOrientation(angle);

      final Measure<Length> textDx = Measure.valueOf(symbol.getDouble("xoffset", 0), NonSI.PIXEL);
      textStyle.setTextDx(textDx);

      final Measure<Length> textDy = Measure.valueOf(symbol.getDouble("yoffset", 0), NonSI.PIXEL);
      textStyle.setTextDx(textDy);

      final MapEx font = symbol.getValue("font");
      if (font != null) {
        final String faceName = font.getString("family", "Arial");
        textStyle.setTextFaceName(faceName);

        final Measure<Length> size = Measure.valueOf(font.getDouble("size", 10), NonSI.PIXEL);
        textStyle.setTextSize(size);

      }

      // "font": {
      // "style": "normal",
      // "weight": "bold",
      // "decoration": "none"
    }
    final TextStyleRenderer textRenderer = new TextStyleRenderer(this, textStyle);
    textRenderer.setName(textName.replace("[", "").replace("]", ""));

    long minimumScale = labelProperties.getLong("minScale", Long.MAX_VALUE);
    if (minimumScale == 0) {
      minimumScale = Long.MAX_VALUE;
    }
    textRenderer.setMinimumScale(minimumScale);
    final long maximumScale = labelProperties.getLong("maxScale", 0);
    textRenderer.setMaximumScale(maximumScale);

    final String where = labelProperties.getString("where");
    textRenderer.setQueryFilter(where);

    renderers.addRenderer(textRenderer);
  }

  @Override
  public ArcGisRestServerRecordLayer clone() {
    final ArcGisRestServerRecordLayer clone = (ArcGisRestServerRecordLayer)super.clone();
    return clone;
  }

  @Override
  protected void forEachRecord(final Query query, final Consumer<? super LayerRecord> consumer) {
    try (
      RecordReader reader = this.layerDescription.newRecordReader(this::newLayerRecord, query)) {
      for (final Record record : reader) {
        consumer.accept((LayerRecord)record);
      }
    }
  }

  public FeatureLayer getLayerDescription() {
    return this.layerDescription;
  }

  public PathName getLayerPath() {
    return this.layerPath;
  }

  @Override
  public int getRecordCount(final Query query) {
    if (this.layerDescription == null) {
      return 0;
    } else {
      return this.layerDescription.getRecordCount(query);
    }
  }

  @Override
  public List<LayerRecord> getRecords(BoundingBox boundingBox) {
    if (hasGeometryField()) {
      boundingBox = convertBoundingBox(boundingBox);
      if (Property.hasValue(boundingBox)) {
        final List<LayerRecord> records = this.layerDescription.getRecords(this::newLayerRecord,
          boundingBox);
        return records;
      }
    }
    return Collections.emptyList();
  }

  public String getUrl() {
    return this.url;
  }

  @Override
  protected boolean initializeDo() {
    FeatureLayer layerDescription = getLayerDescription();
    if (layerDescription == null) {
      final String url = getUrl();
      final PathName layerPath = getLayerPath();

      if (url == null) {
        Logs.error(this, "An ArcGIS Rest server requires a url: " + getPath());
        return false;
      }
      if (layerPath == null) {
        Logs.error(this, "An ArcGIS Rest server requires a layerPath: " + getPath());
        return false;
      }
      ArcGisRestCatalog server;
      try {
        server = ArcGisRestCatalog.newArcGisRestCatalog(url);
      } catch (final Throwable e) {
        Logs.error(this, "Unable to connect to server: " + url + " for " + getPath(), e);
        return false;
      }
      try {
        layerDescription = server.getWebServiceResource(layerPath, FeatureLayer.class);
      } catch (final IllegalArgumentException e) {
        Logs.error(this, "ArcGIS Rest service is not a layer " + getPath(), e);
        return false;
      }
      if (layerDescription == null) {
        Logs.error(this, "No ArcGIS Rest layer with name: " + layerPath + " for " + getPath());
        return false;
      } else {
        setLayerDescription(layerDescription);
      }
    }

    if (layerDescription != null) {
      final RecordDefinition recordDefinition = layerDescription.getRecordDefinition();
      if (recordDefinition != null) {
        setRecordDefinition(recordDefinition);
        setBoundingBox(layerDescription.getBoundingBox());
        initRenderer();
        return super.initializeDo();
      }
    }
    return false;
  }

  private void initRenderer() {
    final List<AbstractRecordLayerRenderer> renderers = new ArrayList<>();
    final MapEx drawingInfo = this.layerDescription.getProperty("drawingInfo");
    if (drawingInfo != null) {
      final MapEx rendererProperties = drawingInfo.getValue("renderer");
      if (rendererProperties != null) {
        final String rendererType = rendererProperties.getString("type");
        if ("simple".equals(rendererType)) {
          final AbstractRecordLayerRenderer renderer = newSymbolRenderer(rendererProperties,
            "symbol");
          if (renderer != null) {
            renderers.add(renderer);
          }
        } else if ("uniqueValue".equals(rendererType)) {
          final FilterMultipleRenderer filterRenderer = newUniqueValueRenderer(rendererProperties);
          renderers.add(filterRenderer);
        } else {
          Logs.error(this, "Unsupported renderer=" + rendererType + "\n" + rendererProperties);
        }
      }

      final List<MapEx> labellingInfo = drawingInfo.getValue("labelingInfo");
      if (labellingInfo != null) {
        final MultipleRenderer labelRenderer = new MultipleRenderer(this);
        labelRenderer.setName("labels");
        for (final MapEx labelProperties : labellingInfo) {
          addTextRenderer(labelRenderer, labelProperties);
        }
        if (!labelRenderer.isEmpty()) {
          renderers.add(labelRenderer);
        }
      }
    }
    if (renderers.isEmpty()) {
    } else if (renderers.size() == 1) {
      setRenderer(renderers.get(0));
    } else {
      setRenderer(new MultipleRenderer(this, renderers));
    }
  }

  private AbstractRecordLayerRenderer newSimpleFillRenderer(final MapEx symbol) {
    final MapEx outline = symbol.getValue("outline");
    final GeometryStyle style;
    if (outline == null) {
      style = new GeometryStyle();
      style.setLineWidth(Measure.valueOf(0, NonSI.PIXEL));
    } else {
      style = newSimpleLineStyle(outline);
    }
    final Color fillColor = getColor(symbol);
    style.setPolygonFill(fillColor);

    return new GeometryStyleRenderer(this, style);
  }

  private AbstractRecordLayerRenderer newSimpleLineRenderer(final MapEx symbol) {
    final GeometryStyle style = newSimpleLineStyle(symbol);
    return new GeometryStyleRenderer(this, style);
  }

  private GeometryStyle newSimpleLineStyle(final MapEx symbol) {
    final double lineWidth = symbol.getDouble("width", 1.0);
    final Color lineColor = getColor(symbol);

    final GeometryStyle style = GeometryStyle.line(lineColor, lineWidth);
    final String dashStyle = symbol.getString("style");
    if (LINE_STYLE_PATTERNS.containsKey(dashStyle)) {
      final List<Double> dashArray = LINE_STYLE_PATTERNS.get(dashStyle);
      if (dashArray == null) {
        style.setLineWidth(Measure.valueOf(0, NonSI.PIXEL));
      } else if (!dashArray.isEmpty()) {
        style.setLineDashArray(dashArray);
      }
    }
    return style;
  }

  private AbstractRecordLayerRenderer newSimpleMarkerRenderer(final MapEx symbol) {
    String markerName = symbol.getString("style", "esriSMSCirlce");
    markerName = markerName.replace("esriSMS", "").toLowerCase();
    final double markerSize = symbol.getDouble("size", 10);
    final Color markerFill = getColor(symbol);
    Color markerColor = new Color(0, 0, 0, 0);
    final MapEx outline = symbol.getValue("outline");
    double lineWidth = 0;
    if (outline != null) {
      markerColor = getColor(outline);
      lineWidth = outline.getDouble("width", lineWidth);
    }
    final MarkerStyle markerStyle = MarkerStyle.marker(markerName, markerSize, markerColor,
      lineWidth, markerFill);
    return new MarkerStyleRenderer(this, markerStyle);
  }

  private AbstractRecordLayerRenderer newSymbolRenderer(final MapEx rendererProperties,
    final String fieldName) {
    final MapEx symbolProperties = rendererProperties.getValue(fieldName);
    if (symbolProperties == null) {
      return null;
    } else {
      final String symbolType = symbolProperties.getString("type");
      if ("esriSMS".equals(symbolType)) {
        return newSimpleMarkerRenderer(symbolProperties);
      } else if ("esriSLS".equals(symbolType)) {
        return newSimpleLineRenderer(symbolProperties);
      } else if ("esriSFS".equals(symbolType)) {
        return newSimpleFillRenderer(symbolProperties);
      } else {

        // TODO PMS
        // TODO PFS
        Logs.error(this, "Unsupported symbol=" + symbolType + "\n" + symbolType);
        return new GeometryStyleRenderer(this);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private FilterMultipleRenderer newUniqueValueRenderer(final MapEx rendererProperties) {
    final FilterMultipleRenderer filterRenderer = new FilterMultipleRenderer(this);
    final String fieldName = rendererProperties.getString("field1");
    filterRenderer.setName(fieldName);
    for (final MapEx valueProperties : (List<MapEx>)rendererProperties
      .getValue("uniqueValueInfos")) {
      final AbstractRecordLayerRenderer valueRenderer = newSymbolRenderer(valueProperties,
        "symbol");
      if (valueRenderer != null) {
        final String valueLabel = valueProperties.getString("label");
        if (valueLabel != null) {
          valueRenderer.setName(valueLabel);
        }
        final String value = valueProperties.getString("value");
        if (value != null) {
          final FieldDefinition fieldDefinition = getFieldDefinition(fieldName);
          if (fieldDefinition.getDataType().isRequiresQuotes()) {
            valueRenderer.setQueryFilter(fieldName + "='" + value + '\'');
          } else {
            valueRenderer.setQueryFilter(fieldName + " = " + value);
          }
        }
        filterRenderer.addRenderer(valueRenderer);
      }
    }
    final AbstractRecordLayerRenderer defaultRenderer = newSymbolRenderer(rendererProperties,
      "defaultSymbol");
    if (defaultRenderer != null) {
      final String defaultLabel = rendererProperties.getString("defaultLabel", "Default");
      defaultRenderer.setName(defaultLabel);
      filterRenderer.addRenderer(defaultRenderer);
    }
    return filterRenderer;
  }

  public void setLayerDescription(final FeatureLayer layerDescription) {
    this.layerDescription = layerDescription;
    if (layerDescription != null) {
      final String name = layerDescription.getName();
      setName(name);

      final String url = layerDescription.getRootServiceUrl();
      setUrl(url);

      final PathName pathName = layerDescription.getPathName();
      setLayerPath(pathName);

      final long minScale = layerDescription.getMinScale();
      setMinimumScale(minScale);

      final long maxScale = layerDescription.getMaxScale();
      setMaximumScale(maxScale);
    }
  }

  public void setLayerPath(final PathName layerPath) {
    this.layerPath = layerPath;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  @Override
  public MapEx toMap() {
    final MapEx map = super.toMap();
    addToMap(map, "url", this.url);
    addToMap(map, "layerPath", this.layerPath);
    return map;
  }
}
