/*
 * $URL:$
 * $Author:$
 * $Date:$
 * $Revision:$

 * Copyright 2004-2007 Revolution Systems Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.revolsys.gis.jts.filter;

import java.util.function.Predicate;

import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.jts.geom.BoundingBox;
import com.vividsolutions.jts.geom.LineString;

public class LineContainsWithinToleranceFilter implements Predicate<LineString> {
  private BoundingBox envelope;

  private boolean flip = false;

  private final CoordinatesList points;

  private double tolerance;

  public LineContainsWithinToleranceFilter(final LineString line) {
    this.points = CoordinatesListUtil.get(line);
    this.envelope = BoundingBox.getBoundingBox(line);
  }

  public LineContainsWithinToleranceFilter(final LineString line, final double tolerance) {
    this.points = CoordinatesListUtil.get(line);
    this.tolerance = tolerance;
    this.envelope = BoundingBox.getBoundingBox(line);
    this.envelope = this.envelope.expand(tolerance);
  }

  public LineContainsWithinToleranceFilter(final LineString line, final double tolerance,
    final boolean flip) {
    this(line, tolerance);
    this.flip = flip;
  }

  @Override
  public boolean test(final LineString line) {
    if (this.envelope.intersects(line.getEnvelopeInternal())) {
      final CoordinatesList points = CoordinatesListUtil.get(line);

      final boolean contains;
      if (this.flip) {
        contains = CoordinatesListUtil.containsWithinTolerance(points, this.points, this.tolerance);
      } else {
        contains = CoordinatesListUtil.containsWithinTolerance(this.points, points, this.tolerance);
      }
      if (contains) {
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

}
