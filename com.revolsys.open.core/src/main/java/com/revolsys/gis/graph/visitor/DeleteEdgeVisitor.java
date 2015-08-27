package com.revolsys.gis.graph.visitor;

import java.util.function.Consumer;

import com.revolsys.gis.graph.Edge;
import com.revolsys.gis.graph.Graph;
import com.revolsys.util.ObjectProcessor;

public class DeleteEdgeVisitor<T> implements Consumer<Edge<T>>, ObjectProcessor<Graph<T>> {
  @Override
  public void accept(final Edge<T> edge) {
    final Graph<T> graph = edge.getGraph();
    graph.remove(edge);
  }

  @Override
  public void process(final Graph<T> graph) {
    graph.visitEdges(this);
  }

}
