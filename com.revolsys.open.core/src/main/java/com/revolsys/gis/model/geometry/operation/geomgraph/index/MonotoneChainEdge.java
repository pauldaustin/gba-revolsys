package com.revolsys.gis.model.geometry.operation.geomgraph.index;

import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.geometry.impl.BoundingBox;
import com.revolsys.gis.model.geometry.operation.geomgraph.Edge;

/**
 * MonotoneChains are a way of partitioning the segments of an edge to allow for
 * fast searching of intersections. They have the following properties:
 * <ol>
 * <li>the segments within a monotone chain will never intersect each other
 * <li>the envelope of any contiguousubset of the segments in a monotone chain
 * isimply the envelope of the endpoints of the subset.
 * </ol>
 * Property 1 means that there is no need to test pairs of segments from within
 * the same monotone chain for intersection. Property 2 allows binary search to
 * be used to find the intersection points of two monotone chains. For many
 * types of real-world data, these properties eliminate a large number of
 * segment comparisons, producing substantial speed gains.
 *
 * @version 1.7
 */
public class MonotoneChainEdge {

  Edge e;

  CoordinatesList pts; // cache a reference to the coord array, for efficiency

  // the lists of start/end indexes of the monotone chains.
  // Includes the end point of the edge as a sentinel
  int[] startIndex;

  // these envelopes are created once and reused
  BoundingBox env1 = new BoundingBox();

  BoundingBox env2 = new BoundingBox();

  public MonotoneChainEdge(final Edge e) {
    this.e = e;
    this.pts = e.getCoordinates();
    final MonotoneChainIndexer mcb = new MonotoneChainIndexer();
    this.startIndex = mcb.getChainStartIndices(this.pts);
  }

  public void computeIntersects(final MonotoneChainEdge mce, final SegmentIntersector si) {
    for (int i = 0; i < this.startIndex.length - 1; i++) {
      for (int j = 0; j < mce.startIndex.length - 1; j++) {
        computeIntersectsForChain(i, mce, j, si);
      }
    }
  }

  private void computeIntersectsForChain(final int start0, final int end0,
    final MonotoneChainEdge mce, final int start1, final int end1, final SegmentIntersector ei) {
    final Coordinates p00 = this.pts.get(start0);
    final Coordinates p01 = this.pts.get(end0);
    final Coordinates p10 = mce.pts.get(start1);
    final Coordinates p11 = mce.pts.get(end1);
    // Debug.println("computeIntersectsForChain:" + p00 + p01 + p10 + p11);
    // terminating condition for the recursion
    if (end0 - start0 == 1 && end1 - start1 == 1) {
      ei.addIntersections(this.e, start0, mce.e, start1);
      return;
    }
    // nothing to do if the envelopes of these chains don't overlap
    this.env1 = new BoundingBox(null, p00.getX(), p00.getY(), p01.getX(), p01.getY());
    this.env2 = new BoundingBox(null, p10.getX(), p10.getY(), p11.getX(), p11.getY());
    if (!this.env1.intersects(this.env2)) {
      return;
    }

    // the chains overlap, so split each in half and iterate (binary search)
    final int mid0 = (start0 + end0) / 2;
    final int mid1 = (start1 + end1) / 2;

    // Assert: mid != start or end (since we checked above for end - start <= 1)
    // check terminating conditions before recursing
    if (start0 < mid0) {
      if (start1 < mid1) {
        computeIntersectsForChain(start0, mid0, mce, start1, mid1, ei);
      }
      if (mid1 < end1) {
        computeIntersectsForChain(start0, mid0, mce, mid1, end1, ei);
      }
    }
    if (mid0 < end0) {
      if (start1 < mid1) {
        computeIntersectsForChain(mid0, end0, mce, start1, mid1, ei);
      }
      if (mid1 < end1) {
        computeIntersectsForChain(mid0, end0, mce, mid1, end1, ei);
      }
    }
  }

  public void computeIntersectsForChain(final int chainIndex0, final MonotoneChainEdge mce,
    final int chainIndex1, final SegmentIntersector si) {
    computeIntersectsForChain(this.startIndex[chainIndex0], this.startIndex[chainIndex0 + 1], mce,
      mce.startIndex[chainIndex1], mce.startIndex[chainIndex1 + 1], si);
  }

  public CoordinatesList getCoordinates() {
    return this.pts;
  }

  public double getMaxX(final int chainIndex) {
    final double x1 = this.pts.getX(this.startIndex[chainIndex]);
    final double x2 = this.pts.getX(this.startIndex[chainIndex + 1]);
    return x1 > x2 ? x1 : x2;
  }

  public double getMinX(final int chainIndex) {
    final double x1 = this.pts.getX(this.startIndex[chainIndex]);
    final double x2 = this.pts.getX(this.startIndex[chainIndex + 1]);
    return x1 < x2 ? x1 : x2;
  }

  public int[] getStartIndexes() {
    return this.startIndex;
  }
}
