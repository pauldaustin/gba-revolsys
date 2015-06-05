package com.revolsys.raster;

import java.awt.geom.AffineTransform;
import java.util.LinkedHashMap;
import java.util.Map;

import com.revolsys.beans.AbstractPropertyChangeObject;
import com.revolsys.format.wkt.WktWriter;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.DoubleCoordinates;
import com.revolsys.gis.model.coordinates.list.DoubleCoordinatesList;
import com.revolsys.io.map.MapSerializer;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.util.CollectionUtil;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class MappedLocation extends AbstractPropertyChangeObject implements
  MapSerializer {
  public static Coordinates targetPointToPixel(final BoundingBox boundingBox,
    final Point point, final int imageWidth, final int imageHeight) {
    return toImagePoint(boundingBox, point, imageWidth, imageHeight);
  }

  public static Coordinates toImagePoint(final BoundingBox boundingBox,
    Point modelPoint, final int imageWidth, final int imageHeight) {
    modelPoint = boundingBox.getGeometryFactory().copy(modelPoint);
    final double modelX = modelPoint.getX();
    final double modelY = modelPoint.getY();
    final double modelDeltaX = modelX - boundingBox.getMinX();
    final double modelDeltaY = modelY - boundingBox.getMinY();

    final double modelWidth = boundingBox.getWidth();
    final double modelHeight = boundingBox.getHeight();

    final double xRatio = modelDeltaX / modelWidth;
    final double yRatio = modelDeltaY / modelHeight;

    final double imageX = imageWidth * xRatio;
    final double imageY = imageHeight * yRatio;
    return new DoubleCoordinates(imageX, imageY);
  }

  public static double[] toModelCoordinates(final GeoReferencedImage image,
    final BoundingBox boundingBox, final boolean useTransform,
    final double... coordinates) {
    double[] targetCoordinates;
    if (useTransform) {
      targetCoordinates = new double[10];
      final AffineTransform transform = image.getAffineTransformation(boundingBox);
      transform.transform(coordinates, 0, targetCoordinates, 0,
        coordinates.length / 2);
    } else {
      targetCoordinates = coordinates.clone();
    }
    final int imageWidth = image.getImageWidth();
    final int imageHeight = image.getImageHeight();
    for (int vertexIndex = 0; vertexIndex < coordinates.length / 2; vertexIndex++) {
      final int vertexOffset = vertexIndex * 2;
      final double xPercent = targetCoordinates[vertexOffset] / imageWidth;
      final double yPercent = (imageHeight - targetCoordinates[vertexOffset + 1])
        / imageHeight;

      final double modelWidth = boundingBox.getWidth();
      final double modelHeight = boundingBox.getHeight();

      final double modelX = boundingBox.getMinX() + modelWidth * xPercent;
      final double modelY = boundingBox.getMinY() + modelHeight * yPercent;
      targetCoordinates[vertexOffset] = modelX;
      targetCoordinates[vertexOffset + 1] = modelY;
    }
    return targetCoordinates;
  }

  private Coordinates sourcePixel;

  private Point targetPoint;

  private GeometryFactory geometryFactory = GeometryFactory.floating(0, 2);

  public MappedLocation(final Coordinates sourcePixel, final Point targetPoint) {
    this.sourcePixel = sourcePixel;
    this.targetPoint = targetPoint;
    this.geometryFactory = GeometryFactory.getFactory(targetPoint);
  }

  public MappedLocation(final Map<String, Object> map) {
    final double sourceX = CollectionUtil.getDouble(map, "sourceX", 0.0);
    final double sourceY = CollectionUtil.getDouble(map, "sourceY", 0.0);
    this.sourcePixel = new DoubleCoordinates(sourceX, sourceY);
    this.targetPoint = this.geometryFactory.createGeometry((String)map.get("target"));
  }

  public GeometryFactory getGeometryFactory() {
    return this.geometryFactory;
  }

  public Coordinates getSourcePixel() {
    return this.sourcePixel;
  }

  public Point getSourcePoint(final GeoReferencedImage image,
    final BoundingBox boundingBox, final boolean useTransform) {
    final Coordinates sourcePixel = getSourcePixel();
    final double[] sourcePoint = toModelCoordinates(image, boundingBox,
      useTransform, sourcePixel.getX(),
      image.getImageHeight() - sourcePixel.getY());
    final GeometryFactory geometryFactory = boundingBox.getGeometryFactory();
    return geometryFactory.point(sourcePoint[0], sourcePoint[1]);
  }

  // public Point getSourcePoint(final WarpFilter filter,
  // final BoundingBox boundingBox) {
  // if (filter == null) {
  // return null;
  // } else {
  // final Point sourcePixel = getSourcePixel();
  // final Point sourcePoint = filter.sourcePixelToTargetPoint(boundingBox,
  // sourcePixel);
  // final GeometryFactory geometryFactory = filter.getGeometryFactory();
  // return geometryFactory.point(sourcePoint);
  // }
  // }

  public LineString getSourceToTargetLine(final GeoReferencedImage image,
    final BoundingBox boundingBox, final boolean useTransform) {

    final Coordinates sourcePixel = getSourcePixel();
    final double[] sourcePoint = toModelCoordinates(image, boundingBox,
      useTransform, sourcePixel.getX(),
      image.getImageHeight() - sourcePixel.getY());
    final GeometryFactory geometryFactory = boundingBox.getGeometryFactory();
    final Point targetPoint = getTargetPoint();
    return geometryFactory.createLineString(new DoubleCoordinatesList(2,
      sourcePoint[0], sourcePoint[1], targetPoint.getX(), targetPoint.getY()));
  }

  public Coordinates getTargetPixel(final BoundingBox boundingBox,
    final int imageWidth, final int imageHeight) {
    final GeometryFactory geometryFactory = boundingBox.getGeometryFactory();
    final Point targetPointCoordinates = geometryFactory.copy(this.targetPoint);
    return targetPointToPixel(boundingBox, targetPointCoordinates, imageWidth,
      imageHeight);
  }

  public Point getTargetPoint() {
    return this.targetPoint;
  }

  public void setGeometryFactory(final GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
    this.targetPoint = this.geometryFactory.copy(this.targetPoint);
  }

  public void setSourcePixel(final Coordinates sourcePixel) {
    final Object oldValue = this.sourcePixel;
    this.sourcePixel = sourcePixel;
    firePropertyChange("sourcePixel", oldValue, sourcePixel);
  }

  public void setTargetPoint(final Point targetPoint) {
    final Object oldValue = this.targetPoint;
    this.targetPoint = targetPoint;
    firePropertyChange("targetPoint", oldValue, targetPoint);
  }

  @Override
  public Map<String, Object> toMap() {
    final Map<String, Object> map = new LinkedHashMap<String, Object>();
    map.put("sourceX", this.sourcePixel.getX());
    map.put("sourceY", this.sourcePixel.getY());
    map.put("target", WktWriter.toString(this.targetPoint, true));
    return map;
  }

  @Override
  public String toString() {
    return this.sourcePixel + "->" + this.targetPoint;
  }
}
