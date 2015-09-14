package com.revolsys.format.gpx;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;
import org.springframework.core.io.Resource;

import com.revolsys.format.xml.StaxUtils;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.CoordinatesUtil;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.DoubleCoordinatesList;
import com.revolsys.gis.model.coordinates.list.DoubleListCoordinatesList;
import com.revolsys.io.FileUtil;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.record.Record;
import com.revolsys.record.RecordFactory;
import com.revolsys.record.io.RecordIterator;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.util.DateUtil;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

public class GpxIterator implements RecordIterator {
  private static final Logger log = Logger.getLogger(GpxIterator.class);

  private String baseName;

  private Record currentRecord;

  private File file;

  private final GeometryFactory geometryFactory = GeometryFactory.floating3(4326);

  private boolean hasNext = true;

  private final XMLStreamReader in;

  private int index = 0;

  private boolean loadNextObject = true;

  private final Queue<Record> objects = new LinkedList<Record>();

  private RecordFactory recordFactory;

  private String schemaName = GpxConstants.GPX_NS_URI;

  private String typePath;

  public GpxIterator(final File file) throws IOException, XMLStreamException {
    this(new FileReader(file));
  }

  public GpxIterator(final Reader in) throws IOException, XMLStreamException {
    this(StaxUtils.createXmlReader(in));
  }

  public GpxIterator(final Reader in, final RecordFactory recordFactory, final String path) {
    this(StaxUtils.createXmlReader(in));
    this.recordFactory = recordFactory;
    this.typePath = path;
  }

  public GpxIterator(final Resource resource, final RecordFactory recordFactory, final String path)
    throws IOException {
    this(StaxUtils.createXmlReader(resource));
    this.recordFactory = recordFactory;
    this.typePath = path;
    this.baseName = FileUtil.getBaseName(resource.getFilename());
  }

  public GpxIterator(final XMLStreamReader in) {
    this.in = in;
    try {
      StaxUtils.skipToStartElement(in);
      skipRecordDefinition();
    } catch (final XMLStreamException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public void close() {
    try {
      this.in.close();
    } catch (final XMLStreamException e) {
      log.error(e.getMessage(), e);
    }

  }

  @Override
  public RecordDefinition getRecordDefinition() {
    return GpxConstants.GPX_TYPE;
  }

  public String getSchemaName() {
    return this.schemaName;
  }

  @Override
  public boolean hasNext() {
    if (!this.hasNext) {
      return false;
    } else if (this.loadNextObject) {
      return loadNextRecord();
    } else {
      return true;
    }
  }

  protected boolean loadNextRecord() {
    try {
      do {
        this.currentRecord = parseRecord();
      } while (this.currentRecord != null && this.typePath != null
        && !this.currentRecord.getRecordDefinition().getPath().equals(this.typePath));
      this.loadNextObject = false;
      if (this.currentRecord == null) {
        close();
        this.hasNext = false;
      }
      return this.hasNext;
    } catch (final XMLStreamException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public Record next() {
    if (hasNext()) {
      this.loadNextObject = true;
      return this.currentRecord;
    } else {
      throw new NoSuchElementException();
    }
  }

  protected Object parseAttribute(final Record record) {
    final String fieldName = this.in.getLocalName();
    final String stringValue = StaxUtils.getElementText(this.in);
    Object value;
    if (stringValue == null) {
      value = null;
    } else if (fieldName.equals("time")) {
      value = DateUtil.getDate("yyyy-MM-dd'T'HH:mm:ss'Z'", stringValue);
    } else {
      value = stringValue;
    }
    if (value != null) {
      record.setValue(fieldName, value);
    }
    return value;
  }

  //
  // private SimpleAttribute processAttribute() throws XMLStreamException {
  // String propertySchemaName = in.getNamespaceURI();
  // String propertyName = in.getLocalName();
  // in.requireLocalPart(XMLStreamReader.START_ELEMENT, null, null);
  // if (in.getName().equals(GpxConstants.EXTENSION_ELEMENT)
  // || in.getName().equals(GpxConstants.TRACK_SEGMENT_ELEMENT)) {
  // StaxUtils.skipSubTree(in);
  // in.requireLocalPart(XMLStreamReader.END_ELEMENT, propertySchemaName,
  // propertyName);
  // return null;
  // }
  // Object value = null;
  // int eventType = StaxUtils.skipWhitespace(in);
  // switch (eventType) {
  // case XMLStreamReader.CHARACTERS:
  // value = in.getText();
  // StaxUtils.skipToEndElement(in);
  // break;
  // case XMLStreamReader.START_ELEMENT:
  // StaxUtils.skipSubTree(in);
  // return null;
  // case XMLStreamReader.END_ELEMENT:
  // value = null;
  // break;
  // default:
  // // assert false : in.getText();
  // break;
  // }
  // SimpleAttribute attribute = new SimpleAttribute(propertyName, value);
  // in.requireLocalPart(XMLStreamReader.END_ELEMENT, propertySchemaName,
  // propertyName);
  // return attribute;
  // }

  protected Record parsePoint(final String featureType, final double index)
    throws XMLStreamException {
    final Record record = this.recordFactory.createRecord(GpxConstants.GPX_TYPE);
    record.setValue("dataset_name", this.baseName);
    record.setValue("index", index);
    record.setValue("feature_type", featureType);
    final double lat = Double.parseDouble(this.in.getAttributeValue("", "lat"));
    final double lon = Double.parseDouble(this.in.getAttributeValue("", "lon"));
    double elevation = Double.NaN;

    while (this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (this.in.getName().equals(GpxConstants.EXTENSION_ELEMENT)) {
        StaxUtils.skipSubTree(this.in);
      } else if (this.in.getName().equals(GpxConstants.ELEVATION_ELEMENT)) {
        elevation = Double.parseDouble(StaxUtils.getElementText(this.in));
      } else {
        parseAttribute(record);
      }
    }

    Coordinate coord = null;
    if (Double.isNaN(elevation)) {
      coord = new Coordinate(lon, lat);
    } else {
      coord = new Coordinate(lon, lat, elevation);
    }

    final Point point = this.geometryFactory.createPoint(coord);
    record.setValue("location", point);
    return record;
  }

  private Record parseRecord() throws XMLStreamException {
    if (!this.objects.isEmpty()) {
      return this.objects.remove();
    } else {
      if (this.in.getEventType() != XMLStreamConstants.START_ELEMENT) {
        StaxUtils.skipToStartElement(this.in);
      }
      while (this.in.getEventType() == XMLStreamConstants.START_ELEMENT) {
        final QName name = this.in.getName();
        if (name.equals(GpxConstants.WAYPOINT_ELEMENT)) {
          return parseWaypoint();
        } else if (name.equals(GpxConstants.TRACK_ELEMENT)) {
          return parseTrack();
        } else if (name.equals(GpxConstants.ROUTE_ELEMENT)) {
          return parseRoute();
        } else {
          StaxUtils.skipSubTree(this.in);
          this.in.nextTag();
        }
      }
      return null;
    }
  }

  private Record parseRoute() throws XMLStreamException {
    this.index++;
    final Record record = this.recordFactory.createRecord(GpxConstants.GPX_TYPE);
    record.setValue("dataset_name", this.baseName);
    record.setValue("index", this.index);
    record.setValue("feature_type", "rte");
    final List<Record> pointObjects = new ArrayList<Record>();
    int numAxis = 2;
    while (this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (this.in.getName().equals(GpxConstants.EXTENSION_ELEMENT)) {
        StaxUtils.skipSubTree(this.in);
      } else if (this.in.getName().equals(GpxConstants.ROUTE_POINT_ELEMENT)) {
        final double pointIndex = this.index + (pointObjects.size() + 1.0) / 10000;
        final Record pointObject = parseRoutPoint(pointIndex);
        pointObjects.add(pointObject);
        final Point point = pointObject.getGeometry();
        final Coordinates coordinates = CoordinatesUtil.get(point);
        numAxis = Math.max(numAxis, coordinates.getNumAxis());
      } else {
        parseAttribute(record);
      }
    }
    final CoordinatesList points = new DoubleCoordinatesList(pointObjects.size(), numAxis);
    for (int i = 0; i < points.size(); i++) {
      final Record pointObject = pointObjects.get(i);
      final Point point = pointObject.getGeometry();
      final Coordinates coordinates = CoordinatesUtil.get(point);
      for (int j = 0; j < numAxis; j++) {
        final double value = coordinates.getValue(j);
        points.setValue(i, j, value);
      }
    }
    final LineString line;
    if (points.size() > 1) {
      line = this.geometryFactory.createLineString(points);
    } else {
      line = this.geometryFactory.createLineString((CoordinatesList)null);
    }

    record.setGeometryValue(line);
    this.objects.addAll(pointObjects);
    return record;
  }

  private Record parseRoutPoint(final double index) throws XMLStreamException {
    final String featureType = "rtept";
    return parsePoint(featureType, index);
  }

  private Record parseTrack() throws XMLStreamException {
    this.index++;
    final Record record = this.recordFactory.createRecord(GpxConstants.GPX_TYPE);
    record.setValue("dataset_name", this.baseName);
    record.setValue("index", this.index);
    record.setValue("feature_type", "trk");
    final List<CoordinatesList> segments = new ArrayList<CoordinatesList>();
    while (this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (this.in.getName().equals(GpxConstants.EXTENSION_ELEMENT)) {
        StaxUtils.skipSubTree(this.in);
      } else if (this.in.getName().equals(GpxConstants.TRACK_SEGMENT_ELEMENT)) {
        final CoordinatesList points = parseTrackSegment();
        if (points.size() > 1) {
          segments.add(points);
        }
      } else {
        parseAttribute(record);
      }
    }
    final MultiLineString lines = this.geometryFactory.createMultiLineString(segments);
    record.setGeometryValue(lines);
    return record;
  }

  private int parseTrackPoint(final CoordinatesList points) throws XMLStreamException {
    final int index = points.size();

    final String lonText = this.in.getAttributeValue("", "lon");
    final double lon = Double.parseDouble(lonText);
    points.setX(index, lon);

    final String latText = this.in.getAttributeValue("", "lat");
    final double lat = Double.parseDouble(latText);
    points.setY(index, lat);

    int numAxis = 2;

    while (this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (this.in.getName().equals(GpxConstants.EXTENSION_ELEMENT)
        || this.in.getName().equals(GpxConstants.TRACK_SEGMENT_ELEMENT)) {
        StaxUtils.skipSubTree(this.in);
      } else {
        if (this.in.getName().equals(GpxConstants.ELEVATION_ELEMENT)) {
          final String elevationText = StaxUtils.getElementText(this.in);
          final double elevation = Double.parseDouble(elevationText);
          points.setZ(index, elevation);
          if (numAxis < 3) {
            numAxis = 3;
          }
        } else if (this.in.getName().equals(GpxConstants.TIME_ELEMENT)) {
          final String dateText = StaxUtils.getElementText(this.in);
          final Calendar calendar = DateUtil.getIsoCalendar(dateText);
          final long time = calendar.getTimeInMillis();
          points.setTime(index, time);
          if (numAxis < 4) {
            numAxis = 4;
          }
        } else {
          // TODO decide if we want to handle the metadata on a track point
          StaxUtils.skipSubTree(this.in);
        }
      }
    }

    return numAxis;
  }

  private CoordinatesList parseTrackSegment() throws XMLStreamException {
    final CoordinatesList points = new DoubleListCoordinatesList(4);
    int numAxis = 2;
    while (this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      final int pointNumAxis = parseTrackPoint(points);
      numAxis = Math.max(numAxis, pointNumAxis);
    }
    return new DoubleCoordinatesList(numAxis, points);
  }

  private Record parseWaypoint() throws XMLStreamException {
    this.index++;
    final String featureType = "wpt";
    return parsePoint(featureType, this.index);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  public void setSchemaName(final String schemaName) {
    this.schemaName = schemaName;
  }

  public void skipRecordDefinition() throws XMLStreamException {
    StaxUtils.requireLocalPart(this.in, GpxConstants.GPX_ELEMENT);
    StaxUtils.skipToStartElement(this.in);
    if (this.in.getName().equals(GpxConstants.METADATA_ELEMENT)) {
      StaxUtils.skipSubTree(this.in);
      StaxUtils.skipToStartElement(this.in);
    }
  }

  @Override
  public String toString() {
    return this.file.getAbsolutePath();
  }

}
