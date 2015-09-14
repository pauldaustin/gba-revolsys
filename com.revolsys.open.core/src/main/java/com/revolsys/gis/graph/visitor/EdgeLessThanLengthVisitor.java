package com.revolsys.gis.graph.visitor;

import java.util.function.Consumer;

import com.revolsys.gis.graph.Edge;
import com.revolsys.gis.graph.RecordGraph;
import com.revolsys.record.Record;
import com.revolsys.util.ObjectProcessor;

public class EdgeLessThanLengthVisitor extends AbstractEdgeListenerVisitor<Record>
  implements ObjectProcessor<RecordGraph> {

  private double minLength;

  private Consumer<Edge<Record>> consumer;

  public EdgeLessThanLengthVisitor() {
  }

  public EdgeLessThanLengthVisitor(final double minLength) {
    this.minLength = minLength;
  }

  public EdgeLessThanLengthVisitor(final double minLength, final Consumer<Edge<Record>> visitor) {
    this.minLength = minLength;
    this.consumer = visitor;
  }

  @Override
  public void accept(final Edge<Record> edge) {
    final double length = edge.getLength();
    if (length < this.minLength) {
      edgeEvent(edge, "Edge less than length", "Review", length + " < " + this.minLength);
      if (this.consumer != null) {
        this.consumer.accept(edge);
      }
    }
  }

  public double getMinLength() {
    return this.minLength;
  }

  @Override
  public void process(final RecordGraph graph) {
    graph.visitEdges(this);
  }

  public void setMinLength(final double minLength) {
    this.minLength = minLength;
  }
}
