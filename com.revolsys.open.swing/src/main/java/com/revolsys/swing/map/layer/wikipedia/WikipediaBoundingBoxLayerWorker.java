package com.revolsys.swing.map.layer.wikipedia;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import com.revolsys.gis.algorithm.index.RecordQuadTree;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.ProjectedCoordinateSystem;
import com.revolsys.io.map.InvokeMethodMapObjectFactory;
import com.revolsys.io.map.MapObjectFactory;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.record.Record;
import com.revolsys.swing.Icons;
import com.revolsys.swing.map.layer.geonames.GeoNamesService;
import com.revolsys.swing.map.layer.record.BoundingBoxRecordLayer;
import com.revolsys.swing.map.layer.record.renderer.MarkerStyleRenderer;
import com.revolsys.swing.map.layer.record.style.MarkerStyle;
import com.revolsys.swing.map.layer.record.style.marker.ImageMarker;
import com.revolsys.swing.parallel.AbstractSwingWorker;
import com.vividsolutions.jts.geom.Point;

public class WikipediaBoundingBoxLayerWorker extends AbstractSwingWorker<RecordQuadTree, Void> {

  public static final MapObjectFactory FACTORY = new InvokeMethodMapObjectFactory("wikipedia",
    "Wikipedia Articles", WikipediaBoundingBoxLayerWorker.class, "create");

  public static BoundingBoxRecordLayer create(final Map<String, Object> properties) {
    final GeometryFactory wgs84 = GeometryFactory.floating3(4326);
    final BoundingBoxRecordLayer layer1 = new BoundingBoxRecordLayer("wikipedia",
      "Wikipedia Articles", WikipediaBoundingBoxLayerWorker.class, wgs84);

    final BufferedImage image = Icons.getImage("wikipedia");
    final ImageMarker marker = new ImageMarker(image);
    final MarkerStyle style = new MarkerStyle();
    style.setMarker(marker);
    layer1.setRenderer(new MarkerStyleRenderer(layer1, style));
    final BoundingBoxRecordLayer layer = layer1;
    layer.setProperties(properties);
    return layer;
  }

  private final BoundingBox boundingBox;

  private final GeometryFactory geometryFactory;

  private final GeoNamesService geoNamesService = new GeoNamesService();

  private final BoundingBoxRecordLayer layer;

  public WikipediaBoundingBoxLayerWorker(final BoundingBoxRecordLayer layer,
    final BoundingBox boundingBox) {
    this.layer = layer;
    this.boundingBox = boundingBox;
    this.geometryFactory = boundingBox.getGeometryFactory();
  }

  @Override
  protected RecordQuadTree doInBackground() throws Exception {
    BoundingBox boundingBox = this.boundingBox;
    GeometryFactory geometryFactory = this.geometryFactory;
    final CoordinateSystem coordinateSystem = geometryFactory.getCoordinateSystem();
    if (coordinateSystem instanceof ProjectedCoordinateSystem) {
      final ProjectedCoordinateSystem projCs = (ProjectedCoordinateSystem)coordinateSystem;
      geometryFactory = GeometryFactory.getFactory(projCs.getGeographicCoordinateSystem());
      boundingBox = new BoundingBox(geometryFactory, boundingBox);
    }
    final List<Record> results = this.geoNamesService.getWikipediaArticles(boundingBox);
    for (final Record record : results) {
      final String title = record.getValue("title");
      final String wikipediaUrl = record.getValue("wikipediaUrl");
      final String thumbnailImage = record.getValue("thumbnailImg");
      final Point point = record.getGeometry();
      String text;
      if (thumbnailImage != null) {
        text = "<html><b>" + title + "</b><br /><img src=\"" + thumbnailImage
          + "\" /><br /></html>";
      } else {
        text = "<html><b>" + title + "</b><br /></html>";
      }

      // if (viewport instanceof ComponentViewport2D) {
      // final ComponentViewport2D componentViewport =
      // (ComponentViewport2D)viewport;
      // componentViewport.addHotSpot(geometryFactory, point, text, "http://"
      // + wikipediaUrl);
      // }
    }
    final RecordQuadTree index = new RecordQuadTree(results);
    return index;
  }

  @Override
  public String toString() {
    return "Load Wikipedia Articles";
  }

  @Override
  protected void uiTask() {
    try {
      final RecordQuadTree index = get();
      this.layer.setIndex(this.boundingBox, index);
    } catch (final Throwable e) {
      this.layer.setIndex(this.boundingBox, null);
    }
  }
}
