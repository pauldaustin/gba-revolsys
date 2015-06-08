package com.revolsys.gis.graph.filter;

import com.revolsys.filter.Filter;
import com.revolsys.gis.graph.Edge;
import com.revolsys.gis.graph.Node;
import com.revolsys.gis.jts.LineStringUtil;
import com.revolsys.jts.geom.BoundingBox;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;

public class IsPointOnLineEdgeFilter<T> implements Filter<Node<T>> {

  private final Edge<T> edge;

  private BoundingBox envelope;

  private final double maxDistance;

  public IsPointOnLineEdgeFilter(final Edge<T> edge, final double maxDistance) {
    this.edge = edge;
    this.maxDistance = maxDistance;
    this.envelope = edge.getBoundingBox();
    this.envelope = this.envelope.expand(maxDistance);
  }

  @Override
  public boolean accept(final Node<T> node) {
    final LineString line = this.edge.getLine();
    if (!this.edge.hasNode(node)) {
      if (this.envelope.intersects(new BoundingBox(node))) {
        if (LineStringUtil.isPointOnLine(line, node, this.maxDistance)) {
          return true;
        }
      }
    }
    return false;
  }

  public Envelope getEnvelope() {
    return this.envelope;
  }

}
