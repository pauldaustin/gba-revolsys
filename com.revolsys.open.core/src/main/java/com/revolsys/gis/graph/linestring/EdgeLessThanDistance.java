package com.revolsys.gis.graph.linestring;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.revolsys.gis.algorithm.index.IdObjectIndex;
import com.revolsys.gis.graph.Edge;
import com.revolsys.gis.graph.Graph;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.LineSegment;
import com.revolsys.visitor.CreateListVisitor;
import com.revolsys.visitor.DelegatingVisitor;

public class EdgeLessThanDistance extends DelegatingVisitor<Edge<LineSegment>>
  implements Predicate<Edge<LineSegment>> {
  public static List<Edge<LineSegment>> getEdges(final Graph<LineSegment> graph,
    final LineSegment lineSegment, final double maxDistance) {
    final CreateListVisitor<Edge<LineSegment>> results = new CreateListVisitor<Edge<LineSegment>>();
    BoundingBox envelope = CoordinatesListUtil.getBoundingBox(lineSegment.getGeometryFactory(),
      lineSegment);
    envelope = envelope.expand(maxDistance);
    final IdObjectIndex<Edge<LineSegment>> edgeIndex = graph.getEdgeIndex();
    edgeIndex.visit(envelope, new EdgeLessThanDistance(lineSegment, maxDistance, results));
    return results.getList();
  }

  public static List<Edge<LineSegment>> getEdges(final LineStringGraph graph,
    final Coordinates fromPoint, final Coordinates toPoint, final double maxDistance) {
    final LineSegment lineSegment = new LineSegment(fromPoint, toPoint);
    return getEdges(graph, lineSegment, maxDistance);

  }

  private final LineSegment lineSegment;

  private final double maxDistance;

  public EdgeLessThanDistance(final LineSegment lineSegment, final double maxDistance) {
    this.lineSegment = lineSegment;
    this.maxDistance = maxDistance;
  }

  public EdgeLessThanDistance(final LineSegment lineSegment, final double maxDistance,
    final Consumer<Edge<LineSegment>> matchVisitor) {
    super(matchVisitor);
    this.lineSegment = lineSegment;
    this.maxDistance = maxDistance;
  }

  @Override
  public void accept(final Edge<LineSegment> edge) {
    if (test(edge)) {
      super.accept(edge);
    }
  }

  @Override
  public boolean test(final Edge<LineSegment> edge) {
    final LineSegment lineSegment = edge.getObject();
    final double distance = lineSegment.distance(this.lineSegment);
    if (distance <= this.maxDistance) {
      return true;
    } else {
      return false;
    }
  }
}
