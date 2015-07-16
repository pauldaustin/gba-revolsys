/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */
package com.revolsys.jts.operation.union;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryCollection;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.LineString;
import com.revolsys.jts.geom.Point;
import com.revolsys.jts.geom.Polygon;
import com.revolsys.jts.geom.Puntal;
import com.revolsys.jts.operation.linemerge.LineMerger;
import com.revolsys.jts.operation.overlay.OverlayOp;
import com.revolsys.jts.operation.overlay.snap.SnapIfNeededOverlayOp;

/**
 * Unions a <code>Collection</code> of {@link Geometry}s or a single Geometry
 * (which may be a {@link GeoometryCollection}) together.
 * By using this special-purpose operation over a collection of geometries
 * it is possible to take advantage of various optimizations to improve performance.
 * Heterogeneous {@link GeometryCollection}s are fully supported.
 * <p>
 * The result obeys the following contract:
 * <ul>
 * <li>Unioning a set of {@link Polygon}s has the effect of
 * merging the areas (i.e. the same effect as
 * iteratively unioning all individual polygons together).
 *
 * <li>Unioning a set of {@link LineString}s has the effect of <b>noding</b>
 * and <b>dissolving</b> the input linework.
 * In this context "fully noded" means that there will be
 * an endpoint or node in the result
 * for every endpoint or line segment crossing in the input.
 * "Dissolved" means that any duplicate (i.e. coincident) line segments or portions
 * of line segments will be reduced to a single line segment in the result.
 * This is consistent with the semantics of the
 * {@link Geometry#union(Geometry)} operation.
 * If <b>merged</b> linework is required, the {@link LineMerger} class can be used.
 *
 * <li>Unioning a set of {@link Point}s has the effect of merging
 * all identical points (producing a set with no duplicates).
 * </ul>
 *
 * <tt>UnaryUnion</tt> always operates on the individual components of MultiGeometries.
 * So it is possible to use it to "clean" invalid self-intersecting MultiPolygons
 * (although the polygon components must all still be individually valid.)
 *
 * @author mbdavis
 *
 */
public class UnaryUnionOp {
  /**
   * Computes the geometric union of a {@link Collection}
   * of {@link Geometry}s.
   *
   * @param geoms a collection of geometries
   * @return the union of the geometries,
   * or <code>null</code> if the input is empty
   */
  public static Geometry union(final Collection<? extends Geometry> geometries) {
    return union(geometries, null);
  }

  /**
   * Computes the geometric union of a {@link Collection}
   * of {@link Geometry}s.
   *
   * If no input geometries were provided but a {@link GeometryFactory} was provided,
   * an empty {@link GeometryCollection} is returned.
   *
   * @param geoms a collection of geometries
   * @param geometryFactory the geometry factory to use if the collection is empty
   * @return the union of the geometries,
   * or an empty GEOMETRYCOLLECTION
   */
  public static Geometry union(final Collection<? extends Geometry> geometries,
    GeometryFactory geometryFactory) {

    final List<Point> points = new ArrayList<>();
    final List<LineString> lines = new ArrayList<>();
    final List<Polygon> polygons = new ArrayList<>();
    for (final Geometry geometry : geometries) {
      if (geometryFactory == null) {
        geometryFactory = geometry.getGeometryFactory();
      }
      points.addAll(geometry.getGeometries(Point.class));
      lines.addAll(geometry.getGeometries(LineString.class));
      polygons.addAll(geometry.getGeometries(Polygon.class));
    }
    return union(geometryFactory, points, lines, polygons);
  }

  /**
   * Constructs a unary union operation for a {@link Geometry}
   * (which may be a {@link GeometryCollection}).
   *
   * @param geom a geometry to union
   * @return the union of the elements of the geometry
   * or an empty GEOMETRYCOLLECTION
   */
  public static Geometry union(final Geometry... geometries) {
    return union(Arrays.asList(geometries));
  }

  /**
   * Gets the union of the input geometries.
   * If no input geometries were provided but a {@link GeometryFactory} was provided,
   * an empty {@link GeometryCollection} is returned.
   * Otherwise, the return value is <code>null</code>.
   *
   * @return a Geometry containing the union,
   * or an empty GEOMETRYCOLLECTION if no geometries were provided in the input,
   * or <code>null</code> if no GeometryFactory was provided
   */
  private static Geometry union(final GeometryFactory geometryFactory, final List<Point> points,
    final List<LineString> lines, final List<Polygon> polygons) {
    if (geometryFactory == null) {
      return null;
    } else {

      /**
       * For points and lines, only a single union operation is
       * required, since the OGC model allowing self-intersecting
       * MultiPoint and MultiLineStrings.
       * This is not the case for polygons, so Cascaded Union is required.
       */
      Geometry unionPoints = null;
      if (points.size() > 0) {
        final Geometry pointal = geometryFactory.geometry(points);
        unionPoints = unionNoOpt(geometryFactory, pointal);
      }

      Geometry unionLines = null;
      if (lines.size() > 0) {
        final Geometry lineal = geometryFactory.geometry(lines);
        unionLines = unionNoOpt(geometryFactory, lineal);
      }

      Geometry unionPolygons = null;
      if (polygons.size() > 0) {
        unionPolygons = CascadedPolygonUnion.union(polygons);
      }

      /**
       * Performing two unions is somewhat inefficient,
       * but is mitigated by unioning lines and points first
       */
      final Geometry unionLA = unionWithNull(unionLines, unionPolygons);
      Geometry union = null;
      if (unionPoints == null) {
        union = unionLA;
      } else if (unionLA == null) {
        union = unionPoints;
      } else {
        union = PointGeometryUnion.union((Puntal)unionPoints, unionLA);
      }

      if (union == null) {
        return geometryFactory.geometryCollection();
      }

      return union;
    }
  }

  /**
   * Computes a unary union with no extra optimization,
   * and no short-circuiting.
   * Due to the way the overlay operations
   * are implemented, this is still efficient in the case of linear
   * and puntal geometries.
   * Uses robust version of overlay operation
   * to ensure identical behaviour to the <tt>union(Geometry)</tt> operation.
   *
   * @param geometry a geometry
   * @return the union of the input geometry
   */
  private static Geometry unionNoOpt(final GeometryFactory geometryFactory,
    final Geometry geometry) {
    final Geometry empty = geometryFactory.point();
    return SnapIfNeededOverlayOp.overlayOp(geometry, empty, OverlayOp.UNION);
  }

  /**
   * Computes the union of two geometries,
   * either of both of which may be null.
   *
   * @param geometry1 a Geometry
   * @param g1 a Geometry
   * @return the union of the input(s)
   * or null if both inputs are null
   */
  private static Geometry unionWithNull(final Geometry geometry1, final Geometry g1) {
    if (geometry1 == null && g1 == null) {
      return null;
    } else if (g1 == null) {
      return geometry1;
    } else if (geometry1 == null) {
      return g1;
    } else {
      return geometry1.union(g1);
    }
  }

}