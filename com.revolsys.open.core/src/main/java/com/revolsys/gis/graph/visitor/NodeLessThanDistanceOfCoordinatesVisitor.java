package com.revolsys.gis.graph.visitor;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.revolsys.gis.algorithm.index.IdObjectIndex;
import com.revolsys.gis.graph.Graph;
import com.revolsys.gis.graph.Node;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.visitor.CreateListVisitor;

public class NodeLessThanDistanceOfCoordinatesVisitor<T> implements Consumer<Node<T>> {
  public static <T> List<Node<T>> getNodes(final Graph<T> graph, final Coordinates point,
    final double maxDistance) {
    final CreateListVisitor<Node<T>> results = new CreateListVisitor<Node<T>>();
    final Consumer<Node<T>> visitor = new NodeWithinDistanceOfCoordinateVisitor<T>(point,
      maxDistance, results);
    BoundingBox envelope = new BoundingBox(point);
    envelope = envelope.expand(maxDistance);
    final IdObjectIndex<Node<T>> nodeIndex = graph.getNodeIndex();
    nodeIndex.visit(envelope, visitor);
    final List<Node<T>> nodes = results.getList();
    Collections.sort(nodes);
    return nodes;
  }

  private final Coordinates coordinates;

  private final Consumer<Node<T>> matchVisitor;

  private final double maxDistance;

  public NodeLessThanDistanceOfCoordinatesVisitor(final Coordinates coordinates,
    final double maxDistance, final Consumer<Node<T>> matchVisitor) {
    this.coordinates = coordinates;
    this.maxDistance = maxDistance;
    this.matchVisitor = matchVisitor;
  }

  @Override
  public void accept(final Node<T> node) {
    final double distance = this.coordinates.distance(node);
    if (distance < this.maxDistance) {
      this.matchVisitor.accept(node);
    }
  }

}
