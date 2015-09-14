package com.revolsys.gis.algorithm.index;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.revolsys.gis.algorithm.index.quadtree.QuadTree;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.record.Record;
import com.revolsys.record.filter.OldRecordGeometryDistanceFilter;
import com.revolsys.record.filter.OldRecordGeometryIntersectsFilter;
import com.revolsys.record.filter.RecordEqualsFilter;
import com.revolsys.visitor.CreateListVisitor;
import com.vividsolutions.jts.geom.Geometry;

public class RecordQuadTree extends QuadTree<Record> {
  public RecordQuadTree() {
  }

  public RecordQuadTree(final Collection<? extends Record> objects) {
    insert(objects);
  }

  public RecordQuadTree(final GeometryFactory geometryFactory) {
    super(geometryFactory);
  }

  public RecordQuadTree(final GeometryFactory geometryFactory,
    final Collection<? extends Record> objects) {
    super(geometryFactory);
    insert(objects);
  }

  public void insert(final Collection<? extends Record> objects) {
    for (final Record object : objects) {
      insert(object);
    }
  }

  public void insert(final Record object) {
    if (object != null) {
      final Geometry geometry = object.getGeometry();
      if (geometry != null && !geometry.isEmpty()) {
        final BoundingBox boundingBox = BoundingBox.getBoundingBox(geometry);
        insert(boundingBox, object);
      }
    }
  }

  public void insertAll(final Collection<? extends Record> objects) {
    for (final Record object : objects) {
      insert(object);
    }
  }

  @Override
  public List<Record> query(final BoundingBox boundingBox) {
    final List<Record> results = super.query(boundingBox);
    for (final Iterator<Record> iterator = results.iterator(); iterator.hasNext();) {
      final Record object = iterator.next();
      final Geometry geometry = object.getGeometry();
      final BoundingBox objectBoundingBox = BoundingBox.getBoundingBox(geometry);
      if (!boundingBox.intersects(objectBoundingBox)) {
        iterator.remove();
      }
    }
    return results;
  }

  public void query(final Geometry geometry, final Consumer<Record> visitor) {
    final BoundingBox boundingBox = BoundingBox.getBoundingBox(geometry);
    forEach(boundingBox, visitor);
  }

  public List<Record> queryDistance(final Geometry geometry, final double distance) {
    BoundingBox boundingBox = BoundingBox.getBoundingBox(geometry);
    boundingBox = boundingBox.expand(distance);
    final Predicate<Record> filter = new OldRecordGeometryDistanceFilter(geometry, distance);
    return queryList(boundingBox, filter);
  }

  public List<Record> queryEnvelope(final Record object) {
    final Geometry geometry = object.getGeometry();
    return queryBoundingBox(geometry);
  }

  public Record queryFirst(final Record object, final Predicate<Record> filter) {
    final Geometry geometry = object.getGeometry();
    return queryFirst(geometry, filter);
  }

  public Record queryFirstEquals(final Record object, final Collection<String> excludedAttributes) {
    final RecordEqualsFilter filter = new RecordEqualsFilter(object, excludedAttributes);
    return queryFirst(object, filter);
  }

  public List<Record> queryIntersects(final BoundingBox boundingBox) {

    final BoundingBox convertedBoundingBox = boundingBox.convert(getGeometryFactory());
    if (convertedBoundingBox.isEmpty()) {
      return Collections.emptyList();
    } else {
      final Geometry geometry = convertedBoundingBox.toPolygon(1, 1);
      final Predicate<Record> filter = new OldRecordGeometryIntersectsFilter(geometry);
      return queryList(geometry, filter);
    }
  }

  public List<Record> queryIntersects(Geometry geometry) {
    if (geometry == null) {
      return Collections.emptyList();
    } else {
      final GeometryFactory geometryFactory = getGeometryFactory();
      if (geometryFactory != null) {
        geometry = geometryFactory.copy(geometry);
      }
      final Predicate<Record> filter = new OldRecordGeometryIntersectsFilter(geometry);
      return queryList(geometry, filter);
    }
  }

  public List<Record> queryList(final BoundingBox boundingBox, final Predicate<Record> filter) {
    return queryList(boundingBox, filter, null);
  }

  public List<Record> queryList(final BoundingBox boundingBox, final Predicate<Record> filter,
    final Comparator<Record> comparator) {
    final CreateListVisitor<Record> listVisitor = new CreateListVisitor<Record>(filter);
    forEach(boundingBox, listVisitor);
    final List<Record> list = listVisitor.getList();
    if (comparator != null) {
      Collections.sort(list, comparator);
    }
    return list;
  }

  public List<Record> queryList(final Geometry geometry, final Predicate<Record> filter) {
    final BoundingBox boundingBox = BoundingBox.getBoundingBox(geometry);
    return queryList(boundingBox, filter);
  }

  public List<Record> queryList(final Geometry geometry, final Predicate<Record> filter,
    final Comparator<Record> comparator) {
    final BoundingBox boundingBox = BoundingBox.getBoundingBox(geometry);
    return queryList(boundingBox, filter, comparator);
  }

  public List<Record> queryList(final Record object, final Predicate<Record> filter) {
    final Geometry geometry = object.getGeometry();
    return queryList(geometry, filter);
  }

  public void remove(final Collection<? extends Record> objects) {
    for (final Record object : objects) {
      remove(object);
    }
  }

  public boolean remove(final Record object) {
    final Geometry geometry = object.getGeometry();
    if (geometry == null) {
      return false;
    } else {
      final BoundingBox boundinBox = BoundingBox.getBoundingBox(geometry);
      return super.remove(boundinBox, object);
    }
  }
}
