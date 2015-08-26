package com.revolsys.gis.jts.filter;

import com.revolsys.gis.jts.LineStringUtil;
import java.util.function.Predicate;
import com.vividsolutions.jts.geom.LineString;

public class LineEqualIgnoreDirectionFilter implements Predicate<LineString> {
  private final int dimension;

  private final LineString line;

  public LineEqualIgnoreDirectionFilter(final LineString line, final int dimension) {
    this.line = line;
    this.dimension = dimension;
  }

  @Override
  public boolean test(final LineString line) {
    return LineStringUtil.equalsIgnoreDirection(line, this.line, this.dimension);
  }

}
