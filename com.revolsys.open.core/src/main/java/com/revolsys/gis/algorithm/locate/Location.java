package com.revolsys.gis.algorithm.locate;

public enum Location {
  BOUNDARY(1), EXTERIOR(2), INTERIOR(0), NONE(-1);

  private int index;

  private Location(final int index) {
    this.index = index;
  }

  public int getIndex() {
    return this.index;
  }
}
