package com.revolsys.swing.map.layer.geonames;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import com.revolsys.data.record.Record;
import com.revolsys.famfamfam.silk.SilkIconLoader;
import com.revolsys.gis.algorithm.index.DataObjectQuadTree;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.GeographicCoordinateSystem;
import com.revolsys.gis.cs.ProjectedCoordinateSystem;
import com.revolsys.io.map.InvokeMethodMapObjectFactory;
import com.revolsys.io.map.MapObjectFactory;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.Point;
import com.revolsys.swing.map.layer.record.DataObjectBoundingBoxLayer;
import com.revolsys.swing.map.layer.record.renderer.MarkerStyleRenderer;
import com.revolsys.swing.map.layer.record.style.MarkerStyle;
import com.revolsys.swing.map.layer.record.style.marker.ImageMarker;
import com.revolsys.swing.parallel.AbstractSwingWorker;

public class GeoNamesBoundingBoxLayerWorker extends
  AbstractSwingWorker<DataObjectQuadTree, Void> {

  public static final MapObjectFactory FACTORY = new InvokeMethodMapObjectFactory(
    "geoname", "Geoname.org", GeoNamesBoundingBoxLayerWorker.class, "create");

  public static DataObjectBoundingBoxLayer create(
    final Map<String, Object> properties) {
    final GeometryFactory wgs84 = GeometryFactory.floating3(4326);
    final DataObjectBoundingBoxLayer layer = new DataObjectBoundingBoxLayer(
      "geoname", "Geo Names", GeoNamesBoundingBoxLayerWorker.class, wgs84);

    final BufferedImage image = SilkIconLoader.getImage("world");
    final ImageMarker marker = new ImageMarker(image);
    final MarkerStyle style = new MarkerStyle();
    style.setMarker(marker);
    layer.setRenderer(new MarkerStyleRenderer(layer, style));
    layer.setProperties(properties);
    return layer;
  }

  private final DataObjectBoundingBoxLayer layer;

  private final BoundingBox boundingBox;

  private final GeoNamesService geoNamesService = new GeoNamesService();

  private final GeometryFactory geometryFactory;

  public GeoNamesBoundingBoxLayerWorker(final DataObjectBoundingBoxLayer layer,
    final BoundingBox boundingBox) {
    this.layer = layer;
    this.boundingBox = boundingBox;
    this.geometryFactory = boundingBox.getGeometryFactory();
  }

  @Override
  protected DataObjectQuadTree doInBackground() throws Exception {
    BoundingBox boundingBox = this.boundingBox;
    GeometryFactory geometryFactory = this.geometryFactory;
    final CoordinateSystem coordinateSystem = geometryFactory.getCoordinateSystem();
    if (coordinateSystem instanceof ProjectedCoordinateSystem) {
      final ProjectedCoordinateSystem projCs = (ProjectedCoordinateSystem)coordinateSystem;
      final GeographicCoordinateSystem geoCs = projCs.getGeographicCoordinateSystem();
      geometryFactory = geoCs.getGeometryFactory();
      boundingBox = boundingBox.convert(geometryFactory);
    }
    final List<Record> results = this.geoNamesService.getNames(boundingBox);
    for (final Record dataObject : results) {
      final String name = dataObject.getValue("name");
      final Point point = dataObject.getGeometryValue();
      final String text = "<html><b>" + name + "</b><br /></html>";

      // if (viewport instanceof ComponentViewport2D) {
      // final ComponentViewport2D componentViewport =
      // (ComponentViewport2D)viewport;
      // componentViewport.addHotSpot(geometryFactory, point, text, null);
      // }
    }
    final DataObjectQuadTree index = new DataObjectQuadTree(results);
    return index;
  }

  @Override
  public String toString() {
    return "Load Geo Names";
  }

  @Override
  protected void uiTask() {
    try {
      final DataObjectQuadTree index = get();
      this.layer.setIndex(this.boundingBox, index);
    } catch (final Throwable e) {
      this.layer.setIndex(this.boundingBox, null);
    }
  }
}
