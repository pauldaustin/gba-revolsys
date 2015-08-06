package com.revolsys.format.geojson;

import java.io.BufferedWriter;
import java.io.Writer;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.io.RecordWriter;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.format.json.JsonWriter;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.io.AbstractWriter;
import com.revolsys.io.IoConstants;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.util.MathUtil;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GeoJsonRecordWriter extends AbstractWriter<Record>
  implements RecordWriter, GeoJsonConstants {

  boolean initialized = false;

  private int srid = -1;

  /** The writer */
  private JsonWriter out;

  private boolean singleObject;

  private final boolean cogo;

  public GeoJsonRecordWriter(final Writer out) {
    this(out, false);
  }

  public GeoJsonRecordWriter(final Writer out, final boolean cogo) {
    this.out = new JsonWriter(new BufferedWriter(out));
    this.out.setIndent(true);
    this.cogo = cogo;
  }

  /**
   * Closes the underlying reader.
   */
  @Override
  public void close() {
    if (this.out != null) {
      try {
        writeFooter();
      } finally {
        this.out.close();
        this.out = null;
      }
    }
  }

  private void coordinate(final CoordinatesList coordinates, final int i) {
    double x = coordinates.getX(i);
    double y = coordinates.getY(i);

    if (this.cogo && i > 0) {
      final double currentX = x;
      final double previousX = coordinates.getX(i - 1);
      final double previousY = coordinates.getY(i - 1);
      x = MathUtil.distance(previousX, previousY, currentX, y);
      y = MathUtil.angleNorthDegrees(previousX, previousY, currentX, y);
    }

    this.out.print('[');
    this.out.value(x);

    this.out.print(',');
    this.out.value(y);

    final double z = coordinates.getZ(i);
    if (!Double.isNaN(z)) {
      this.out.print(',');
      this.out.value(z);
    }
    this.out.print(']');
  }

  private void coordinates(final CoordinatesList coordinates) {
    this.out.startList(false);
    this.out.indent();
    coordinate(coordinates, 0);
    for (int i = 1; i < coordinates.size(); i++) {
      this.out.endAttribute();
      this.out.indent();
      coordinate(coordinates, i);
    }
    this.out.endList();
  }

  private void coordinates(final LineString line) {
    final CoordinatesList coordinates = CoordinatesListUtil.get(line);
    coordinates(coordinates);
  }

  public void coordinates(final Point point) {
    final CoordinatesList coordinates = CoordinatesListUtil.get(point);
    coordinate(coordinates, 0);
  }

  public void coordinates(final Polygon polygon) {
    this.out.startList(false);
    this.out.indent();

    final LineString exteriorRing = polygon.getExteriorRing();
    coordinates(exteriorRing);
    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
      final LineString interiorRing = polygon.getInteriorRingN(i);
      this.out.endAttribute();
      this.out.indent();
      coordinates(interiorRing);
    }

    this.out.endList();
  }

  @Override
  public void flush() {
    this.out.flush();
  }

  private void geometry(final Geometry geometry) {
    this.out.startObject();
    if (geometry instanceof Point) {
      final Point point = (Point)geometry;
      point(point);
    } else if (geometry instanceof LineString) {
      final LineString line = (LineString)geometry;
      line(line);
    } else if (geometry instanceof Polygon) {
      final Polygon polygon = (Polygon)geometry;
      polygon(polygon);
    } else if (geometry instanceof MultiPoint) {
      final MultiPoint multiPoint = (MultiPoint)geometry;
      multiPoint(multiPoint);
    } else if (geometry instanceof MultiLineString) {
      final MultiLineString multiLine = (MultiLineString)geometry;
      multiLineString(multiLine);
    } else if (geometry instanceof MultiPolygon) {
      final MultiPolygon multiPolygon = (MultiPolygon)geometry;
      multiPolygon(multiPolygon);
    } else if (geometry instanceof GeometryCollection) {
      final GeometryCollection geometryCollection = (GeometryCollection)geometry;
      geometryCollection(geometryCollection);
    }
    this.out.endObject();
  }

  private void geometryCollection(final GeometryCollection geometryCollection) {
    type(GEOMETRY_COLLECTION);

    this.out.endAttribute();
    this.out.label(GEOMETRIES);
    this.out.startList();
    final int numGeometries = geometryCollection.getNumGeometries();
    if (numGeometries > 0) {
      geometry(geometryCollection.getGeometryN(0));
      for (int i = 1; i < numGeometries; i++) {
        final Geometry geometry = geometryCollection.getGeometryN(i);
        this.out.endAttribute();
        geometry(geometry);
      }
    }
    this.out.endList();
  }

  public boolean isCogo() {
    return this.cogo;
  }

  private void line(final LineString line) {
    if (this.cogo) {
      type(COGO_LINE_STRING);
    } else {
      type(LINE_STRING);
    }
    this.out.endAttribute();
    this.out.label(COORDINATES);
    coordinates(line);
  }

  private void multiLineString(final MultiLineString multiLineString) {
    if (this.cogo) {
      type(COGO_LINE_STRING);
    } else {
      type(MULTI_LINE_STRING);
    }

    this.out.endAttribute();
    this.out.label(COORDINATES);
    this.out.startList();
    this.out.indent();
    final int numGeometries = multiLineString.getNumGeometries();
    if (numGeometries > 0) {
      coordinates((LineString)multiLineString.getGeometryN(0));
      for (int i = 1; i < numGeometries; i++) {
        final LineString lineString = (LineString)multiLineString.getGeometryN(i);
        this.out.endAttribute();
        this.out.indent();
        coordinates(lineString);
      }
    }
    this.out.endList();
  }

  private void multiPoint(final MultiPoint multiPoint) {
    type(MULTI_POINT);

    this.out.endAttribute();
    this.out.label(COORDINATES);
    this.out.startList();
    this.out.indent();
    final int numGeometries = multiPoint.getNumGeometries();
    if (numGeometries > 0) {
      coordinates((Point)multiPoint.getGeometryN(0));
      for (int i = 1; i < numGeometries; i++) {
        final Point point = (Point)multiPoint.getGeometryN(i);
        this.out.endAttribute();
        this.out.indent();
        coordinates(point);
      }
    }
    this.out.endList();
  }

  private void multiPolygon(final MultiPolygon multiPolygon) {
    if (this.cogo) {
      type(COGO_MULTI_POLYGON);
    } else {
      type(MULTI_POLYGON);
    }

    this.out.endAttribute();
    this.out.label(COORDINATES);
    this.out.startList();
    this.out.indent();
    final int numGeometries = multiPolygon.getNumGeometries();
    if (numGeometries > 0) {
      coordinates((Polygon)multiPolygon.getGeometryN(0));
      for (int i = 1; i < numGeometries; i++) {
        final Polygon polygon = (Polygon)multiPolygon.getGeometryN(i);
        this.out.endAttribute();
        this.out.indent();
        coordinates(polygon);
      }
    }
    this.out.endList();
  }

  private void point(final Point point) {
    type(POINT);
    this.out.endAttribute();
    this.out.label(COORDINATES);
    coordinates(point);
  }

  private void polygon(final Polygon polygon) {
    if (this.cogo) {
      type(COGO_POLYGON);
    } else {
      type(POLYGON);
    }

    this.out.endAttribute();
    this.out.label(COORDINATES);
    coordinates(polygon);
  }

  private void srid(final int srid) {
    final String urn = URN_OGC_DEF_CRS_EPSG + srid;
    this.out.label(CRS);
    this.out.startObject();
    type(NAME);
    this.out.endAttribute();
    this.out.label(PROPERTIES);
    this.out.startObject();
    this.out.label(NAME);
    this.out.value(urn);
    this.out.endObject();
    this.out.endObject();
  }

  private void type(final String type) {
    this.out.label(TYPE);
    this.out.value(type);
  }

  @Override
  public void write(final Record object) {
    if (this.initialized) {
      this.out.endAttribute();
    } else {
      writeHeader();
      this.initialized = true;
    }
    this.out.startObject();
    type(FEATURE);
    final Geometry mainGeometry = object.getGeometry();
    writeSrid(mainGeometry);
    final RecordDefinition metaData = object.getRecordDefinition();
    final int geometryIndex = metaData.getGeometryFieldIndex();
    boolean geometryWritten = false;
    this.out.endAttribute();
    this.out.label(GEOMETRY);
    if (mainGeometry != null) {
      geometryWritten = true;
      geometry(mainGeometry);
    }
    if (!geometryWritten) {
      this.out.value(null);
    }
    this.out.endAttribute();
    this.out.label(PROPERTIES);
    this.out.startObject();
    final int numAttributes = metaData.getFieldCount();
    if (numAttributes > 1 || numAttributes == 1 && geometryIndex == -1) {
      int lastIndex = numAttributes - 1;
      if (lastIndex == geometryIndex) {
        lastIndex--;
      }
      for (int i = 0; i < numAttributes; i++) {
        if (i != geometryIndex) {
          final String name = metaData.getFieldName(i);
          final Object value = object.getValue(i);
          this.out.label(name);
          if (value instanceof Geometry) {
            final Geometry geometry = (Geometry)value;
            geometry(geometry);
          } else {
            this.out.value(value);
          }
          if (i < lastIndex) {
            this.out.endAttribute();
          }
        }
      }
    }
    this.out.endObject();
    this.out.endObject();
  }

  private void writeFooter() {
    if (!this.singleObject) {
      this.out.endList();
      this.out.endObject();
    }
    final String callback = getProperty(IoConstants.JSONP_PROPERTY);
    if (callback != null) {
      this.out.print(");");
    }
  }

  private void writeHeader() {
    final String callback = getProperty(IoConstants.JSONP_PROPERTY);
    if (callback != null) {
      this.out.print(callback);
      this.out.print('(');
    }
    this.singleObject = Boolean.TRUE.equals(getProperty(IoConstants.SINGLE_OBJECT_PROPERTY));
    if (!this.singleObject) {
      this.out.startObject();
      type(FEATURE_COLLECTION);
      this.srid = writeSrid();
      this.out.endAttribute();
      this.out.label(FEATURES);
      this.out.startList();
    }
  }

  private int writeSrid() {
    final GeometryFactory geometryFactory = getProperty(IoConstants.GEOMETRY_FACTORY);
    return writeSrid(geometryFactory);
  }

  private void writeSrid(final Geometry geometry) {
    if (geometry != null) {
      final GeometryFactory geometryFactory = GeometryFactory.getFactory(geometry);
      writeSrid(geometryFactory);
    }
  }

  protected int writeSrid(final GeometryFactory geometryFactory) {
    if (geometryFactory != null) {
      final int srid = geometryFactory.getSRID();
      if (srid != 0 && srid != this.srid) {
        this.out.endAttribute();
        srid(srid);
        return srid;
      }
    }
    return -1;
  }
}
