package com.revolsys.gis.parallel;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.revolsys.data.record.Record;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

public class OutsideBoundaryObjects {
  private static final Logger LOG = LoggerFactory.getLogger(OutsideBoundaryObjects.class);

  private Set<Record> objects = new LinkedHashSet<Record>();

  private Geometry boundary;

  private PreparedGeometry preparedBoundary;

  public boolean addObject(final Record object) {
    return objects.add(object);
  }

  public boolean boundaryContains(final Record object) {
    final Geometry geometry = object.getGeometryValue();
    return boundaryContains(geometry);
  }

  public boolean boundaryContains(final Geometry geometry) {
    return geometry == null || boundary == null
      || preparedBoundary.contains(geometry);
  }

  public void clear() {
    objects = new LinkedHashSet<Record>();
  }

  public void expandBoundary(final Geometry geometry) {
    if (boundary == null) {
      setBoundary(geometry);
    } else {
      setBoundary(boundary.union(geometry));
    }
  }

  public Set<Record> getAndClearObjects() {
    final Set<Record> objects = this.objects;
    LOG.info("Outside boundary objects size=" + this.objects.size());
    clear();
    return objects;
  }

  public Geometry getBoundary() {
    return boundary;
  }

  public Set<Record> getObjects() {
    return objects;
  }

  public boolean removeObject(final Record object) {
    return objects.remove(object);
  }

  public void setBoundary(final Geometry boundary) {
    this.boundary = boundary;
    this.preparedBoundary = PreparedGeometryFactory.prepare(boundary);
  }

  public void setObjects(final Set<Record> objects) {
    this.objects = objects;
  }
}
