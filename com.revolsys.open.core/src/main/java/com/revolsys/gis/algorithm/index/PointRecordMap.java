package com.revolsys.gis.algorithm.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.revolsys.data.record.Record;
import com.revolsys.filter.Filter;
import com.revolsys.filter.FilterUtil;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.CoordinatesUtil;
import com.revolsys.gis.model.coordinates.DoubleCoordinates;
import com.revolsys.parallel.channel.Channel;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class PointRecordMap {

  private Comparator<Record> comparator;

  private Map<Coordinates, List<Record>> objectMap = new HashMap<Coordinates, List<Record>>();

  private int size = 0;

  private boolean removeEmptyLists;

  public PointRecordMap() {
  }

  public PointRecordMap(final Comparator<Record> comparator) {
    this.comparator = comparator;
  }

  /**
   * Add a {@link Point} {@link Record} to the list of objects at the given
   * coordinate.
   *
   * @param pointObjects The map of point objects.
   * @param object The object to add.
   */
  public void add(final Record object) {
    final Point point = object.getGeometryValue();
    final List<Record> objects = getOrCreateObjects(point);
    objects.add(object);
    if (this.comparator != null) {
      Collections.sort(objects, this.comparator);
    }
    this.size++;
  }

  public void clear() {
    this.size = 0;
    this.objectMap = new HashMap<Coordinates, List<Record>>();
  }

  public boolean containsKey(final Point point) {
    final Coordinates coordinates = getCoordinates(point);
    return this.objectMap.containsKey(coordinates);
  }

  public List<Record> getAll() {
    final List<Record> objects = new ArrayList<Record>();
    for (final List<Record> objectsAtPoint : this.objectMap.values()) {
      objects.addAll(objectsAtPoint);
    }
    return objects;
  }

  public Set<Coordinates> getCoordinates() {
    return Collections.unmodifiableSet(this.objectMap.keySet());
  }

  private Coordinates getCoordinates(final Coordinates point) {
    final double x = point.getX();
    final double y = point.getY();
    final Coordinates coordinates = new DoubleCoordinates(x, y);
    return coordinates;
  }

  private Coordinates getCoordinates(final Geometry geometry) {
    final Coordinates coordinates = CoordinatesUtil.get(geometry);
    return getCoordinates(coordinates);
  }

  private Coordinates getCoordinates(final Point point) {
    final double x = point.getX();
    final double y = point.getY();
    final Coordinates coordinates = new DoubleCoordinates(x, y);
    return coordinates;
  }

  public <V extends Record> V getFirstMatch(final Point point) {
    final List<Record> objects = getObjects(point);
    if (objects.isEmpty()) {
      return null;
    } else {
      return (V)objects.get(0);
    }

  }

  public Record getFirstMatch(final Record object, final Filter<Record> filter) {
    final List<Record> objects = getObjects(object);
    for (final Record matchObject : objects) {
      if (filter.accept(matchObject)) {
        return matchObject;
      }
    }
    return null;
  }

  public List<Record> getMatches(final Record object, final Filter<Record> filter) {
    final List<Record> objects = getObjects(object);
    final List<Record> filteredObjects = FilterUtil.filter(objects, filter);
    return filteredObjects;
  }

  public List<Record> getObjects(Coordinates coordinates) {
    coordinates = getCoordinates(coordinates);
    final List<Record> objects = this.objectMap.get(coordinates);
    if (objects == null) {
      return Collections.emptyList();
    } else {
      return new ArrayList<Record>(objects);
    }
  }

  public List<Record> getObjects(final Point point) {
    final Coordinates coordinates = getCoordinates(point);
    final List<Record> objects = getObjects(coordinates);
    return objects;
  }

  public List<Record> getObjects(final Record object) {
    final Point point = object.getGeometryValue();
    final List<Record> objects = getObjects(point);
    return objects;
  }

  protected List<Record> getOrCreateObjects(final Point point) {
    final Coordinates indexCoordinates = getCoordinates(point);
    List<Record> objects = this.objectMap.get(indexCoordinates);
    if (objects == null) {
      objects = new ArrayList<Record>(1);
      this.objectMap.put(indexCoordinates, objects);
    }
    return objects;
  }

  public void initialize(final Point point) {
    if (!isRemoveEmptyLists()) {
      getOrCreateObjects(point);
    }
  }

  public boolean isRemoveEmptyLists() {
    return this.removeEmptyLists;
  }

  public void remove(final Record object) {
    final Geometry geometry = object.getGeometryValue();
    final Coordinates coordinates = getCoordinates(geometry);
    final List<Record> objects = this.objectMap.get(coordinates);
    if (objects != null) {
      objects.remove(object);
      if (objects.isEmpty()) {
        if (isRemoveEmptyLists()) {
          this.objectMap.remove(coordinates);
        }
      } else if (this.comparator != null) {
        Collections.sort(objects, this.comparator);
      }
    }
    this.size--;
  }

  public void setRemoveEmptyLists(final boolean removeEmptyLists) {
    this.removeEmptyLists = removeEmptyLists;
  }

  public int size() {
    return this.size;
  }

  public void sort(final Record object) {
    if (this.comparator != null) {
      final List<Record> objects = getObjects(object);
      if (objects != null) {
        Collections.sort(objects, this.comparator);
      }
    }
  }

  public void write(final Channel<Record> out) {
    if (out != null) {
      for (final Coordinates coordinates : getCoordinates()) {
        final List<Record> objects = getObjects(coordinates);
        for (final Record object : objects) {
          out.write(object);
        }
      }
    }
  }
}
