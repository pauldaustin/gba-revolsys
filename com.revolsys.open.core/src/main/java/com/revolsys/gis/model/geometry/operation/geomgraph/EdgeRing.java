package com.revolsys.gis.model.geometry.operation.geomgraph;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.gis.model.geometry.GeometryFactory;
import com.revolsys.gis.model.geometry.LinearRing;
import com.revolsys.gis.model.geometry.Polygon;
import com.revolsys.gis.model.geometry.algorithm.RayCrossingCounter;
import com.revolsys.gis.model.geometry.algorithm.locate.Location;
import com.revolsys.gis.model.geometry.impl.BoundingBox;
import com.revolsys.gis.model.geometry.util.TopologyException;
import com.vividsolutions.jts.util.Assert;

public abstract class EdgeRing {

  protected DirectedEdge startDe; // the directed edge which starts the list of
                                  // edges for this EdgeRing

  private int maxNodeDegree = -1;

  private final List edges = new ArrayList(); // the DirectedEdges making up
                                              // this

  // EdgeRing

  private final List<Coordinates> pts = new ArrayList<Coordinates>();

  private final Label label = new Label(Location.NONE.getIndex()); // label
                                                                   // stores the

  // locations

  // of each geometry on the
  // face surrounded by this
  // ring

  private LinearRing ring; // the ring created for this EdgeRing

  private boolean isHole;

  private EdgeRing shell; // if non-null, the ring is a hole and this EdgeRing
                          // is its containing shell

  private final List<EdgeRing> holes = new ArrayList<EdgeRing>(); // a list of
                                                                  // EdgeRings
                                                                  // which

  // are

  // holes in this EdgeRing

  protected GeometryFactory geometryFactory;

  public EdgeRing(final DirectedEdge start, final GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
    computePoints(start);
    computeRing();
  }

  public void addHole(final EdgeRing ring) {
    this.holes.add(ring);
  }

  protected void addPoints(final Edge edge, final boolean isForward, final boolean isFirstEdge) {
    final CoordinatesList edgePts = edge.getCoordinates();
    if (isForward) {
      int startIndex = 1;
      if (isFirstEdge) {
        startIndex = 0;
      }
      for (int i = startIndex; i < edgePts.size(); i++) {
        this.pts.add(edgePts.get(i));
      }
    } else { // is backward
      int startIndex = edgePts.size() - 2;
      if (isFirstEdge) {
        startIndex = edgePts.size() - 1;
      }
      for (int i = startIndex; i >= 0; i--) {
        this.pts.add(edgePts.get(i));
      }
    }
  }

  private void computeMaxNodeDegree() {
    this.maxNodeDegree = 0;
    DirectedEdge de = this.startDe;
    do {
      final Node node = de.getNode();
      final int degree = ((DirectedEdgeStar)node.getEdges()).getOutgoingDegree(this);
      if (degree > this.maxNodeDegree) {
        this.maxNodeDegree = degree;
      }
      de = getNext(de);
    } while (de != this.startDe);
    this.maxNodeDegree *= 2;
  }

  /**
   * Collect all the points from the DirectedEdges of this ring into a
   * contiguous list
   */
  protected void computePoints(final DirectedEdge start) {
    // System.out.println("buildRing");
    this.startDe = start;
    DirectedEdge de = start;
    boolean isFirstEdge = true;
    do {
      // Assert.isTrue(de != null, "found null Directed Edge");
      if (de == null) {
        throw new TopologyException("Found null DirectedEdge");
      }
      if (de.getEdgeRing() == this) {
        throw new TopologyException("Directed Edge visited twice during ring-building at "
          + de.getCoordinate());
      }

      this.edges.add(de);
      // Debug.println(de);
      // Debug.println(de.getEdge());
      final Label label = de.getLabel();
      Assert.isTrue(label.isArea());
      mergeLabel(label);
      addPoints(de.getEdge(), de.isForward(), isFirstEdge);
      isFirstEdge = false;
      setEdgeRing(de, this);
      de = getNext(de);
    } while (de != this.startDe);
  }

  /**
   * Compute a LinearRing from the point list previously collected. Test if the
   * ring is a hole (i.e. if it is CCW) and set the hole flag accordingly.
   */
  public void computeRing() {
    if (this.ring != null) {
      return; // don't compute more than once
    }
    final Coordinates[] coord = new Coordinates[this.pts.size()];
    for (int i = 0; i < this.pts.size(); i++) {
      coord[i] = this.pts.get(i);
    }
    this.ring = this.geometryFactory.createLinearRing(coord);
    this.isHole = CoordinatesListUtil.isCCW(this.ring);
    // Debug.println( (isHole ? "hole - " : "shell - ") +
    // WKTWriter.toLineString(new
    // CoordinateArraySequence(ring.getCoordinates())));
  }

  /**
   * This method will cause the ring to be computed. It will also check any
   * holes, if they have been assigned.
   */
  public boolean containsPoint(final Coordinates p) {
    final LinearRing shell = getLinearRing();
    final BoundingBox env = shell.getBoundingBox();
    if (!env.contains(p)) {
      return false;
    }
    if (RayCrossingCounter.locatePointInRing(p, this.ring) == Location.EXTERIOR) {
      return false;
    }

    for (final Object element : this.holes) {
      final EdgeRing hole = (EdgeRing)element;
      if (hole.containsPoint(p)) {
        return false;
      }
    }
    return true;
  }

  public Coordinates getCoordinate(final int i) {
    return this.pts.get(i);
  }

  /**
   * Returns the list of DirectedEdges that make up this EdgeRing
   */
  public List getEdges() {
    return this.edges;
  }

  public Label getLabel() {
    return this.label;
  }

  public LinearRing getLinearRing() {
    return this.ring;
  }

  public int getMaxNodeDegree() {
    if (this.maxNodeDegree < 0) {
      computeMaxNodeDegree();
    }
    return this.maxNodeDegree;
  }

  abstract public DirectedEdge getNext(DirectedEdge de);

  public EdgeRing getShell() {
    return this.shell;
  }

  public boolean isHole() {
    // computePoints();
    return this.isHole;
  }

  public boolean isIsolated() {
    return this.label.getGeometryCount() == 1;
  }

  public boolean isShell() {
    return this.shell == null;
  }

  protected void mergeLabel(final Label deLabel) {
    mergeLabel(deLabel, 0);
    mergeLabel(deLabel, 1);
  }

  /**
   * Merge the RHS label from a DirectedEdge into the label for this EdgeRing.
   * The DirectedEdge label may be null. This is acceptable - it results from a
   * node which is NOT an intersection node between the Geometries (e.g. the end
   * node of a LinearRing). In this case the DirectedEdge label does not
   * contribute any information to the overall labelling, and is simply skipped.
   */
  protected void mergeLabel(final Label deLabel, final int geomIndex) {
    final int loc = deLabel.getLocation(geomIndex, Position.RIGHT);
    // no information to be had from this label
    if (loc == Location.NONE.getIndex()) {
      return;
    }
    // if there is no current RHS value, set it
    if (this.label.getLocation(geomIndex) == Location.NONE.getIndex()) {
      this.label.setLocation(geomIndex, loc);
      return;
    }
  }

  abstract public void setEdgeRing(DirectedEdge de, EdgeRing er);

  public void setInResult() {
    DirectedEdge de = this.startDe;
    do {
      de.getEdge().setInResult(true);
      de = de.getNext();
    } while (de != this.startDe);
  }

  public void setShell(final EdgeRing shell) {
    this.shell = shell;
    if (shell != null) {
      shell.addHole(this);
    }
  }

  public Polygon toPolygon(final GeometryFactory geometryFactory) {
    final List<LinearRing> rings = new ArrayList<LinearRing>();
    rings.add(getLinearRing());

    for (final EdgeRing hole : this.holes) {
      rings.add(hole.getLinearRing());
    }
    final Polygon poly = geometryFactory.createPolygon(rings);
    return poly;
  }

}
