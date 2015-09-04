package com.revolsys.gis.algorithm.index;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.LineSegmentUtil;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.util.ExitLoopException;
import com.vividsolutions.jts.geom.Envelope;

public class PointQuadTree<T> extends AbstractPointSpatialIndex<T> {

  private GeometryFactory geometryFactory;

  private PointQuadTreeNode<T> root;

  public PointQuadTree() {
  }

  public PointQuadTree(final GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
  }

  public boolean contains(final Coordinates point) {
    if (this.root == null) {
      return false;
    } else {
      return this.root.contains(point);
    }
  }

  public List<Entry<Coordinates, T>> findEntriesWithinDistance(final Coordinates from,
    final Coordinates to, final double maxDistance) {
    final BoundingBox boundingBox = new BoundingBox(this.geometryFactory, from, to);
    final List<Entry<Coordinates, T>> entries = new ArrayList<Entry<Coordinates, T>>();
    this.root.findEntriesWithin(entries, boundingBox);
    for (final Iterator<Entry<Coordinates, T>> iterator = entries.iterator(); iterator.hasNext();) {
      final Entry<Coordinates, T> entry = iterator.next();
      final Coordinates coordinates = entry.getKey();
      final double distance = LineSegmentUtil.distance(from, to, coordinates);
      if (distance >= maxDistance) {
        iterator.remove();
      }
    }
    return entries;
  }

  public List<T> findWithin(BoundingBox boundingBox) {
    if (this.geometryFactory != null) {
      boundingBox = boundingBox.convert(this.geometryFactory);
    }
    return findWithin((Envelope)boundingBox);
  }

  public List<T> findWithin(final Envelope envelope) {
    final List<T> results = new ArrayList<T>();
    if (this.root != null) {
      this.root.findWithin(results, envelope);
    }
    return results;
  }

  public List<T> findWithinDistance(final Coordinates from, final Coordinates to,
    final double maxDistance) {
    final List<Entry<Coordinates, T>> entries = findEntriesWithinDistance(from, to, maxDistance);
    final List<T> results = new ArrayList<T>();
    for (final Entry<Coordinates, T> entry : entries) {
      final T value = entry.getValue();
      results.add(value);
    }
    return results;
  }

  public List<T> findWithinDistance(final Coordinates point, final double maxDistance) {
    final double x = point.getX();
    final double y = point.getY();
    BoundingBox envelope = new BoundingBox(x, y);
    envelope = envelope.expand(maxDistance);
    final List<T> results = new ArrayList<T>();
    if (this.root != null) {
      this.root.findWithin(results, x, y, maxDistance, envelope);
    }
    return results;
  }

  @Override
  public void put(final Coordinates point, final T value) {
    final double x = point.getX();
    final double y = point.getY();
    put(x, y, value);
  }

  public void put(final double x, final double y, final T value) {
    final PointQuadTreeNode<T> node = new PointQuadTreeNode<T>(value, x, y);
    if (this.root == null) {
      this.root = node;
    } else {
      this.root.put(x, y, node);
    }
  }

  @Override
  public boolean remove(final Coordinates point, final T value) {
    final double x = point.getX();
    final double y = point.getY();
    return remove(x, y, value);
  }

  public boolean remove(final double x, final double y, final T value) {
    if (this.root == null) {
      return false;
    } else {
      this.root = this.root.remove(x, y, value);
      // TODO change so it returns if the item was removed
      return true;
    }
  }

  @Override
  public void visit(final Consumer<T> visitor) {
    if (this.root != null) {
      try {
        this.root.forEach(visitor);
      } catch (final ExitLoopException e) {
      }
    }
  }

  @Override
  public void visit(final Envelope envelope, final Consumer<T> visitor) {
    if (this.root != null) {
      try {
        this.root.forEach(envelope, visitor);
      } catch (final ExitLoopException e) {
      }
    }
  }
}
