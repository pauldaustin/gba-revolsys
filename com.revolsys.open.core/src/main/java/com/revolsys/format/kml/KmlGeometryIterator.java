package com.revolsys.format.kml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.springframework.core.io.Resource;

import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.format.xml.StaxUtils;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.DoubleCoordinatesList;
import com.revolsys.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class KmlGeometryIterator extends AbstractIterator<Geometry>implements Kml22Constants {
  private GeometryFactory geometryFactory = GeometryFactory.floating3(COORDINATE_SYSTEM_ID);

  private XMLStreamReader in;

  public KmlGeometryIterator(final InputStream in) {
    this.in = StaxUtils.createXmlReader(in);
  }

  public KmlGeometryIterator(final Resource resource) {
    this.in = StaxUtils.createXmlReader(resource);
  }

  @Override
  protected void doClose() {
    StaxUtils.closeSilent(this.in);
    this.geometryFactory = null;
    this.in = null;
  }

  @Override
  protected void doInit() {
    StaxUtils.skipToStartElement(this.in);
  }

  @Override
  protected Geometry getNext() {
    try {
      final Geometry geometry = parseGeometry();
      if (geometry == null) {
        throw new NoSuchElementException();
      } else {
        return geometry;
      }
    } catch (final XMLStreamException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private CoordinatesList parseCoordinates() throws XMLStreamException {
    StaxUtils.requireLocalName(this.in, COORDINATES);
    final String coordinatesListString = StaxUtils.getElementText(this.in);
    final String[] coordinatesListArray = coordinatesListString.trim().split("\\s+");
    final CoordinatesList coordinatesList = new DoubleCoordinatesList(coordinatesListArray.length,
      3);
    for (int i = 0; i < coordinatesListArray.length; i++) {
      final String coordinatesString = coordinatesListArray[i];
      final String[] coordinatesArray = coordinatesString.split(",");
      for (int ordinateIndex = 0; ordinateIndex < coordinatesArray.length
        && ordinateIndex < 3; ordinateIndex++) {
        final String coordinate = coordinatesArray[ordinateIndex];
        coordinatesList.setValue(i, ordinateIndex, Double.valueOf(coordinate));
      }
    }
    StaxUtils.skipToEndElementByLocalName(this.in, COORDINATES);
    return coordinatesList;
  }

  private Geometry parseGeometry() throws XMLStreamException {
    if (this.in.getEventType() != XMLStreamConstants.START_ELEMENT) {
      StaxUtils.skipToStartElement(this.in);
    }
    while (this.in.getEventType() == XMLStreamConstants.START_ELEMENT) {
      if (StaxUtils.matchElementLocalName(this.in, MULTI_GEOMETRY)) {
        return parseMultiGeometry();
      } else if (StaxUtils.matchElementLocalName(this.in, POINT)) {
        return parsePoint();
      } else if (StaxUtils.matchElementLocalName(this.in, LINE_STRING)) {
        return parseLineString();
      } else if (StaxUtils.matchElementLocalName(this.in, POLYGON)) {
        return parsePolygon();
      } else {
        while (this.in.next() != XMLStreamConstants.START_ELEMENT
          && this.in.getEventType() != XMLStreamConstants.END_DOCUMENT) {

        }
      }
    }
    return null;
  }

  private LinearRing parseInnerBoundary() throws XMLStreamException {
    LinearRing ring = null;
    StaxUtils.requireLocalName(this.in, INNER_BOUNDARY_IS);
    while (this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (ring == null && StaxUtils.matchElementLocalName(this.in, LINEAR_RING)) {
        ring = parseLinearRing();
      } else {
        StaxUtils.skipSubTree(this.in);
      }
    }
    StaxUtils.skipToEndElementByLocalName(this.in, INNER_BOUNDARY_IS);
    return ring;
  }

  private LinearRing parseLinearRing() throws XMLStreamException {
    StaxUtils.requireLocalName(this.in, LINEAR_RING);
    CoordinatesList cooordinatesList = null;
    while (!StaxUtils.isEndElementLocalName(this.in, LINEAR_RING)
      && this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (StaxUtils.matchElementLocalName(this.in, COORDINATES)) {
        cooordinatesList = parseCoordinates();
      } else {
        StaxUtils.skipSubTree(this.in);
      }
    }
    StaxUtils.skipToEndElementByLocalName(this.in, LINEAR_RING);
    final LinearRing ring = this.geometryFactory.createLinearRing(cooordinatesList);
    return ring;
  }

  private LineString parseLineString() throws XMLStreamException {
    StaxUtils.requireLocalName(this.in, LINE_STRING);
    CoordinatesList cooordinatesList = null;
    while (!StaxUtils.isEndElementLocalName(this.in, LINE_STRING)
      && this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (StaxUtils.matchElementLocalName(this.in, COORDINATES)) {
        cooordinatesList = parseCoordinates();
      } else {
        StaxUtils.skipSubTree(this.in);
      }
    }
    final LineString lineString = this.geometryFactory.createLineString(cooordinatesList);
    StaxUtils.skipToEndElementByLocalName(this.in, LINE_STRING);
    return lineString;
  }

  private Geometry parseMultiGeometry() throws XMLStreamException {
    StaxUtils.requireLocalName(this.in, MULTI_GEOMETRY);
    final List<Geometry> geometries = new ArrayList<Geometry>();
    while (!StaxUtils.isEndElementLocalName(this.in, MULTI_GEOMETRY)
      && this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      final Geometry geometry = parseGeometry();
      if (geometry != null) {
        geometries.add(geometry);
      }
    }
    final Geometry geometryCollection = this.geometryFactory.createGeometry(geometries);
    StaxUtils.skipToEndElementByLocalName(this.in, MULTI_GEOMETRY);
    return geometryCollection;
  }

  private LinearRing parseOuterBoundary() throws XMLStreamException {
    StaxUtils.requireLocalName(this.in, OUTER_BOUNDARY_IS);
    LinearRing ring = null;
    while (!StaxUtils.isEndElementLocalName(this.in, OUTER_BOUNDARY_IS)
      && this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (ring == null && StaxUtils.matchElementLocalName(this.in, LINEAR_RING)) {
        ring = parseLinearRing();
      } else {
        StaxUtils.skipSubTree(this.in);
      }
    }
    StaxUtils.skipToEndElementByLocalName(this.in, OUTER_BOUNDARY_IS);
    return ring;
  }

  private Point parsePoint() throws XMLStreamException {
    StaxUtils.requireLocalName(this.in, POINT);
    CoordinatesList cooordinatesList = null;
    while (!StaxUtils.isEndElementLocalName(this.in, POINT)
      && this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (cooordinatesList == null && StaxUtils.matchElementLocalName(this.in, COORDINATES)) {
        cooordinatesList = parseCoordinates();
      } else {
        StaxUtils.skipSubTree(this.in);
      }
    }
    final Point point = this.geometryFactory.createPoint(cooordinatesList);
    StaxUtils.skipToEndElementByLocalName(this.in, POINT);
    return point;
  }

  private Polygon parsePolygon() throws XMLStreamException {
    StaxUtils.requireLocalName(this.in, POLYGON);
    final List<LinearRing> rings = new ArrayList<LinearRing>();
    while (!StaxUtils.isEndElementLocalName(this.in, POLYGON)
      && this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (rings.isEmpty()) {
        if (StaxUtils.matchElementLocalName(this.in, OUTER_BOUNDARY_IS)) {
          rings.add(parseOuterBoundary());
        } else {
          StaxUtils.skipSubTree(this.in);
        }
      } else if (StaxUtils.matchElementLocalName(this.in, INNER_BOUNDARY_IS)) {
        final LinearRing innerRing = parseInnerBoundary();
        if (innerRing != null) {
          rings.add(innerRing);
        }
      } else {
        StaxUtils.skipSubTree(this.in);
      }
    }
    final Polygon polygon = this.geometryFactory.createPolygon(rings);
    StaxUtils.skipToEndElementByLocalName(this.in, POLYGON);
    return polygon;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return StaxUtils.toString(this.in);
  }

}
