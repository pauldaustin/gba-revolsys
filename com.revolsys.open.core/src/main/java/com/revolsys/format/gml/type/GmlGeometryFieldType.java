package com.revolsys.format.gml.type;

import javax.xml.namespace.QName;

import com.revolsys.datatype.DataType;
import com.revolsys.format.gml.GmlConstants;
import com.revolsys.format.gml.GmlRecordWriter;
import com.revolsys.format.xml.XmlWriter;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GmlGeometryFieldType extends AbstractGmlFieldType {
  public GmlGeometryFieldType(final DataType dataType) {
    super(dataType, "xs:" + dataType.getName());
  }

  private void coordinates(final XmlWriter out, final CoordinatesList points) {
    out.startTag(GmlConstants.COORDINATES);
    final byte numAxis = points.getNumAxis();
    boolean first = true;
    for (int i = 0; i < points.size(); i++) {
      if (first) {
        first = false;
      } else {
        out.text(" ");
      }
      final double x = points.getX(i);
      out.text(x);
      final double y = points.getY(i);
      out.text(",");
      out.text(y);
      if (numAxis > 2) {
        final double z = points.getZ(i);
        if (Double.isNaN(z)) {
          out.text(0);
        } else {
          out.text(z);
        }
      }
    }
    out.endTag(GmlConstants.COORDINATES);
  }

  private void geometry(final XmlWriter out, final Object value, final boolean writeSrsName) {
    if (value instanceof Point) {
      final Point point = (Point)value;
      point(out, point, writeSrsName);
    } else if (value instanceof LineString) {
      final LineString line = (LineString)value;
      lineString(out, line, writeSrsName);
    } else if (value instanceof Polygon) {
      final Polygon polygon = (Polygon)value;
      polygon(out, polygon, writeSrsName);
    } else if (value instanceof MultiPoint) {
      final MultiPoint multiPoint = (MultiPoint)value;
      multiPoint(out, multiPoint, writeSrsName);
    } else if (value instanceof MultiLineString) {
      final MultiLineString multiLine = (MultiLineString)value;
      multiLineString(out, multiLine, writeSrsName);
    } else if (value instanceof MultiPolygon) {
      final MultiPolygon multiPolygon = (MultiPolygon)value;
      multiPolygon(out, multiPolygon, writeSrsName);
    } else if (value instanceof GeometryCollection) {
      final GeometryCollection geometryCollection = (GeometryCollection)value;
      geometryCollection(out, geometryCollection, writeSrsName);
    }
  }

  private void geometryCollection(final XmlWriter out, final GeometryCollection geometryCollection,
    final boolean writeSrsName) {
    geometryCollection(out, MULTI_GEOMETRY, GEOMETRY_MEMBER, geometryCollection, writeSrsName);
  }

  private void geometryCollection(final XmlWriter out, final QName tag, final QName memberTag,
    final GeometryCollection geometryCollection, final boolean writeSrsName) {
    out.startTag(tag);
    srsName(out, geometryCollection, writeSrsName);
    for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
      final Geometry geometry = geometryCollection.getGeometryN(i);
      out.startTag(memberTag);
      geometry(out, geometry, false);
      out.endTag(memberTag);
    }
    out.endTag(tag);
  }

  private void linearRing(final XmlWriter out, final LineString line, final boolean writeSrsName) {
    out.startTag(LINEAR_RING);
    final CoordinatesList points = CoordinatesListUtil.get(line);
    coordinates(out, points);
    out.endTag(LINEAR_RING);
  }

  private void lineString(final XmlWriter out, final LineString line, final boolean writeSrsName) {
    out.startTag(LINE_STRING);
    srsName(out, line, writeSrsName);
    final CoordinatesList points = CoordinatesListUtil.get(line);
    coordinates(out, points);
    out.endTag(LINE_STRING);
  }

  private void multiLineString(final XmlWriter out, final MultiLineString multiLine,
    final boolean writeSrsName) {
    geometryCollection(out, MULTI_LINE_STRING, LINE_STRING_MEMBER, multiLine, writeSrsName);
  }

  private void multiPoint(final XmlWriter out, final MultiPoint multiPoint,
    final boolean writeSrsName) {
    geometryCollection(out, MULTI_POINT, POINT_MEMBER, multiPoint, writeSrsName);
  }

  private void multiPolygon(final XmlWriter out, final MultiPolygon multiPolygon,
    final boolean writeSrsName) {
    geometryCollection(out, MULTI_POLYGON, POLYGON_MEMBER, multiPolygon, writeSrsName);
  }

  private void point(final XmlWriter out, final Point point, final boolean writeSrsName) {
    out.startTag(POINT);
    srsName(out, point, writeSrsName);
    final CoordinatesList points = CoordinatesListUtil.get(point);
    coordinates(out, points);
    out.endTag(POINT);
  }

  private void polygon(final XmlWriter out, final Polygon polygon, final boolean writeSrsName) {
    out.startTag(POLYGON);
    srsName(out, polygon, writeSrsName);

    final LineString exteriorRing = polygon.getExteriorRing();
    out.startTag(OUTER_BOUNDARY_IS);
    linearRing(out, exteriorRing, false);
    out.endTag(OUTER_BOUNDARY_IS);

    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
      final LineString interiorRing = polygon.getInteriorRingN(i);
      out.startTag(INNER_BOUNDARY_IS);
      linearRing(out, interiorRing, false);
      out.endTag(INNER_BOUNDARY_IS);
    }

    out.endTag(POLYGON);
  }

  private void srsName(final XmlWriter out, final Geometry geometry, final boolean writeSrsName) {
    if (writeSrsName) {
      final GeometryFactory factory = GeometryFactory.getFactory(geometry);
      GmlRecordWriter.srsName(out, factory);
    }
  }

  @Override
  protected void writeValueText(final XmlWriter out, final Object value) {
    geometry(out, value, true);
  }
}
