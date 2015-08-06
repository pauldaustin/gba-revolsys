package com.revolsys.format.gpx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.namespace.QName;

import com.revolsys.data.record.Record;
import com.revolsys.format.xml.XmlWriter;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.epsg.EpsgCoordinateSystems;
import com.revolsys.gis.cs.projection.CoordinateProjectionUtil;
import com.revolsys.gis.cs.projection.CoordinatesOperation;
import com.revolsys.gis.cs.projection.ProjectionFactory;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.DoubleCoordinates;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.gis.model.coordinates.list.InPlaceIterator;
import com.revolsys.io.AbstractWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class GpxWriter extends AbstractWriter<Record> {

  private String commentAttribute = "comment";

  private String descriptionAttribute = "description";

  private File file;

  private String nameAttribute = "name";

  private final XmlWriter out;

  public GpxWriter(final File file) throws IOException {
    this(new FileWriter(file));
    this.file = file;
  }

  public GpxWriter(final Writer writer) throws IOException {
    this.out = new XmlWriter(new BufferedWriter(writer));
    this.out.setIndent(false);
    this.out.startDocument("UTF-8", "1.0");

    this.out.startTag(GpxConstants.GPX_ELEMENT);
    this.out.attribute(GpxConstants.VERSION_ATTRIBUTE, "1.1");
    this.out.attribute(GpxConstants.CREATOR_ATTRIBUTE, "Revolution Systems Inc. - GIS");
  }

  @Override
  public void close() {
    this.out.endTag();
    this.out.endDocument();
    this.out.close();
  }

  @Override
  public void flush() {
    this.out.flush();
  }

  public String getCommentAttribute() {
    return this.commentAttribute;
  }

  public String getDescriptionAttribute() {
    return this.descriptionAttribute;
  }

  public String getNameAttribute() {
    return this.nameAttribute;
  }

  public void setCommentAttribute(final String commentAttribute) {
    this.commentAttribute = commentAttribute;
  }

  public void setDescriptionAttribute(final String descriptionAttribute) {
    this.descriptionAttribute = descriptionAttribute;
  }

  public void setNameAttribute(final String nameAttribute) {
    this.nameAttribute = nameAttribute;
  }

  @Override
  public String toString() {
    return this.file.getAbsolutePath();
  }

  @Override
  public void write(final Record object) {
    try {
      final Geometry geometry = object.getGeometry();
      if (geometry instanceof Point) {
        writeWaypoint(object);
      } else if (geometry instanceof LineString) {
        writeTrack(object);
      }
    } catch (final IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private void writeAttributes(final Record object) {
    final Object time = object.getValue("timestamp");
    if (time != null) {
      if (time instanceof Date) {
        final DateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.");
        timestampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.out.element(GpxConstants.TIME_ELEMENT, timestampFormat.format(time));
      } else {
        this.out.element(GpxConstants.TIME_ELEMENT, time.toString());
      }
    }
    writeElement(object, GpxConstants.NAME_ELEMENT, this.nameAttribute);
    writeElement(object, GpxConstants.COMMENT_ELEMENT, this.commentAttribute);
    writeElement(object, GpxConstants.DESCRIPTION_ELEMENT, this.descriptionAttribute);
  }

  private void writeElement(final Record object, final QName tag, final String attributeName) {
    final String name = object.getValue(attributeName);
    if (name != null && name.length() > 0) {
      this.out.element(tag, name);
    }
  }

  private void writeTrack(final Record object) throws IOException {
    this.out.startTag(GpxConstants.TRACK_ELEMENT);
    final LineString line = object.getGeometry();
    final int srid = line.getSRID();
    final CoordinateSystem coordinateSystem = EpsgCoordinateSystems.getCoordinateSystem(srid);
    final CoordinatesOperation inverseCoordinatesOperation = ProjectionFactory.getToGeographicsCoordinatesOperation(coordinateSystem);
    final CoordinatesList coordinatesList = CoordinatesListUtil.get(line);
    writeAttributes(object);
    this.out.startTag(GpxConstants.TRACK_SEGMENT_ELEMENT);
    final DoubleCoordinates geoCoordinates = new DoubleCoordinates(coordinatesList.getDimension());

    for (final Coordinates coordinates : new InPlaceIterator(coordinatesList)) {
      inverseCoordinatesOperation.perform(coordinates, geoCoordinates);
      this.out.startTag(GpxConstants.TRACK_POINT_ELEMENT);
      this.out.attribute(GpxConstants.LON_ATTRIBUTE, geoCoordinates.getX());
      this.out.attribute(GpxConstants.LAT_ATTRIBUTE, geoCoordinates.getY());
      if (coordinatesList.getDimension() > 2) {
        final double elevation = geoCoordinates.getValue(2);
        if (!Double.isNaN(elevation)) {
          this.out.element(GpxConstants.ELEVATION_ELEMENT, String.valueOf(elevation));
        }
      }
      this.out.endTag(GpxConstants.TRACK_POINT_ELEMENT);
    }
    this.out.endTag(GpxConstants.TRACK_SEGMENT_ELEMENT);
    this.out.endTag(GpxConstants.TRACK_ELEMENT);
  }

  private void writeWaypoint(final Record wayPoint) throws IOException {
    this.out.startTag(GpxConstants.WAYPOINT_ELEMENT);
    final Point point = wayPoint.getGeometry();
    final Coordinate coordinate = point.getCoordinate();
    final CoordinateSystem coordinateSystem = EpsgCoordinateSystems.getCoordinateSystem(point.getSRID());
    final CoordinatesOperation inverseCoordinatesOperation = ProjectionFactory.getToGeographicsCoordinatesOperation(coordinateSystem);
    final Coordinate geoCoordinate = CoordinateProjectionUtil.perform(inverseCoordinatesOperation,
      coordinate);
    this.out.attribute(GpxConstants.LON_ATTRIBUTE, geoCoordinate.x);
    this.out.attribute(GpxConstants.LAT_ATTRIBUTE, geoCoordinate.y);
    if (point.getCoordinateSequence().getDimension() > 2) {
      final double elevation = geoCoordinate.z;
      if (!Double.isNaN(elevation)) {
        this.out.element(GpxConstants.ELEVATION_ELEMENT, String.valueOf(elevation));
      }
    }
    writeAttributes(wayPoint);
    this.out.endTag(GpxConstants.WAYPOINT_ELEMENT);
  }

}
