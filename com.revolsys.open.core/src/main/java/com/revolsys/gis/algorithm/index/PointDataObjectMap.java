package com.revolsys.gis.algorithm.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.revolsys.filter.Filter;
import com.revolsys.filter.FilterUtil;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.CoordinatesUtil;
import com.revolsys.gis.model.coordinates.DoubleCoordinates;
import com.revolsys.parallel.channel.Channel;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class PointDataObjectMap {

  private Comparator<DataObject> comparator;

  private Map<Coordinates, List<DataObject>> objectMap = new HashMap<Coordinates, List<DataObject>>();

  private int size = 0;

  private boolean removeEmptyLists;

  public PointDataObjectMap() {
  }

  public PointDataObjectMap(final Comparator<DataObject> comparator) {
    this.comparator = comparator;
  }

  /**
   * Add a {@link Point} {@link DataObject} to the list of objects at the given
   * coordinate.
   * 
   * @param pointObjects The map of point objects.
   * @param object The object to add.
   */
  public void add(final DataObject object) {
    final Point point = object.getGeometryValue();
    final List<DataObject> objects = getOrCreateObjects(point);
    objects.add(object);
    if (this.comparator != null) {
      Collections.sort(objects, this.comparator);
    }
    this.size++;
  }

  public void clear() {
    this.size = 0;
    this.objectMap = new HashMap<Coordinates, List<DataObject>>();
  }

  public boolean containsKey(final Point point) {
    final Coordinates coordinates = getCoordinates(point);
    return this.objectMap.containsKey(coordinates);
  }

  public List<DataObject> getAll() {
    final List<DataObject> objects = new ArrayList<DataObject>();
    for (final List<DataObject> objectsAtPoint : this.objectMap.values()) {
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

  public DataObject getFirstMatch(final DataObject object,
    final Filter<DataObject> filter) {
    final List<DataObject> objects = getObjects(object);
    for (final DataObject matchObject : objects) {
      if (filter.accept(matchObject)) {
        return matchObject;
      }
    }
    return null;
  }

  public <V extends DataObject> V getFirstMatch(final Point point) {
    final List<DataObject> objects = getObjects(point);
    if (objects.isEmpty()) {
      return null;
    } else {
      return (V)objects.get(0);
    }

  }

  public List<DataObject> getMatches(final DataObject object,
    final Filter<DataObject> filter) {
    final List<DataObject> objects = getObjects(object);
    final List<DataObject> filteredObjects = FilterUtil.filter(objects, filter);
    return filteredObjects;
  }

  public List<DataObject> getObjects(Coordinates coordinates) {
    coordinates = getCoordinates(coordinates);
    final List<DataObject> objects = this.objectMap.get(coordinates);
    if (objects == null) {
      return Collections.emptyList();
    } else {
      return new ArrayList<DataObject>(objects);
    }
  }

  public List<DataObject> getObjects(final DataObject object) {
    final Point point = object.getGeometryValue();
    final List<DataObject> objects = getObjects(point);
    return objects;
  }

  public List<DataObject> getObjects(final Point point) {
    final Coordinates coordinates = getCoordinates(point);
    final List<DataObject> objects = getObjects(coordinates);
    return objects;
  }

  protected List<DataObject> getOrCreateObjects(final Point point) {
    final Coordinates indexCoordinates = getCoordinates(point);
    List<DataObject> objects = this.objectMap.get(indexCoordinates);
    if (objects == null) {
      objects = new ArrayList<DataObject>(1);
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

  public void remove(final DataObject object) {
    final Geometry geometry = object.getGeometryValue();
    final Coordinates coordinates = getCoordinates(geometry);
    final List<DataObject> objects = this.objectMap.get(coordinates);
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

  public void sort(final DataObject object) {
    if (this.comparator != null) {
      final List<DataObject> objects = getObjects(object);
      if (objects != null) {
        Collections.sort(objects, this.comparator);
      }
    }
  }

  public void write(final Channel<DataObject> out) {
    if (out != null) {
      for (final Coordinates coordinates : getCoordinates()) {
        final List<DataObject> objects = getObjects(coordinates);
        for (final DataObject object : objects) {
          out.write(object);
        }
      }
    }
  }
}
