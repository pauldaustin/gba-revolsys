package com.revolsys.gis.graph.visitor;

import java.util.List;
import java.util.function.Predicate;

import com.revolsys.equals.Geometry3DExactEquals;
import com.revolsys.gis.graph.Edge;
import com.revolsys.gis.graph.Graph;
import com.revolsys.gis.graph.RecordGraph;
import com.revolsys.gis.graph.filter.EdgeObjectFilter;
import com.revolsys.gis.graph.filter.EdgeTypeNameFilter;
import com.revolsys.gis.jts.JtsGeometryUtil;
import com.revolsys.gis.jts.filter.EqualFilter;
import com.revolsys.gis.jts.filter.LinearIntersectionFilter;
import com.revolsys.predicate.AndPredicate;
import com.revolsys.record.Record;
import com.revolsys.record.RecordLog;
import com.revolsys.record.filter.OldRecordGeometryFilter;
import com.revolsys.util.ObjectProcessor;
import com.revolsys.visitor.AbstractVisitor;
import com.vividsolutions.jts.geom.LineString;

public class LinearIntersectionNotEqualEdgeLogVisitor extends AbstractVisitor<Edge<Record>>
  implements ObjectProcessor<RecordGraph> {
  private static final String PROCESSED = LinearIntersectionNotEqualLineEdgeCleanupVisitor.class
    .getName() + ".PROCESSED";

  static {
    Geometry3DExactEquals.addExclude(PROCESSED);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void accept(final Edge<Record> edge) {
    final Record object = edge.getObject();
    final LineString line = edge.getLine();
    if (JtsGeometryUtil.getGeometryProperty(line, PROCESSED) != Boolean.TRUE) {
      final String typePath = edge.getTypeName();

      final Graph<Record> graph = edge.getGraph();

      final AndPredicate<Edge<Record>> attributeAndGeometryFilter = new AndPredicate<Edge<Record>>();

      attributeAndGeometryFilter.addFilter(new EdgeTypeNameFilter<Record>(typePath));

      final Predicate<Edge<Record>> filter = getPredicate();
      if (filter != null) {
        attributeAndGeometryFilter.addFilter(filter);
      }

      final Predicate<Record> notEqualLineFilter = new OldRecordGeometryFilter<LineString>(
        new EqualFilter<LineString>(line)).negate();

      final OldRecordGeometryFilter<LineString> linearIntersectionFilter = new OldRecordGeometryFilter<LineString>(
        new LinearIntersectionFilter(line));

      attributeAndGeometryFilter.addFilter(new EdgeObjectFilter<Record>(
        new AndPredicate<Record>(notEqualLineFilter, linearIntersectionFilter)));

      final List<Edge<Record>> intersectingEdges = graph.getEdges(attributeAndGeometryFilter, line);

      if (!intersectingEdges.isEmpty()) {
        RecordLog.error(getClass(), "Overlapping edge", object);
        JtsGeometryUtil.setGeometryProperty(line, PROCESSED, Boolean.TRUE);
        for (final Edge<Record> intersectingEdge : intersectingEdges) {
          final Record intersectingObject = intersectingEdge.getObject();
          final LineString intersectingLine = intersectingObject.getGeometry();
          if (JtsGeometryUtil.getGeometryProperty(intersectingLine, PROCESSED) != Boolean.TRUE) {
            JtsGeometryUtil.setGeometryProperty(intersectingLine, PROCESSED, Boolean.TRUE);
          }
        }
      }
    }
  }

  @Override
  public void process(final RecordGraph graph) {
    graph.visitEdges(this);
  }
}
