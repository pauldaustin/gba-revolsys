package com.revolsys.gis.graph.visitor;

import java.util.function.Consumer;
import java.util.function.Predicate;

import com.revolsys.gis.graph.Edge;
import com.revolsys.gis.graph.Graph;
import com.revolsys.gis.graph.filter.EdgeObjectFilter;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.util.ExitLoopException;
import com.revolsys.visitor.DelegatingVisitor;

public class ChannelOutEdgeVisitor<T> implements Consumer<Edge<T>> {
  public static <T> void write(final Graph<T> graph, final Channel<T> out) {
    final Consumer<Edge<T>> visitor = new ChannelOutEdgeVisitor<T>(out);
    graph.visitEdges(visitor);
  }

  public static <T> void write(final Graph<T> graph, final Predicate<T> filter,
    final Channel<T> out) {
    final Consumer<Edge<T>> visitor = new ChannelOutEdgeVisitor<T>(out);
    final EdgeObjectFilter<T> edgeFilter = new EdgeObjectFilter<T>(filter);
    final Consumer<Edge<T>> filterVisitor = new DelegatingVisitor<Edge<T>>(edgeFilter, visitor);
    graph.visitEdges(filterVisitor);
  }

  private final Channel<T> out;

  public ChannelOutEdgeVisitor(final Channel<T> out) {
    this.out = out;
  }

  @Override
  public void accept(final Edge<T> edge) {
    if (this.out == null) {
      throw new ExitLoopException();
    } else {
      final T object = edge.getObject();
      this.out.write(object);
    }
  }
}
