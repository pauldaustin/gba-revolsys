package com.revolsys.gis.graph.visitor;

import java.util.List;
import java.util.function.Consumer;

import com.revolsys.gis.algorithm.index.IdObjectIndex;
import com.revolsys.gis.graph.Edge;
import com.revolsys.gis.graph.Graph;
import com.revolsys.visitor.CreateListVisitor;
import com.vividsolutions.jts.geom.Dimension;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.IntersectionMatrix;
import com.vividsolutions.jts.geom.LineString;

public class EdgeIntersectLineVisitor<T> implements Consumer<Edge<T>> {

  public static <T> List<Edge<T>> getEdges(final Graph<T> graph, final LineString line) {
    final CreateListVisitor<Edge<T>> results = new CreateListVisitor<Edge<T>>();
    final Envelope env = line.getEnvelopeInternal();
    final IdObjectIndex<Edge<T>> index = graph.getEdgeIndex();
    index.visit(env, new EdgeIntersectLineVisitor<T>(line, results));
    return results.getList();

  }

  private final LineString line;

  private final Consumer<Edge<T>> matchVisitor;

  public EdgeIntersectLineVisitor(final LineString line, final Consumer<Edge<T>> matchVisitor) {
    this.line = line;
    this.matchVisitor = matchVisitor;
  }

  @Override
  public void accept(final Edge<T> edge) {
    final LineString line = edge.getLine();
    final IntersectionMatrix relate = this.line.relate(line);
    if (relate.get(0, 0) == Dimension.L) {
      this.matchVisitor.accept(edge);
    }
  }

}
