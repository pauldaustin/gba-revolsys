package com.revolsys.gis.algorithm.index.quadtree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.DoubleBits;

/**
 * A Key is a unique identifier for a node in a quadtree.
 * It contains a lower-left point and a level number. The level number
 * is the power of two for the size of the node envelope
 *
 * @version 1.7
 */
public class Key {

  public static int computeQuadLevel(final Envelope env) {
    final double dx = env.getWidth();
    final double dy = env.getHeight();
    final double dMax = dx > dy ? dx : dy;
    final int level = DoubleBits.exponent(dMax) + 1;
    return level;
  }

  // the fields which make up the key
  private final Coordinate pt = new Coordinate();

  private int level = 0;

  // auxiliary data which is derived from the key for use in computation
  private Envelope env = null;

  public Key(final Envelope itemEnv) {
    computeKey(itemEnv);
  }

  /**
   * return a square envelope containing the argument envelope,
   * whose extent is a power of two and which is based at a power of 2
   */
  public void computeKey(final Envelope itemEnv) {
    this.level = computeQuadLevel(itemEnv);
    this.env = new Envelope();
    computeKey(this.level, itemEnv);
    // MD - would be nice to have a non-iterative form of this algorithm
    while (!this.env.contains(itemEnv)) {
      this.level += 1;
      computeKey(this.level, itemEnv);
    }
  }

  private void computeKey(final int level, final Envelope itemEnv) {
    final double quadSize = DoubleBits.powerOf2(level);
    this.pt.x = Math.floor(itemEnv.getMinX() / quadSize) * quadSize;
    this.pt.y = Math.floor(itemEnv.getMinY() / quadSize) * quadSize;
    this.env.init(this.pt.x, this.pt.x + quadSize, this.pt.y, this.pt.y + quadSize);
  }

  public Coordinate getCentre() {
    return new Coordinate((this.env.getMinX() + this.env.getMaxX()) / 2,
      (this.env.getMinY() + this.env.getMaxY()) / 2);
  }

  public Envelope getEnvelope() {
    return this.env;
  }

  public int getLevel() {
    return this.level;
  }

  public Coordinate getPoint() {
    return this.pt;
  }
}
