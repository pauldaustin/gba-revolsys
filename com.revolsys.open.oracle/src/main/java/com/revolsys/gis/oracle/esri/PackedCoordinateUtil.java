package com.revolsys.gis.oracle.esri;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.DoubleCoordinatesList;
import com.revolsys.io.FileUtil;
import com.revolsys.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * NOTE -1 and 0 are before applying the scale factor
 * Point        (x,y [,z] [,m])
 * Line         (x,y (,x,y)+) [,z (,z)+] [,m (,m)+]
 * Multi Point  x,y (,-1,0, x,y)* [,z (,0,z)*] [,m (,0,m)*]
 * Multi Line   (x,y (,x,y)+) (,-1,0, (x,y (,x,y)+))* [(,z (,z)+) (,0, (,z (,z)+))*] [(,m (,m)+) (,0, (,m (,m)+))*]
 *
 */
public class PackedCoordinateUtil {

  public static Geometry getGeometry(final byte[] data, final GeometryFactory geometryFactory,
    final int entity, final int vertexCount, final Double xOffset, final Double yOffset,
    final Double xyScale, final Double zOffset, final Double zScale, final Double mOffset,
    final Double mScale) {
    final InputStream in = new ByteArrayInputStream(data);
    return getGeometry(in, geometryFactory, entity, vertexCount, xOffset, yOffset, xyScale, zOffset,
      zScale, mOffset, mScale);
  }

  public static Geometry getGeometry(final InputStream pointsIn,
    final GeometryFactory geometryFactory, final int geometryType, final int vertexCount,
    final Double xOffset, final Double yOffset, final Double xyScale, final Double zOffset,
    final Double zScale, final Double mOffset, final Double mScale) {
    switch (geometryType) {
      case ArcSdeConstants.ST_GEOMETRY_POINT:
        return getPoint(pointsIn, geometryFactory, vertexCount, xOffset, yOffset, xyScale, zOffset,
          zScale, mOffset, mScale);
      case ArcSdeConstants.ST_GEOMETRY_MULTI_POINT:
        return getMultiPoint(pointsIn, geometryFactory, vertexCount, xOffset, yOffset, xyScale,
          zOffset, zScale, mOffset, mScale);
      case ArcSdeConstants.ST_GEOMETRY_LINESTRING:
        return getLineString(pointsIn, geometryFactory, vertexCount, xOffset, yOffset, xyScale,
          zOffset, zScale, mOffset, mScale);
      case ArcSdeConstants.ST_GEOMETRY_MULTI_LINESTRING:
        return getMultiLineString(pointsIn, geometryFactory, vertexCount, xOffset, yOffset, xyScale,
          zOffset, zScale, mOffset, mScale);
      case ArcSdeConstants.ST_GEOMETRY_POLYGON:
        return getPolygon(pointsIn, geometryFactory, vertexCount, xOffset, yOffset, xyScale,
          zOffset, zScale, mOffset, mScale);
      case ArcSdeConstants.ST_GEOMETRY_MULTI_POLYGON:
        return getMultiPolygon(pointsIn, geometryFactory, vertexCount, xOffset, yOffset, xyScale,
          zOffset, zScale, mOffset, mScale);
      default:
        throw new IllegalArgumentException("Unknown ST_GEOMETRY entity type: " + geometryType);
    }
  }

  private static LineString getLineString(final InputStream pointsIn,
    final GeometryFactory geometryFactory, final int vertexCount, final Double xOffset,
    final Double yOffset, final Double xyScale, final Double zOffset, final Double zScale,
    final Double mOffset, final Double mScale) {
    final CoordinatesList points = getPointsSinglePart(vertexCount, xOffset, yOffset, xyScale,
      zOffset, zScale, mOffset, mScale, pointsIn);
    return geometryFactory.createLineString(points);
  }

  private static MultiLineString getMultiLineString(final InputStream pointsIn,
    final GeometryFactory geometryFactory, final int vertexCount, final Double xOffset,
    final Double yOffset, final Double xyScale, final Double zOffset, final Double zScale,
    final Double mOffset, final Double mScale) {
    final List<CoordinatesList> parts = getPointsMultiPart(vertexCount, xOffset, yOffset, xyScale,
      zOffset, zScale, mOffset, mScale, pointsIn);
    return geometryFactory.createMultiLineString(parts);
  }

  private static MultiPoint getMultiPoint(final InputStream pointsIn,
    final GeometryFactory geometryFactory, final int vertexCount, final Double xOffset,
    final Double yOffset, final Double xyScale, final Double zOffset, final Double zScale,
    final Double mOffset, final Double mScale) {
    final List<CoordinatesList> parts = getPointsMultiPart(vertexCount, xOffset, yOffset, xyScale,
      zOffset, zScale, mOffset, mScale, pointsIn);
    return geometryFactory.createMultiPoint(parts);
  }

  private static MultiPolygon getMultiPolygon(final InputStream pointsIn,
    final GeometryFactory geometryFactory, final int vertexCount, final Double xOffset,
    final Double yOffset, final Double xyScale, final Double zOffset, final Double zScale,
    final Double mOffset, final Double mScale) {
    final List<List<CoordinatesList>> pointsList = getMultiPolygonPoints(vertexCount, xOffset,
      yOffset, xyScale, zOffset, zScale, mOffset, mScale, pointsIn);
    try {
      return geometryFactory.createMultiPolygon(pointsList);
    } catch (final IllegalArgumentException e) {
      e.printStackTrace();
      LoggerFactory.getLogger(PackedCoordinateUtil.class).error("Unable to load polygon", e);
      return null;
    }
  }

  private static List<List<CoordinatesList>> getMultiPolygonPoints(final int vertexCount,
    final Double xOffset, final Double yOffset, final Double xyScale, final Double zOffset,
    final Double zScale, final Double mOffset, final Double mScale, final InputStream inputStream) {

    try {
      final List<List<CoordinatesList>> parts = new ArrayList<List<CoordinatesList>>();
      final PackedIntegerInputStream in = new PackedIntegerInputStream(inputStream);

      @SuppressWarnings("unused")
      final long packedByteLength = in.readLong5();
      final long dimensionFlag = in.readLong();
      @SuppressWarnings("unused")
      final int annotationDimension = in.read();
      @SuppressWarnings("unused")
      final int shapeFlags = in.read();
      final boolean hasZ = (dimensionFlag & 0x01) == 0x01;
      final boolean hasM = (dimensionFlag & 0x02) == 0x02;

      int axisCount;
      if (hasM) {
        axisCount = 4;
      } else if (hasZ) {
        axisCount = 3;
      } else {
        axisCount = 2;
      }

      List<CoordinatesList> pointsList = new ArrayList<>();
      final DoubleCoordinatesList points = new DoubleCoordinatesList(vertexCount, axisCount);

      long previousX = Math.round(xOffset * xyScale);
      long previousY = Math.round(yOffset * xyScale);

      int j = 0;
      for (int i = 0; i < vertexCount; i++) {
        final long deltaX = in.readLong();
        final long deltaY = in.readLong();
        previousX = previousX + deltaX;
        previousY = previousY + deltaY;
        final double x = previousX / xyScale;
        final double y = previousY / xyScale;
        if (previousX == -1 && previousY == 0 || x == -1 && y == 0) {
          if (!pointsList.isEmpty()) {
            parts.add(pointsList);
          }
          pointsList = new ArrayList<>();
        } else {
          points.setValue(j, 0, x);
          points.setValue(j, 1, y);
          if (j > 0 && i < vertexCount - 1) {
            if (points.equal(0, points, j)) {
              final CoordinatesList coordinates = points.subList(0, j + 1);
              if (coordinates.size() > 2) {
                pointsList.add(coordinates);
              }
              j = 0;
            } else {
              j++;
            }
          } else {
            j++;
          }
        }
      }
      final CoordinatesList coordinates = points.subList(0, j);
      if (coordinates.size() > 2) {
        pointsList.add(coordinates);
      }
      if (!pointsList.isEmpty()) {
        parts.add(pointsList);
      }
      if (hasZ) {
        getMultiPolygonPointsZorM(in, parts, 2, zOffset, zScale);
      }
      if (hasM) {
        getMultiPolygonPointsZorM(in, parts, 3, mOffset, mScale);
      }

      return parts;
    } catch (final IOException e) {
      throw new RuntimeException("Error reading coordinates", e);
    } finally {
      FileUtil.closeSilent(inputStream);
    }
  }

  private static void getMultiPolygonPointsZorM(final PackedIntegerInputStream in,
    final List<List<CoordinatesList>> parts, final int axisIndex, final double offset,
    final double scale) throws IOException {

    long previousValue = Math.round(offset * scale);
    boolean first = true;
    for (final List<CoordinatesList> part : parts) {
      if (first) {
        first = false;
      } else {
        in.readLong();
        previousValue = 0;
      }
      for (final CoordinatesList points : part) {
        final int vertexCount = points.size();
        for (int i = 0; i < vertexCount; i++) {
          final long deltaValue = in.readLong();
          previousValue = previousValue + deltaValue;
          final double value = previousValue / scale;
          points.setValue(i, axisIndex, value);
        }
      }
    }
  }

  public static int getNumPoints(final List<List<CoordinatesList>> parts) {
    int vertexCount = 0;
    if (!parts.isEmpty()) {
      for (final List<CoordinatesList> part : parts) {
        for (final CoordinatesList points : part) {
          vertexCount += points.size();
        }
      }
      vertexCount += parts.size() - 1;
    }
    return vertexCount;
  }

  public static byte[] getPackedBytes(final Double xOffset, final Double yOffset,
    final Double xyScale, final boolean hasZ, final Double zOffset, final Double zScale,
    final boolean hasM, final Double mScale, final Double mOffset,
    final List<List<CoordinatesList>> parts) {

    final int packedByteLength = 0;
    byte dimensionFlag = 0;
    final byte annotationDimension = 0;
    final byte shapeFlags = 0;

    if (hasZ) {
      dimensionFlag |= 1;
    }
    if (hasM) {
      dimensionFlag |= 2;
    }

    final PackedIntegerOutputStream out = new PackedIntegerOutputStream();
    out.writeLong5(packedByteLength);
    out.writeLong(dimensionFlag);
    out.writeLong(annotationDimension);
    out.writeLong(shapeFlags);

    // Write x,y for all parts
    long previousX = Math.round(xOffset * xyScale);
    long previousY = Math.round(yOffset * xyScale);
    boolean first = true;
    for (final List<CoordinatesList> part : parts) {
      if (first) {
        first = false;
      } else {
        out.writeLong(-1 - previousX);
        out.writeLong(-previousY);
        previousX = -1;
        previousY = 0;
      }
      for (final CoordinatesList points : part) {
        final int vertexCount = points.size();
        for (int i = 0; i < vertexCount; i++) {
          previousX = writeOrdinate(out, points, previousX, xyScale, i, 0);
          previousY = writeOrdinate(out, points, previousY, xyScale, i, 1);
        }
      }
    }

    // Write z for all parts
    if (hasZ) {
      writeMultiCoordinates(out, parts, 2, zOffset, zScale);
    }

    // Write m for all parts
    if (hasM) {
      writeMultiCoordinates(out, parts, 3, mOffset, mScale);
    }
    return out.toByteArray();
  }

  private static Point getPoint(final InputStream pointsIn, final GeometryFactory geometryFactory,
    final int vertexCount, final Double xOffset, final Double yOffset, final Double xyScale,
    final Double zOffset, final Double zScale, final Double mOffset, final Double mScale) {
    final CoordinatesList points = getPointsSinglePart(vertexCount, xOffset, yOffset, xyScale,
      zOffset, zScale, mOffset, mScale, pointsIn);
    return geometryFactory.createPoint(points);
  }

  private static List<CoordinatesList> getPointsMultiPart(final int vertexCount,
    final Double xOffset, final Double yOffset, final Double xyScale, final Double zOffset,
    final Double zScale, final Double mOffset, final Double mScale, final InputStream inputStream) {

    try {
      final List<CoordinatesList> pointsList = new ArrayList<CoordinatesList>();
      final PackedIntegerInputStream in = new PackedIntegerInputStream(inputStream);

      @SuppressWarnings("unused")
      final long packedByteLength = in.readLong5();
      final long dimensionFlag = in.readLong();
      @SuppressWarnings("unused")
      final int annotationDimension = in.read();
      @SuppressWarnings("unused")
      final int shapeFlags = in.read();
      final boolean hasZ = (dimensionFlag & 0x01) == 0x01;
      final boolean hasM = (dimensionFlag & 0x02) == 0x02;

      int axisCount;
      if (hasM) {
        axisCount = 4;
      } else if (hasZ) {
        axisCount = 3;
      } else {
        axisCount = 2;
      }
      final DoubleCoordinatesList points = new DoubleCoordinatesList(vertexCount, axisCount);

      long previousX = Math.round(xOffset * xyScale);
      long previousY = Math.round(yOffset * xyScale);

      int j = 0;
      for (int i = 0; i < vertexCount; i++) {
        final long deltaX = in.readLong();
        final long deltaY = in.readLong();
        previousX = previousX + deltaX;
        previousY = previousY + deltaY;
        final double x = previousX / xyScale;
        final double y = previousY / xyScale;

        // x & y are just temporary to deal with bad data in the GBA
        if (previousX == -1 && previousY == 0 || x == -1 && y == 0) {
          pointsList.add(points.subList(0, j));
          j = 0;
        } else {
          points.setValue(j, 0, x);
          points.setValue(j, 1, y);
          j++;
        }
      }
      pointsList.add(points.subList(0, j));

      if (hasZ) {
        getPointsMutliPartZorM(in, pointsList, 2, zOffset, zScale);
      }
      if (hasM) {
        getPointsMutliPartZorM(in, pointsList, 3, mOffset, mScale);
      }

      return pointsList;
    } catch (final IOException e) {
      throw new RuntimeException("Error reading coordinates", e);
    } finally {
      FileUtil.closeSilent(inputStream);
    }
  }

  private static void getPointsMutliPartZorM(final PackedIntegerInputStream in,
    final List<CoordinatesList> pointsList, final int axisIndex, final double offset,
    final double scale) throws IOException {
    long previousValue = Math.round(offset * scale);

    boolean first = true;
    for (final CoordinatesList part : pointsList) {
      if (first) {
        first = false;
      } else {
        in.readLong();
        previousValue = 0;
      }
      final int vertexCount = part.size();
      for (int i = 0; i < vertexCount; i++) {
        final long deltaValue = in.readLong();
        previousValue = previousValue + deltaValue;
        final double value = previousValue / scale;
        part.setValue(i, axisIndex, value);
      }
    }
  }

  @SuppressWarnings("unused")
  private static CoordinatesList getPointsSinglePart(final int vertexCount, final Double xOffset,
    final Double yOffset, final Double xyScale, final Double zOffset, final Double zScale,
    final Double mOffset, final Double mScale, final InputStream inputStream) {

    try {
      final PackedIntegerInputStream in = new PackedIntegerInputStream(inputStream);

      final long packedByteLength = in.readLong5();
      final long dimensionFlag = in.readLong();
      final int annotationDimension = in.read();
      final int shapeFlags = in.read();
      final boolean hasZ = (dimensionFlag & 0x01) == 0x01;
      final boolean hasM = (dimensionFlag & 0x02) == 0x02;

      int axisCount;
      if (hasM) {
        axisCount = 4;
      } else if (hasZ) {
        axisCount = 3;
      } else {
        axisCount = 2;
      }
      final DoubleCoordinatesList points = new DoubleCoordinatesList(vertexCount, axisCount);

      long previousX = Math.round(xOffset * xyScale);
      long previousY = Math.round(yOffset * xyScale);

      for (int i = 0; i < vertexCount; i++) {
        final long deltaX = in.readLong();
        final long deltaY = in.readLong();
        previousX = previousX + deltaX;
        previousY = previousY + deltaY;
        final double x = previousX / xyScale;
        final double y = previousY / xyScale;
        points.setValue(i, 0, x);
        points.setValue(i, 1, y);
      }

      if (hasZ) {
        getPointsSinglePartZorM(in, points, 2, zOffset, zScale);
      }
      if (hasM) {
        getPointsSinglePartZorM(in, points, 3, mOffset, mScale);
      }

      return points;
    } catch (final IOException e) {
      throw new RuntimeException("Error reading coordinates", e);
    } finally {
      FileUtil.closeSilent(inputStream);
    }
  }

  private static void getPointsSinglePartZorM(final PackedIntegerInputStream in,
    final CoordinatesList points, final int axisIndex, final double offset, final double scale)
      throws IOException {
    long previousValue = Math.round(offset * scale);

    final int vertexCount = points.size();
    for (int i = 0; i < vertexCount; i++) {
      final long deltaValue = in.readLong();
      previousValue = previousValue + deltaValue;
      final double value = previousValue / scale;
      points.setValue(i, axisIndex, value);
    }
  }

  private static Polygon getPolygon(final InputStream pointsIn,
    final GeometryFactory geometryFactory, final int vertexCount, final Double xOffset,
    final Double yOffset, final Double xyScale, final Double zOffset, final Double zScale,
    final Double mOffset, final Double mScale) {
    final List<CoordinatesList> pointsList = getPolygonPoints(vertexCount, xOffset, yOffset,
      xyScale, zOffset, zScale, mOffset, mScale, pointsIn);
    try {
      return geometryFactory.createPolygon(pointsList);
    } catch (final IllegalArgumentException e) {
      e.printStackTrace();
      LoggerFactory.getLogger(PackedCoordinateUtil.class).error("Unable to load polygon", e);
      return null;
    }
  }

  private static List<CoordinatesList> getPolygonPoints(final int vertexCount, final Double xOffset,
    final Double yOffset, final Double xyScale, final Double zOffset, final Double zScale,
    final Double mOffset, final Double mScale, final InputStream inputStream) {

    try {
      final List<CoordinatesList> pointsList = new ArrayList<CoordinatesList>();
      final PackedIntegerInputStream in = new PackedIntegerInputStream(inputStream);

      @SuppressWarnings("unused")
      final long packedByteLength = in.readLong5();
      final long dimensionFlag = in.readLong();
      @SuppressWarnings("unused")
      final int annotationDimension = in.read();
      @SuppressWarnings("unused")
      final int shapeFlags = in.read();
      final boolean hasZ = (dimensionFlag & 0x01) == 0x01;
      final boolean hasM = (dimensionFlag & 0x02) == 0x02;

      int axisCount;
      if (hasM) {
        axisCount = 4;
      } else if (hasZ) {
        axisCount = 3;
      } else {
        axisCount = 2;
      }
      final DoubleCoordinatesList points = new DoubleCoordinatesList(vertexCount, axisCount);

      long previousX = Math.round(xOffset * xyScale);
      long previousY = Math.round(yOffset * xyScale);

      int j = 0;
      for (int i = 0; i < vertexCount; i++) {
        final long deltaX = in.readLong();
        final long deltaY = in.readLong();
        previousX = previousX + deltaX;
        previousY = previousY + deltaY;
        final double x = previousX / xyScale;
        final double y = previousY / xyScale;
        points.setValue(j, 0, x);
        points.setValue(j, 1, y);
        if (j > 0 && i < vertexCount - 1) {
          if (points.equal(0, points, j)) {
            final CoordinatesList coordinates = points.subList(0, j + 1);
            if (coordinates.size() > 2) {
              pointsList.add(coordinates);
            }
            j = 0;
          } else {
            j++;
          }
        } else {
          j++;
        }
      }
      final CoordinatesList coordinates = points.subList(0, j);
      if (coordinates.size() > 2) {
        pointsList.add(coordinates);
      }
      if (hasZ) {
        getPolygonPointsZorM(in, pointsList, 2, zOffset, zScale);
      }
      if (hasM) {
        getPolygonPointsZorM(in, pointsList, 3, mOffset, mScale);
      }
      return pointsList;
    } catch (final IOException e) {
      throw new RuntimeException("Error reading coordinates", e);
    } finally {
      FileUtil.closeSilent(inputStream);
    }
  }

  private static void getPolygonPointsZorM(final PackedIntegerInputStream in,
    final List<CoordinatesList> pointsList, final int axisIndex, final double offset,
    final double scale) throws IOException {
    long previousValue = Math.round(offset * scale);

    for (final CoordinatesList points : pointsList) {
      final int vertexCount = points.size();
      for (int i = 0; i < vertexCount; i++) {
        final long deltaValue = in.readLong();
        previousValue = previousValue + deltaValue;
        final double value = previousValue / scale;
        points.setValue(i, axisIndex, value);
      }
    }
  }

  private static long writeCoordinates(final PackedIntegerOutputStream out,
    final CoordinatesList points, long previousValue, final double scale, final int ordinateIndex) {
    for (int i = 0; i < points.size(); i++) {
      previousValue = writeOrdinate(out, points, previousValue, scale, i, ordinateIndex);
    }
    return previousValue;
  }

  private static void writeMultiCoordinates(final PackedIntegerOutputStream out,
    final List<List<CoordinatesList>> partsList, final int axisIndex, final double offset,
    final double scale) {
    long previous = Math.round(offset * scale);
    boolean firstPart = true;
    for (final List<CoordinatesList> part : partsList) {
      if (firstPart) {
        firstPart = false;
      } else {
        out.writeLong(-previous);
        previous = 0;
      }
      for (final CoordinatesList points : part) {
        if (points.getNumAxis() > axisIndex) {
          previous = writeCoordinates(out, points, previous, scale, axisIndex);
        } else {
          previous = writeZeroCoordinates(out, points.size(), scale, previous);
        }
      }
    }
  }

  /**
   * Write the value of an ordinate from the coordinates which has the specified
   * coordinateIndex and ordinateIndex. The value written is the difference
   * between the current value and the previous value which are both multiplied
   * by the scale and rounded to longs before conversion.
   *
   * @param out The stream to write the bytes to.
   * @param coordinates The coordinates.
   * @param previousValue The value of the previous coordinate, returned from
   *          this method.
   * @param scale The scale which defines the precision of the values.
   * @param coordinateIndex The coordinate index.
   * @param ordinateIndex The ordinate index.
   * @return The current ordinate value * scale rounded to a long value.
   */
  private static long writeOrdinate(final PackedIntegerOutputStream out,
    final CoordinatesList coordinates, final long previousValue, final double scale,
    final int coordinateIndex, final int ordinateIndex) {
    final double value = coordinates.getOrdinate(coordinateIndex, ordinateIndex);
    return writeOrdinate(out, previousValue, scale, value);
  }

  private static long writeOrdinate(final PackedIntegerOutputStream out, final long previousValue,
    final double scale, final double value) {
    long longValue;
    if (Double.isNaN(value)) {
      longValue = 0;
    } else {
      longValue = Math.round(value * scale);
    }
    out.writeLong(longValue - previousValue);
    return longValue;
  }

  private static long writeZeroCoordinates(final PackedIntegerOutputStream out,
    final int numCoordinates, final double scale, long previousValue) {
    for (int i = 0; i < numCoordinates; i++) {
      previousValue = writeOrdinate(out, previousValue, scale, 0);
    }
    return previousValue;
  }
}
