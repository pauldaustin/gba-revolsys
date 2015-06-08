package com.revolsys.gis.model.geometry.operation.geomgraph;

import java.io.PrintStream;

import com.revolsys.gis.model.geometry.util.TopologyException;
import com.vividsolutions.jts.geom.Location;

/**
 * @version 1.7
 */
public class DirectedEdge extends EdgeEnd {

  /**
   * Computes the factor for the change in depth when moving from one location
   * to another. E.g. if crossing from the INTERIOR to the EXTERIOR the depth
   * decreases, so the factor is -1
   */
  public static int depthFactor(final int currLocation, final int nextLocation) {
    if (currLocation == Location.EXTERIOR && nextLocation == Location.INTERIOR) {
      return 1;
    } else if (currLocation == Location.INTERIOR && nextLocation == Location.EXTERIOR) {
      return -1;
    }
    return 0;
  }

  protected boolean isForward;

  private boolean isInResult = false;

  private boolean isVisited = false;

  private DirectedEdge sym; // the symmetric edge

  private DirectedEdge next; // the next edge in the edge ring for the polygon
                             // containing this edge

  private DirectedEdge nextMin; // the next edge in the MinimalEdgeRing that
                                // contains this edge

  private EdgeRing edgeRing; // the EdgeRing that this edge is part of

  private EdgeRing minEdgeRing; // the MinimalEdgeRing that this edge is part of

  /**
   * The depth of each side (position) of this edge. The 0 element of the array
   * is never used.
   */
  private final int[] depth = {
    0, -999, -999
  };

  public DirectedEdge(final Edge edge, final boolean isForward) {
    super(edge);
    this.isForward = isForward;
    if (isForward) {
      init(edge.getCoordinate(0), edge.getCoordinate(1));
    } else {
      final int n = edge.getNumPoints() - 1;
      init(edge.getCoordinate(n), edge.getCoordinate(n - 1));
    }
    computeDirectedLabel();
  }

  /**
   * Compute the label in the appropriate orientation for this DirEdge
   */
  private void computeDirectedLabel() {
    this.label = new Label(this.edge.getLabel());
    if (!this.isForward) {
      this.label.flip();
    }
  }

  public int getDepth(final int position) {
    return this.depth[position];
  }

  public int getDepthDelta() {
    int depthDelta = this.edge.getDepthDelta();
    if (!this.isForward) {
      depthDelta = -depthDelta;
    }
    return depthDelta;
  }

  @Override
  public Edge getEdge() {
    return this.edge;
  }

  public EdgeRing getEdgeRing() {
    return this.edgeRing;
  }

  public EdgeRing getMinEdgeRing() {
    return this.minEdgeRing;
  }

  public DirectedEdge getNext() {
    return this.next;
  }

  public DirectedEdge getNextMin() {
    return this.nextMin;
  }

  /**
   * Each Edge gives rise to a pair of symmetric DirectedEdges, in opposite
   * directions.
   *
   * @return the DirectedEdge for the same Edge but in the opposite direction
   */
  public DirectedEdge getSym() {
    return this.sym;
  }

  public boolean isForward() {
    return this.isForward;
  }

  public boolean isInResult() {
    return this.isInResult;
  }

  /**
   * This is an interior Area edge if
   * <ul>
   * <li>its label is an Area label for both Geometries
   * <li>and for each Geometry both sides are in the interior.
   * </ul>
   *
   * @return true if this is an interior Area edge
   */
  public boolean isInteriorAreaEdge() {
    boolean isInteriorAreaEdge = true;
    for (int i = 0; i < 2; i++) {
      if (!(this.label.isArea(i) && this.label.getLocation(i, Position.LEFT) == Location.INTERIOR && this.label.getLocation(
        i, Position.RIGHT) == Location.INTERIOR)) {
        isInteriorAreaEdge = false;
      }
    }
    return isInteriorAreaEdge;
  }

  /**
   * This edge is a line edge if
   * <ul>
   * <li>at least one of the labels is a line label
   * <li>any labels which are not line labels have all Locations = EXTERIOR
   * </ul>
   */
  public boolean isLineEdge() {
    final boolean isLine = this.label.isLine(0) || this.label.isLine(1);
    final boolean isExteriorIfArea0 = !this.label.isArea(0)
      || this.label.allPositionsEqual(0, Location.EXTERIOR);
    final boolean isExteriorIfArea1 = !this.label.isArea(1)
      || this.label.allPositionsEqual(1, Location.EXTERIOR);

    return isLine && isExteriorIfArea0 && isExteriorIfArea1;
  }

  public boolean isVisited() {
    return this.isVisited;
  }

  @Override
  public void print(final PrintStream out) {
    super.print(out);
    out.print(" " + this.depth[Position.LEFT] + "/" + this.depth[Position.RIGHT]);
    out.print(" (" + getDepthDelta() + ")");
    // out.print(" " + this.hashCode());
    // if (next != null) out.print(" next:" + next.hashCode());
    if (this.isInResult) {
      out.print(" inResult");
    }
  }

  public void printEdge(final PrintStream out) {
    print(out);
    out.print(" ");
    if (this.isForward) {
      this.edge.print(out);
    } else {
      this.edge.printReverse(out);
    }
  }

  public void setDepth(final int position, final int depthVal) {
    if (this.depth[position] != -999) {
      // if (depth[position] != depthVal) {
      // Debug.print(this);
      // }
      if (this.depth[position] != depthVal) {
        throw new TopologyException("assigned depths do not match", getCoordinate());
        // Assert.isTrue(depth[position] == depthVal,
        // "assigned depths do not match at " + getCoordinate());
      }
    }
    this.depth[position] = depthVal;
  }

  /**
   * Set both edge depths. One depth for a given side is provided. The other is
   * computed depending on the Location transition and the depthDelta of the
   * edge.
   */
  public void setEdgeDepths(final int position, final int depth) {
    // get the depth transition delta from R to L for this directed Edge
    int depthDelta = getEdge().getDepthDelta();
    if (!this.isForward) {
      depthDelta = -depthDelta;
    }

    // if moving from L to R instead of R to L must change sign of delta
    int directionFactor = 1;
    if (position == Position.LEFT) {
      directionFactor = -1;
    }

    final int oppositePos = Position.opposite(position);
    final int delta = depthDelta * directionFactor;
    // TESTINGint delta = depthDelta * DirectedEdge.depthFactor(loc,
    // oppositeLoc);
    final int oppositeDepth = depth + delta;
    setDepth(position, depth);
    setDepth(oppositePos, oppositeDepth);
  }

  public void setEdgeRing(final EdgeRing edgeRing) {
    this.edgeRing = edgeRing;
  }

  public void setInResult(final boolean isInResult) {
    this.isInResult = isInResult;
  }

  public void setMinEdgeRing(final EdgeRing minEdgeRing) {
    this.minEdgeRing = minEdgeRing;
  }

  public void setNext(final DirectedEdge next) {
    this.next = next;
  }

  public void setNextMin(final DirectedEdge nextMin) {
    this.nextMin = nextMin;
  }

  public void setSym(final DirectedEdge de) {
    this.sym = de;
  }

  public void setVisited(final boolean isVisited) {
    this.isVisited = isVisited;
  }

  /**
   * setVisitedEdge marks both DirectedEdges attached to a given Edge. This is
   * used for edges corresponding to lines, which will only appear oriented in a
   * single direction in the result.
   */
  public void setVisitedEdge(final boolean isVisited) {
    setVisited(isVisited);
    this.sym.setVisited(isVisited);
  }

}
