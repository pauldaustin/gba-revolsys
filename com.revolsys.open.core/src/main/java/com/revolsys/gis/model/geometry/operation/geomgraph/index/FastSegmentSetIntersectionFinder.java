package com.revolsys.gis.model.geometry.operation.geomgraph.index;

import java.util.Collection;
import java.util.List;

import com.revolsys.gis.model.geometry.Geometry;
import com.revolsys.gis.model.geometry.operation.chain.MCIndexSegmentSetMutualIntersector;
import com.revolsys.gis.model.geometry.operation.chain.SegmentIntersectionDetector;
import com.revolsys.gis.model.geometry.operation.chain.SegmentSetMutualIntersector;
import com.revolsys.gis.model.geometry.operation.chain.SegmentString;
import com.revolsys.gis.model.geometry.operation.chain.SegmentStringUtil;

/**
 * Finds if two sets of {@link SegmentString}s intersect. Uses indexing for fast
 * performance and to optimize repeated tests against a target set of lines.
 * Short-circuited to return as soon an intersection is found.
 *
 * @version 1.7
 */
public class FastSegmentSetIntersectionFinder {
  private static final String KEY = FastSegmentSetIntersectionFinder.class.getName();

  private static LineIntersector li = new RobustLineIntersector();

  public static FastSegmentSetIntersectionFinder get(final Geometry geometry) {
    FastSegmentSetIntersectionFinder instance = geometry.getProperty(KEY);
    if (instance == null) {
      final List<SegmentString> segments = SegmentStringUtil.extractSegmentStrings(geometry);
      instance = new FastSegmentSetIntersectionFinder(segments);
      geometry.setPropertySoft(KEY, instance);
    }
    return instance;
  }

  // for testing purposes
  // private SimpleSegmentSetMutualIntersector mci;

  private SegmentSetMutualIntersector segSetMutInt;

  public FastSegmentSetIntersectionFinder(final Collection<SegmentString> baseSegStrings) {
    init(baseSegStrings);
  }

  /**
   * Gets the segment set intersector used by this class. This allows other uses
   * of the same underlying indexed structure.
   *
   * @return the segment set intersector used
   */
  public SegmentSetMutualIntersector getSegmentSetIntersector() {
    return this.segSetMutInt;
  }

  private void init(final Collection<SegmentString> baseSegStrings) {
    this.segSetMutInt = new MCIndexSegmentSetMutualIntersector();
    // segSetMutInt = new MCIndexIntersectionSegmentSetMutualIntersector();

    // mci = new SimpleSegmentSetMutualIntersector();
    this.segSetMutInt.setBaseSegments(baseSegStrings);
  }

  public boolean intersects(final Collection<SegmentString> segStrings) {
    final SegmentIntersectionDetector intFinder = new SegmentIntersectionDetector(li);
    this.segSetMutInt.setSegmentIntersector(intFinder);

    this.segSetMutInt.process(segStrings);
    return intFinder.hasIntersection();
  }

  public boolean intersects(final Collection<SegmentString> segStrings,
    final SegmentIntersectionDetector intDetector) {
    this.segSetMutInt.setSegmentIntersector(intDetector);

    this.segSetMutInt.process(segStrings);
    return intDetector.hasIntersection();
  }
}
