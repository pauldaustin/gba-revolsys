package com.revolsys.gis.format.shape.io.geometry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.format.shape.io.ShapefileConstants;
import com.revolsys.gis.io.EndianOutput;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.io.EndianInput;
import com.revolsys.util.MathUtil;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class MultiPolygonConverter implements ShapefileGeometryConverter {
  private GeometryFactory geometryFactory;

  public MultiPolygonConverter() {
    this(null);
  }

  public MultiPolygonConverter(final GeometryFactory geometryFactory) {
    if (geometryFactory != null) {
      this.geometryFactory = geometryFactory;
    } else {
      this.geometryFactory = new GeometryFactory();
    }
  }

  public int getShapeType() {
    return ShapefileConstants.MULTI_PATCH_SHAPE;
  }

  public Geometry read(final EndianInput in, final long recordLength)
    throws IOException {
    List<Polygon> polygons = new ArrayList<Polygon>();
    in.skipBytes(4 * MathUtil.BYTES_IN_DOUBLE);
    final int numParts = in.readLEInt();
    final int numPoints = in.readLEInt();
    int dimension;
    if (recordLength > 44 + 8 * numParts + 16 * numPoints) {
      dimension = 3;
    } else {
      dimension = 2;
    }
    final int[] partIndex = ShapefileGeometryUtil.readPartIndex(in, numParts,
      numPoints);
    final int[] partTypes = ShapefileGeometryUtil.readIntArray(in, numParts);

    final List<CoordinatesList> parts = ShapefileGeometryUtil.createCoordinatesLists(
      partIndex, dimension);
    ShapefileGeometryUtil.readPoints(in, partIndex, parts);
    if (dimension > 2) {
      ShapefileGeometryUtil.readCoordinates(in, partIndex, parts, 2);
    }
    List<CoordinatesList> rings = new ArrayList<CoordinatesList>();
    for (int i = 0; i < numParts; i++) {
      int partType = partTypes[i];
      CoordinatesList points = parts.get(i);
      switch (partType) {
        case 2:
          if (!rings.isEmpty()) {
            Polygon polygon = geometryFactory.createPolygon(rings);
            polygons.add(polygon);
            rings = new ArrayList<CoordinatesList>();
          }
          rings.add(points);
        break;
        case 3:
          if (rings.isEmpty()) {
            throw new IllegalStateException(
              "Interior ring without a exterior ring");
          } else {
            rings.add(points);

          }
        break;

        default:
          throw new IllegalStateException("Unsupported part type " + partType);
      }
    }
    if (!rings.isEmpty()) {
      Polygon polygon = geometryFactory.createPolygon(rings);
      polygons.add(polygon);
    }
    return geometryFactory.createMultiPolygon(polygons);
  }

  public void write(final EndianOutput out, final Geometry geometry)
    throws IOException {
    if (geometry instanceof MultiPolygon) {
      final MultiPolygon multiPolygon = (MultiPolygon)geometry;

      int numPoints = 0;
      List<Integer> partIndexes = new ArrayList<Integer>();
      List<Integer> partTypes = new ArrayList<Integer>();
      List<CoordinatesList> partPoints = new ArrayList<CoordinatesList>();
      boolean hasZ = false;
      for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
        Polygon polygon = (Polygon)multiPolygon.getGeometryN(i);
        final LineString exteriorRing = polygon.getExteriorRing();
        if (exteriorRing.getDimension() > 2) {
          hasZ = true;
        }
        numPoints += addPart(ShapefileConstants.OUTER_RING, numPoints,
          partIndexes, partTypes, partPoints, exteriorRing);
        for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
          LineString innerRing = polygon.getInteriorRingN(j);
          numPoints += addPart(ShapefileConstants.INNER_RING, numPoints,
            partIndexes, partTypes, partPoints, innerRing);
        }
      }

      final int numParts = partIndexes.size();
      int recordLength = 44 + 8 * numParts + 16 * numPoints;
      if (hasZ) {
        recordLength += 16 + 8 * numPoints;
      }

      out.writeInt(recordLength / 2);
      out.writeLEInt(getShapeType());
      ShapefileGeometryUtil.writeEnvelope(out,
        multiPolygon.getEnvelopeInternal());
      out.writeLEInt(numParts);
      out.writeLEInt(numPoints);
      for (Integer partIndex : partIndexes) {
        out.writeLEInt(partIndex);
      }
      for (Integer partType : partTypes) {
        out.writeLEInt(partType);
      }

      for (CoordinatesList points : partPoints) {
        ShapefileGeometryUtil.write2DCoordinates(out, points);
      }
      if (hasZ) {
        ShapefileGeometryUtil.writeCoordinateZValues(out, partPoints);
      }
    } else {
      throw new IllegalArgumentException("Expecting " + MultiPolygon.class
        + " geometry got " + geometry.getClass());
    }
  }

  private int addPart(int partType, int index, List<Integer> partIndexes,
    List<Integer> partTypes, List<CoordinatesList> partPoints, LineString ring) {
    partIndexes.add(index);
    partTypes.add(partType);
    CoordinatesList points = CoordinatesListUtil.get(ring);
    partPoints.add(points);

    return points.size();
  }
}
