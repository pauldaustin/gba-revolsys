package com.revolsys.gis.model.geometry.operation.geomgraph;

import com.vividsolutions.jts.geom.Location;

/**
 * A <code>Label</code> indicates the topological relationship of a component of
 * a topology graph to a given <code>Geometry</code>. This class supports labels
 * for relationships to two <code>Geometry</code>s, which is sufficient for
 * algorithms for binary operations.
 * <P>
 * Topology graphs support the concept of labeling nodes and edges in the graph.
 * The label of a node or edge specifies its topological relationship to one or
 * more geometries. (In fact, since JTS operations have only two arguments
 * labels are required for only two geometries). A label for a node or edge has
 * one or two elements, depending on whether the node or edge occurs in one or
 * both of the input <code>Geometry</code>s. Elements contain attributes which
 * categorize the topological location of the node or edge relative to the
 * parent <code>Geometry</code>; that is, whether the node or edge is in the
 * interior, boundary or exterior of the <code>Geometry</code>. Attributes have
 * a value from the set <code>{Interior, Boundary, Exterior}</code>. In a node
 * each element has a single attribute <code>&lt;On&gt;</code>. For an edge each
 * element has a triplet of attributes <code>&lt;Left, On, Right&gt;</code>.
 * <P>
 * It is up to the client code to associate the 0 and 1
 * <code>TopologyLocation</code>s with specific geometries.
 *
 * @version 1.7
 */
public class Label {

  // converts a Label to a Line label (that is, one with no side Locations)
  public static Label toLineLabel(final Label label) {
    final Label lineLabel = new Label(Location.NONE);
    for (int i = 0; i < 2; i++) {
      lineLabel.setLocation(i, label.getLocation(i));
    }
    return lineLabel;
  }

  TopologyLocation elt[] = new TopologyLocation[2];

  /**
   * Construct a Label with a single location for both Geometries. Initialize
   * the locations to Null
   */
  public Label(final int onLoc) {
    this.elt[0] = new TopologyLocation(onLoc);
    this.elt[1] = new TopologyLocation(onLoc);
  }

  /**
   * Construct a Label with a single location for both Geometries. Initialize
   * the location for the Geometry index.
   */
  public Label(final int geomIndex, final int onLoc) {
    this.elt[0] = new TopologyLocation(Location.NONE);
    this.elt[1] = new TopologyLocation(Location.NONE);
    this.elt[geomIndex].setLocation(onLoc);
  }

  /**
   * Construct a Label with On, Left and Right locations for both Geometries.
   * Initialize the locations for both Geometries to the given values.
   */
  public Label(final int onLoc, final int leftLoc, final int rightLoc) {
    this.elt[0] = new TopologyLocation(onLoc, leftLoc, rightLoc);
    this.elt[1] = new TopologyLocation(onLoc, leftLoc, rightLoc);
  }

  /**
   * Construct a Label with On, Left and Right locations for both Geometries.
   * Initialize the locations for the given Geometry index.
   */
  public Label(final int geomIndex, final int onLoc, final int leftLoc, final int rightLoc) {
    this.elt[0] = new TopologyLocation(Location.NONE, Location.NONE, Location.NONE);
    this.elt[1] = new TopologyLocation(Location.NONE, Location.NONE, Location.NONE);
    this.elt[geomIndex].setLocations(onLoc, leftLoc, rightLoc);
  }

  /**
   * Construct a Label with the same values as the argument Label.
   */
  public Label(final Label lbl) {
    this.elt[0] = new TopologyLocation(lbl.elt[0]);
    this.elt[1] = new TopologyLocation(lbl.elt[1]);
  }

  public boolean allPositionsEqual(final int geomIndex, final int loc) {
    return this.elt[geomIndex].allPositionsEqual(loc);
  }

  public void flip() {
    this.elt[0].flip();
    this.elt[1].flip();
  }

  public int getGeometryCount() {
    int count = 0;
    if (!this.elt[0].isNull()) {
      count++;
    }
    if (!this.elt[1].isNull()) {
      count++;
    }
    return count;
  }

  public int getLocation(final int geomIndex) {
    return this.elt[geomIndex].get(Position.ON);
  }

  public int getLocation(final int geomIndex, final int posIndex) {
    return this.elt[geomIndex].get(posIndex);
  }

  public boolean isAnyNull(final int geomIndex) {
    return this.elt[geomIndex].isAnyNull();
  }

  public boolean isArea() {
    return this.elt[0].isArea() || this.elt[1].isArea();
  }

  public boolean isArea(final int geomIndex) {
    /*
     * Testing if (elt[0].getLocations().length != elt[1].getLocations().length)
     * { System.out.println(this); }
     */
    return this.elt[geomIndex].isArea();
  }

  public boolean isEqualOnSide(final Label lbl, final int side) {
    return this.elt[0].isEqualOnSide(lbl.elt[0], side)
      && this.elt[1].isEqualOnSide(lbl.elt[1], side);
  }

  public boolean isLine(final int geomIndex) {
    return this.elt[geomIndex].isLine();
  }

  public boolean isNull(final int geomIndex) {
    return this.elt[geomIndex].isNull();
  }

  /**
   * Merge this label with another one. Merging updates any null attributes of
   * this label with the attributes from lbl
   */
  public void merge(final Label lbl) {
    for (int i = 0; i < 2; i++) {
      if (this.elt[i] == null && lbl.elt[i] != null) {
        this.elt[i] = new TopologyLocation(lbl.elt[i]);
      } else {
        this.elt[i].merge(lbl.elt[i]);
      }
    }
  }

  public void setAllLocations(final int geomIndex, final int location) {
    this.elt[geomIndex].setAllLocations(location);
  }

  public void setAllLocationsIfNull(final int location) {
    setAllLocationsIfNull(0, location);
    setAllLocationsIfNull(1, location);
  }

  public void setAllLocationsIfNull(final int geomIndex, final int location) {
    this.elt[geomIndex].setAllLocationsIfNull(location);
  }

  public void setLocation(final int geomIndex, final int location) {
    this.elt[geomIndex].setLocation(Position.ON, location);
  }

  public void setLocation(final int geomIndex, final int posIndex, final int location) {
    this.elt[geomIndex].setLocation(posIndex, location);
  }

  /**
   * Converts one GeometryLocation to a Line location
   */
  public void toLine(final int geomIndex) {
    if (this.elt[geomIndex].isArea()) {
      this.elt[geomIndex] = new TopologyLocation(this.elt[geomIndex].location[0]);
    }
  }

  @Override
  public String toString() {
    final StringBuffer buf = new StringBuffer();
    if (this.elt[0] != null) {
      buf.append("A:");
      buf.append(this.elt[0].toString());
    }
    if (this.elt[1] != null) {
      buf.append(" B:");
      buf.append(this.elt[1].toString());
    }
    return buf.toString();
  }
}
