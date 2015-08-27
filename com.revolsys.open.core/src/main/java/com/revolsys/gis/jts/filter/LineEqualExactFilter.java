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

import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import java.util.function.Predicate;
import com.vividsolutions.jts.geom.LineString;

public class LineEqualExactFilter implements Predicate<LineString> {
  int numAxis = -1;

  private final CoordinatesList points;

  public LineEqualExactFilter(final LineString line) {
    this.points = CoordinatesListUtil.get(line);
  }

  public LineEqualExactFilter(final LineString line, final int numAxis) {
    this.points = CoordinatesListUtil.get(line);
    this.numAxis = numAxis;
  }

  @Override
  public boolean test(final LineString line) {
    final CoordinatesList points = CoordinatesListUtil.get(line);

    final boolean equal;
    if (this.numAxis >= 2) {
      equal = this.points.equals(points, this.numAxis);
    } else {
      equal = this.points.equals(points);
    }
    if (equal) {
      return true;
    } else {
      return false;
    }
  }

}
