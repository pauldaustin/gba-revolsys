package com.revolsys.data.comparator;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.jts.GeometryComparator;
import com.revolsys.util.CompareUtil;
import com.vividsolutions.jts.geom.Geometry;

public class RecordGeometryComparitor implements Comparator<Record> {

  private static final GeometryComparator GEOMETRYC_COMPARATOR = new GeometryComparator();

  private boolean decending = false;

  public RecordGeometryComparitor() {
  }

  public RecordGeometryComparitor(final boolean decending) {
    this.decending = decending;
  }

  @Override
  public int compare(final Record object1, final Record object2) {
    if (object1 == object2) {
      return 0;
    } else {
      final Geometry geometry1 = object1.getGeometry();
      final Geometry geometry2 = object2.getGeometry();
      int compare = CompareUtil.compare(GEOMETRYC_COMPARATOR, geometry1, geometry2);
      if (compare == 0) {
        compare = geometry1.compareTo(geometry2);
        if (compare == 0) {
          final Object id1 = object1.getIdValue();
          final Object id2 = object2.getIdValue();
          compare = CompareUtil.compare(id1, id2);
          if (compare == 0) {
            final RecordDefinition recordDefinition1 = object1.getRecordDefinition();
            final RecordDefinition recordDefinition2 = object2.getRecordDefinition();
            final Set<String> fieldNames = new LinkedHashSet<String>();
            fieldNames.addAll(recordDefinition1.getFieldNames());
            fieldNames.addAll(recordDefinition2.getFieldNames());
            compare = compareAttributes(object1, object2, fieldNames);
          }
        }
      }
      if (this.decending) {
        return -compare;
      } else {
        return compare;
      }
    }
  }

  public int compareAttributes(final Record object1, final Record object2,
    final Set<String> fieldNames) {
    for (final String fieldName : fieldNames) {
      final Object value1 = object1.getValue(fieldName);
      final Object value2 = object2.getValue(fieldName);
      final int compare = CompareUtil.compare(value1, value2);
      if (compare != 0) {
        return compare;
      }
    }
    return 0;
  }

}
