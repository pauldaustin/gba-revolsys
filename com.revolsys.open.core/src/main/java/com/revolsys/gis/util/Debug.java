package com.revolsys.gis.util;

import java.sql.Timestamp;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordState;
import com.revolsys.gis.graph.Edge;
import com.vividsolutions.jts.geom.LineString;

public class Debug {
  public static void equals(final Object object1, final Object object2) {
    if (object1.equals(object2)) {
      noOp();
    }
  }

  public static void idNull(final Record object) {
    if (object.getIdentifier() == null) {
      noOp();
    }
  }

  public static void infinite(final double value) {
    if (Double.isInfinite(value)) {
      noOp();
    }
  }

  public static void isNull(final Object value) {
    if (value == null) {
      noOp();
    }
  }

  public static void modified(final Record object) {
    if (object.getState() == RecordState.Modified) {
      noOp();
    }
  }

  public static void nan(final double value) {
    if (Double.isNaN(value)) {
      noOp();
    }
  }

  public static void noOp() {
  }

  public static void println(final Object object) {
    System.out.println(object);
  }

  public static void printTime() {
    println(new Timestamp(System.currentTimeMillis()));
  }

  public static void typePath(final Edge<?> edge, final String typePath) {
    final String typePath2 = edge.getTypeName();
    equals(typePath2, typePath);
  }

  public static void typePath(final Record object, final String typePath) {
    final String typePath2 = object.getRecordDefinition().getPath();
    equals(typePath2, typePath);
  }

  public static void zeroLegthLine(final LineString line) {
    if (line.getLength() == 0) {
      noOp();
    }
  }
}
