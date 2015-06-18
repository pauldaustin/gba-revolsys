package com.revolsys.data.record.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.revolsys.data.equals.DataObjectEquals;
import com.revolsys.data.equals.EqualsInstance;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.data.model.DataObjectUtil;
import com.revolsys.gis.graph.Edge;
import com.revolsys.gis.jts.LineStringUtil;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class DirectionalAttributes extends AbstractRecordDefinitionProperty {
  public static final String PROPERTY_NAME = DirectionalAttributes.class.getName()
    + ".propertyName";

  private static final Logger LOG = LoggerFactory.getLogger(DirectionalAttributes.class);

  public static boolean canMergeObjects(final Coordinates point, final Record object1,
    final Record object2) {
    final Set<String> excludes = Collections.emptySet();
    final DirectionalAttributes property = DirectionalAttributes.getProperty(object1);
    return property.canMerge(point, object1, object2, excludes);
  }

  public static boolean canMergeObjects(final Coordinates point, final Record object1,
    final Record object2, final Set<String> equalExcludeAttributes) {
    final DirectionalAttributes property = DirectionalAttributes.getProperty(object1);
    return property.canMerge(point, object1, object2, equalExcludeAttributes);
  }

  public static void edgeSplitAttributes(final LineString line, final Coordinates point,
    final List<Edge<Record>> edges) {
    if (!edges.isEmpty()) {
      final Edge<Record> firstEdge = edges.get(0);
      final Record object = firstEdge.getObject();
      final DirectionalAttributes property = DirectionalAttributes.getProperty(object);
      property.setEdgeSplitAttributes(line, point, edges);
    }
  }

  public static boolean equalsObjects(final Record object1, final Record object2) {
    final Set<String> excludes = Collections.emptySet();
    return equalsObjects(object1, object2, excludes);
  }

  public static boolean equalsObjects(final Record object1, final Record object2,
    final Collection<String> equalExcludeAttributes) {
    final DirectionalAttributes property = DirectionalAttributes.getProperty(object1);
    return property.equals(object1, object2, equalExcludeAttributes);
  }

  public static Set<String> getCantMergeAttributesObjects(final Coordinates point,
    final Record object1, final Record object2, final Set<String> equalExcludeAttributes) {
    final DirectionalAttributes property = DirectionalAttributes.getProperty(object1);
    return property.getCantMergeAttributes(point, object1, object2, equalExcludeAttributes);
  }

  public static DirectionalAttributes getProperty(final Record object) {
    final RecordDefinition metaData = object.getRecordDefinition();
    return getProperty(metaData);
  }

  public static DirectionalAttributes getProperty(final RecordDefinition metaData) {
    DirectionalAttributes property = metaData.getProperty(PROPERTY_NAME);
    if (property == null) {
      property = new DirectionalAttributes();
      property.setRecordDefinition(metaData);
    }
    return property;
  }

  public static Record getReverseObject(final Record object) {
    final DirectionalAttributes property = getProperty(object);
    final Record reverse = property.getReverse(object);
    return reverse;
  }

  public static boolean hasProperty(final Record object) {
    final RecordDefinition metaData = object.getRecordDefinition();
    return metaData.getProperty(PROPERTY_NAME) != null;
  }

  public static Record merge(final Coordinates point, final Record object1, final Record object2) {
    final DirectionalAttributes property = DirectionalAttributes.getProperty(object1);
    return property.getMergedObject(point, object1, object2);
  }

  public static Record merge(final Record object1, final Record object2) {
    final DirectionalAttributes property = DirectionalAttributes.getProperty(object1);
    return property.getMergedObject(object1, object2);
  }

  public static Record mergeLongest(final Coordinates point, final Record object1,
    final Record object2) {
    final DirectionalAttributes property = DirectionalAttributes.getProperty(object1);
    return property.getMergedObjectReverseLongest(point, object1, object2);
  }

  public static Record mergeLongest(final Record object1, final Record object2) {
    final DirectionalAttributes property = DirectionalAttributes.getProperty(object1);
    return property.getMergedObjectReverseLongest(object1, object2);
  }

  public static void reverse(final Record object) {
    final DirectionalAttributes property = getProperty(object);
    property.reverseAttributesAndGeometry(object);
  }

  private final Map<String, String> endAttributeNamePairs = new HashMap<String, String>();

  private final Map<String, String> sideAttributeNamePairs = new HashMap<String, String>();

  private final Map<String, String> reverseAttributeNameMap = new HashMap<String, String>();

  private final Set<String> startAttributeNames = new HashSet<String>();

  private final Set<String> sideAttributeNames = new HashSet<String>();

  private final Set<String> endAttributeNames = new HashSet<String>();

  private final Map<String, Map<Object, Object>> directionalAttributeValues = new HashMap<String, Map<Object, Object>>();

  private final List<List<String>> endAndSideAttributeNamePairs = new ArrayList<List<String>>();

  private final List<List<String>> endTurnAttributeNamePairs = new ArrayList<List<String>>();

  public DirectionalAttributes() {
  }

  /**
   * Add a mapping from the fromAttributeName to the toAttributeName and an
   * inverse mapping to the namePairs map.
   *
   * @param namePairs The name pair mapping.
   * @param fromAttributeName The from attribute name.
   * @param toAttributeName The to attribute name.
   */
  private void addAttributeNamePair(final Map<String, String> namePairs,
    final String fromAttributeName, final String toAttributeName) {
    final String fromPair = namePairs.get(fromAttributeName);
    if (fromPair == null) {
      final String toPair = namePairs.get(toAttributeName);
      if (toPair == null) {
        namePairs.put(fromAttributeName, toAttributeName);
        namePairs.put(toAttributeName, fromAttributeName);
      } else if (toPair.equals(fromAttributeName)) {
        throw new IllegalArgumentException("Cannot override mapping " + toAttributeName + "="
          + toPair + " to " + fromAttributeName);
      }
    } else if (fromPair.equals(toAttributeName)) {
      throw new IllegalArgumentException("Cannot override mapping " + fromAttributeName + "="
        + fromPair + " to " + toAttributeName);
    }
  }

  public void addDirectionalAttributeValues(final String attributeName,
    final Map<? extends Object, ? extends Object> values) {
    final Map<Object, Object> newValues = new LinkedHashMap<Object, Object>();
    for (final Entry<? extends Object, ? extends Object> entry : values.entrySet()) {
      final Object value1 = entry.getKey();
      final Object value2 = entry.getValue();
      addValue(newValues, value1, value2);
      addValue(newValues, value2, value1);
    }
    this.directionalAttributeValues.put(attributeName, newValues);
  }

  public void addEndAndSideAttributePairs(final String startLeftAttributeName,
    final String startRightAttributeName, final String endLeftAttributeName,
    final String endRightAttributeName) {
    this.endAndSideAttributeNamePairs.add(Arrays.asList(startLeftAttributeName,
      startRightAttributeName, endLeftAttributeName, endRightAttributeName));
    addEndAttributePairInternal(startLeftAttributeName, endLeftAttributeName);
    addEndAttributePairInternal(startRightAttributeName, endRightAttributeName);
    addAttributeNamePair(this.reverseAttributeNameMap, startLeftAttributeName,
      endRightAttributeName);
    addAttributeNamePair(this.reverseAttributeNameMap, endLeftAttributeName,
      startRightAttributeName);
  }

  public void addEndAttributePair(final String startAttributeName, final String endAttributeName) {
    addEndAttributePairInternal(startAttributeName, endAttributeName);
    addAttributeNamePair(this.reverseAttributeNameMap, startAttributeName, endAttributeName);
  }

  private void addEndAttributePairInternal(final String startAttributeName,
    final String endAttributeName) {
    addAttributeNamePair(this.endAttributeNamePairs, startAttributeName, endAttributeName);
    this.startAttributeNames.add(startAttributeName);
    this.endAttributeNames.add(endAttributeName);
  }

  public void addEndTurnAttributePairs(final String startLeftAttributeName,
    final String startRightAttributeName, final String endLeftAttributeName,
    final String endRightAttributeName) {
    this.endTurnAttributeNamePairs.add(Arrays.asList(startLeftAttributeName,
      startRightAttributeName, endLeftAttributeName, endRightAttributeName));
    addEndAttributePairInternal(startLeftAttributeName, endLeftAttributeName);
    addEndAttributePairInternal(startRightAttributeName, endRightAttributeName);
    addAttributeNamePair(this.reverseAttributeNameMap, startLeftAttributeName, endLeftAttributeName);
    addAttributeNamePair(this.reverseAttributeNameMap, startRightAttributeName,
      endRightAttributeName);
  }

  public void addSideAttributePair(final String leftAttributeName, final String rightAttributeName) {
    addAttributeNamePair(this.sideAttributeNamePairs, leftAttributeName, rightAttributeName);
    this.sideAttributeNames.add(leftAttributeName);
    this.sideAttributeNames.add(rightAttributeName);
    addAttributeNamePair(this.reverseAttributeNameMap, leftAttributeName, rightAttributeName);
  }

  protected void addValue(final Map<Object, Object> map, final Object key, final Object value) {
    final Object oldValue = map.get(key);
    if (oldValue != null && !oldValue.equals(value)) {
      throw new IllegalArgumentException("Cannot override mapping " + key + "=" + oldValue
        + " with " + value);
    }
    map.put(key, value);
  }

  public boolean canMerge(final Coordinates point, final Record object1, final Record object2,
    final Collection<String> equalExcludeAttributes) {
    final boolean[] forwardsIndicators = getForwardsIndicators(point, object1, object2);

    if (forwardsIndicators != null) {
      final RecordDefinition metaData = getRecordDefinition();
      final EqualIgnoreAttributes equalIgnore = EqualIgnoreAttributes.getProperty(metaData);
      for (final String attributeName : metaData.getFieldNames()) {
        if (!DataObjectEquals.isAttributeIgnored(metaData, equalExcludeAttributes, attributeName)
          && !equalIgnore.isAttributeIgnored(attributeName)) {
          if (!canMerge(attributeName, point, object1, object2, equalExcludeAttributes,
            forwardsIndicators)) {
            return false;
          }
        }
      }
      return true;
    } else {
      return false;
    }
  }

  public boolean canMerge(final String attributeName, final Coordinates point,
    final Record object1, final Record object2, final Collection<String> equalExcludeAttributes,
    final boolean[] forwardsIndicators) {
    final RecordDefinition metaData = getRecordDefinition();
    if (attributeName.equals(metaData.getGeometryFieldName())) {
      final LineString line1 = object1.getGeometryValue();
      final LineString line2 = object2.getGeometryValue();
      return !line1.equals(line2);
    }
    if (forwardsIndicators == null) {
      return false;
    } else {
      final boolean line1Forwards = forwardsIndicators[0];
      final boolean line2Forwards = forwardsIndicators[1];
      if (hasDirectionalAttributeValues(attributeName)) {
        if (line1Forwards != line2Forwards) {
          final Object value1 = object1.getValue(attributeName);
          final Object value2 = getDirectionalAttributeValue(object2, attributeName);
          if (EqualsInstance.INSTANCE.equals(value1, value2, equalExcludeAttributes)) {
            return true;
          } else {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Different values (" + attributeName + "=" + value1 + ") != ("
                + attributeName + " = " + value2 + ")");
              LOG.debug(object1.toString());
              LOG.debug(object2.toString());
            }
            return false;
          }
        }
      } else if (isStartAttribute(attributeName)) {
        return canMergeStartAttribute(attributeName, object1, line1Forwards, object2,
          line2Forwards, equalExcludeAttributes);
      } else if (isEndAttribute(attributeName)) {
        return canMergeEndAttribute(attributeName, object1, line1Forwards, object2, line2Forwards,
          equalExcludeAttributes);
      } else if (isSideAttribute(attributeName)) {
        if (line1Forwards != line2Forwards) {
          final String oppositeAttributeName = getSideAttributePair(attributeName);
          if (oppositeAttributeName == null) { // only check the pair once
            return true;
          } else {
            return equals(object1, attributeName, object2, oppositeAttributeName,
              equalExcludeAttributes);
          }
        }
      }
      return equals(object1, attributeName, object2, attributeName, equalExcludeAttributes);
    }
  }

  protected boolean canMergeEndAttribute(final String endAttributeName, final Record object1,
    final boolean line1Forwards, final Record object2, final boolean line2Forwards,
    final Collection<String> equalExcludeAttributes) {
    final String startAttributeName = this.endAttributeNamePairs.get(endAttributeName);
    if (line1Forwards) {
      if (line2Forwards) {
        // -->*--> true true
        return isNull(object1, endAttributeName, object2, startAttributeName,
          equalExcludeAttributes);
      } else {
        // -->*<-- true false
        return isNull(object1, endAttributeName, object2, endAttributeName, equalExcludeAttributes);
      }
    } else {
      if (line2Forwards) {
        // <--*--> false true
        return true;
      } else {
        // <--*<-- false false
        return isNull(object1, startAttributeName, object2, endAttributeName,
          equalExcludeAttributes);
      }
    }
  }

  protected boolean canMergeStartAttribute(final String startAttributeName, final Record object1,
    final boolean line1Forwards, final Record object2, final boolean line2Forwards,
    final Collection<String> equalExcludeAttributes) {
    final String endAttributeName = this.endAttributeNamePairs.get(startAttributeName);
    if (line1Forwards) {
      if (line2Forwards) {
        // -->*--> true true
        return isNull(object1, endAttributeName, object2, startAttributeName,
          equalExcludeAttributes);
      } else {
        // -->*<-- true false
        return true;
      }
    } else {
      if (line2Forwards) {
        // <--*--> false true
        return isNull(object1, startAttributeName, object2, startAttributeName,
          equalExcludeAttributes);
      } else {
        // <--*<-- false false
        return isNull(object1, startAttributeName, object2, endAttributeName,
          equalExcludeAttributes);
      }
    }
  }

  public void clearEndAttributes(final Record object) {
    for (final String attributeName : this.endAttributeNames) {
      object.setValue(attributeName, null);
    }
  }

  public void clearStartAttributes(final Record object) {
    for (final String attributeName : this.startAttributeNames) {
      object.setValue(attributeName, null);
    }
  }

  public boolean equals(final Record object1, final Record object2,
    final Collection<String> equalExcludeAttributes) {
    final RecordDefinition metaData = getRecordDefinition();
    final EqualIgnoreAttributes equalIgnore = EqualIgnoreAttributes.getProperty(metaData);
    for (final String attributeName : metaData.getFieldNames()) {
      if (!equalExcludeAttributes.contains(attributeName)
        && !equalIgnore.isAttributeIgnored(attributeName)) {
        if (!equals(attributeName, object1, object2, equalExcludeAttributes)) {
          return false;
        }
      }
    }
    return true;
  }

  protected boolean equals(final Record object1, final String name1, final Record object2,
    final String name2, final Collection<String> equalExcludeAttributes) {
    final Object value1 = object1.getValue(name1);
    final Object value2 = object2.getValue(name2);
    if (EqualsInstance.INSTANCE.equals(value1, value2, equalExcludeAttributes)) {
      return true;
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Different values (" + name1 + "=" + value1 + ") != (" + name2 + " = " + value2
          + ")");
        LOG.debug(object1.toString());
        LOG.debug(object2.toString());
      }
      return false;
    }
  }

  protected boolean equals(final String attributeName, final Record object1, final Record object2,
    final Collection<String> equalExcludeAttributes) {
    final LineString line1 = object1.getGeometryValue();
    final LineString line2 = object2.getGeometryValue();
    final RecordDefinition metaData = getRecordDefinition();
    if (attributeName.equals(metaData.getGeometryFieldName())) {
      return line1.equals(line2);
    }
    final CoordinatesList points1 = CoordinatesListUtil.get(line1);
    final CoordinatesList points2 = CoordinatesListUtil.get(line2);

    boolean reverseEquals;
    if (points1.equal(0, points2, 0)) {
      if (points1.equal(0, points1, points1.size() - 1)) {
        // TODO handle loops
        throw new IllegalArgumentException("Cannot handle loops");
      }
      reverseEquals = false;
    } else {
      reverseEquals = true;
    }
    if (reverseEquals) {
      return equalsReverse(attributeName, object1, object2, equalExcludeAttributes);
    } else {
      return equals(object1, attributeName, object2, attributeName, equalExcludeAttributes);
    }
  }

  private boolean equalsReverse(final String attributeName, final Record object1,
    final Record object2, final Collection<String> equalExcludeAttributes) {
    if (hasDirectionalAttributeValues(attributeName)) {
      final Object value1 = object1.getValue(attributeName);
      final Object value2 = getDirectionalAttributeValue(object2, attributeName);
      if (EqualsInstance.INSTANCE.equals(value1, value2, equalExcludeAttributes)) {
        return true;
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Different values (" + attributeName + "=" + value1 + ") != (" + attributeName
            + " = " + value2 + ")");
          LOG.debug(object1.toString());
          LOG.debug(object2.toString());
        }
        return false;
      }
    } else {
      final String reverseAttributeName = getReverseAttributeName(attributeName);
      if (reverseAttributeName == null) {
        return equals(object1, attributeName, object2, attributeName, equalExcludeAttributes);
      } else {
        return equals(object1, attributeName, object2, reverseAttributeName, equalExcludeAttributes);
      }
    }
  }

  public Set<String> getCantMergeAttributes(final Coordinates point, final Record object1,
    final Record object2, final Collection<String> equalExcludeAttributes) {
    final RecordDefinition metaData = getRecordDefinition();
    final boolean[] forwardsIndicators = getForwardsIndicators(point, object1, object2);
    if (forwardsIndicators != null) {
      final Set<String> attributeNames = new LinkedHashSet<String>();
      final EqualIgnoreAttributes equalIgnore = EqualIgnoreAttributes.getProperty(metaData);
      for (final String attributeName : metaData.getFieldNames()) {
        if (!equalExcludeAttributes.contains(attributeName)
          && !equalIgnore.isAttributeIgnored(attributeName)) {
          if (!canMerge(attributeName, point, object1, object2, equalExcludeAttributes,
            forwardsIndicators)) {
            attributeNames.add(attributeName);
          }
        }
      }
      return attributeNames;
    } else {
      final String geometryAttributeName = metaData.getGeometryFieldName();
      return Collections.singleton(geometryAttributeName);
    }
  }

  protected Object getDirectionalAttributeValue(final Map<String, ? extends Object> object,
    final String attributeName) {
    final Object value = object.get(attributeName);

    final Map<Object, Object> valueMap = this.directionalAttributeValues.get(attributeName);
    if (valueMap != null) {
      if (valueMap.containsKey(value)) {
        final Object directionalValue = valueMap.get(value);
        return directionalValue;
      }
    }
    return value;
  }

  public Map<String, Map<Object, Object>> getDirectionalAttributeValues() {
    return this.directionalAttributeValues;
  }

  public List<List<String>> getEndAndSideAttributeNamePairs() {
    return this.endAndSideAttributeNamePairs;
  }

  public Map<String, String> getEndAttributeNamePairs() {
    return this.endAttributeNamePairs;
  }

  public Set<String> getEndAttributeNames() {
    return this.endAttributeNames;
  }

  public List<List<String>> getEndTurnAttributeNamePairs() {
    return this.endTurnAttributeNamePairs;
  }

  protected boolean[] getForwardsIndicators(final Coordinates point, final Record object1,
    final Record object2) {
    final LineString line1 = object1.getGeometryValue();
    final LineString line2 = object2.getGeometryValue();

    final CoordinatesList points1 = CoordinatesListUtil.get(line1);
    final CoordinatesList points2 = CoordinatesListUtil.get(line2);

    final boolean[] forwards = new boolean[2];
    final int lastPointIndex1 = points1.size() - 1;
    if (points1.equal(0, points2, 0) && points1.equal(0, point, 2)) {
      // <--*--> false true
      forwards[0] = false;
      forwards[1] = true;
    } else if (points1.equal(points1.size() - 1, points2, points2.size() - 1)
      && points1.equal(lastPointIndex1, point, 2)) {
      // -->*<-- true false
      forwards[0] = true;
      forwards[1] = false;
    } else if (points1.equal(points1.size() - 1, points2, 0)
      && points1.equal(lastPointIndex1, point, 2)) {
      // -->*--> true true
      forwards[0] = true;
      forwards[1] = true;
    } else if (points1.equal(0, points2, points2.size() - 1) && points1.equal(0, point, 2)) {
      // <--*<-- false false
      forwards[0] = false;
      forwards[1] = false;
    } else {
      return null;
    }
    return forwards;
  }

  public Map<String, Object> getMergedMap(final Coordinates point, final Record object1,
    Record object2) {
    final LineString line1 = object1.getGeometryValue();
    LineString line2 = object2.getGeometryValue();
    final CoordinatesList points1 = CoordinatesListUtil.get(line1);
    final CoordinatesList points2 = CoordinatesListUtil.get(line2);

    Record startObject;
    Record endObject;

    LineString newLine;
    final int lastPoint1 = points1.size() - 1;
    final int lastPoint2 = points2.size() - 1;

    if (points1.equal(0, points2, 0) && points1.equal2d(0, point)) {
      object2 = getReverse(object2);
      line2 = object2.getGeometryValue();
      startObject = object2;
      endObject = object1;
      newLine = LineStringUtil.merge(point, line1, line2);
    } else if (points1.equal(lastPoint1, points2, lastPoint2) && points1.equal2d(lastPoint1, point)) {
      object2 = getReverse(object2);
      line2 = object2.getGeometryValue();
      startObject = object1;
      endObject = object2;
      newLine = LineStringUtil.merge(point, line1, line2);
    } else if (points1.equal(lastPoint1, points2, 0) && points1.equal2d(lastPoint1, point)) {
      startObject = object1;
      endObject = object2;
      newLine = LineStringUtil.merge(point, line1, line2);
    } else if (points1.equal(0, points2, lastPoint2) && points1.equal2d(0, point)) {
      startObject = object2;
      endObject = object1;
      newLine = LineStringUtil.merge(point, line2, line1);
    } else {
      throw new IllegalArgumentException("Lines for objects don't touch");
    }

    final Map<String, Object> newValues = new LinkedHashMap<String, Object>(object1);
    setStartAttributes(startObject, newValues);
    setEndAttributes(endObject, newValues);
    final RecordDefinition metaData = object1.getRecordDefinition();
    final String geometryAttributeName = metaData.getGeometryFieldName();
    newValues.put(geometryAttributeName, newLine);
    return newValues;
  }

  /**
   * Get a new object that is the result of merging the two objects. The
   * attributes will be taken from the object with the longest length. If one
   * line needs to be reversed then the second object will be reversed.
   *
   * @param object1
   * @param object2
   * @return
   */
  public Record getMergedObject(final Coordinates point, final Record object1, Record object2) {
    final LineString line1 = object1.getGeometryValue();
    LineString line2 = object2.getGeometryValue();
    final CoordinatesList points1 = CoordinatesListUtil.get(line1);
    final CoordinatesList points2 = CoordinatesListUtil.get(line2);

    Record startObject;
    Record endObject;

    final boolean line1Longer = line1.getLength() > line2.getLength();
    LineString newLine;
    final int lastPoint1 = points1.size() - 1;
    final int lastPoint2 = points2.size() - 1;

    if (points1.equal(0, points2, 0) && points1.equal2d(0, point)) {
      object2 = getReverse(object2);
      line2 = object2.getGeometryValue();
      startObject = object2;
      endObject = object1;
      newLine = LineStringUtil.merge(point, line1, line2);
    } else if (points1.equal(lastPoint1, points2, lastPoint2) && points1.equal2d(lastPoint1, point)) {
      object2 = getReverse(object2);
      line2 = object2.getGeometryValue();
      startObject = object1;
      endObject = object2;
      newLine = LineStringUtil.merge(point, line1, line2);
    } else if (points1.equal(lastPoint1, points2, 0) && points1.equal2d(lastPoint1, point)) {
      startObject = object1;
      endObject = object2;
      newLine = LineStringUtil.merge(point, line1, line2);
    } else if (points1.equal(0, points2, lastPoint2) && points1.equal2d(0, point)) {
      startObject = object2;
      endObject = object1;
      newLine = LineStringUtil.merge(point, line2, line1);
    } else {
      throw new IllegalArgumentException("Lines for objects don't touch");
    }

    Record newObject;
    if (line1Longer) {
      newObject = DataObjectUtil.copy(object1, newLine);
    } else {
      newObject = DataObjectUtil.copy(object2, newLine);
    }
    setStartAttributes(startObject, newObject);
    setEndAttributes(endObject, newObject);
    LengthFieldName.setObjectLength(newObject);
    return newObject;
  }

  /**
   * Get a new object that is the result of merging the two objects. The
   * attributes will be taken from the object with the longest length. If one
   * line needs to be reversed then the second object will be reversed.
   *
   * @param object1
   * @param object2
   * @return
   */
  public Record getMergedObject(final Record object1, Record object2) {
    final LineString line1 = object1.getGeometryValue();
    LineString line2 = object2.getGeometryValue();
    final CoordinatesList points1 = CoordinatesListUtil.get(line1);
    final CoordinatesList points2 = CoordinatesListUtil.get(line2);

    Record startObject;
    Record endObject;

    final boolean line1Longer = line1.getLength() > line2.getLength();
    LineString newLine;

    if (points1.equal(0, points2, 0)) {
      object2 = getReverse(object2);
      line2 = object2.getGeometryValue();
      startObject = object2;
      endObject = object1;
      newLine = LineStringUtil.merge(line1, line2);
    } else if (points1.equal(points1.size() - 1, points2, points2.size() - 1)) {
      object2 = getReverse(object2);
      line2 = object2.getGeometryValue();
      startObject = object1;
      endObject = object2;
      newLine = LineStringUtil.merge(line1, line2);
    } else if (points1.equal(points1.size() - 1, points2, 0)) {
      startObject = object1;
      endObject = object2;
      newLine = LineStringUtil.merge(line1, line2);
    } else if (points1.equal(0, points2, points2.size() - 1)) {
      startObject = object2;
      endObject = object1;
      newLine = LineStringUtil.merge(line2, line1);
    } else {
      throw new IllegalArgumentException("Lines for objects don't touch");
    }

    Record newObject;
    if (line1Longer) {
      newObject = DataObjectUtil.copy(object1, newLine);
    } else {
      newObject = DataObjectUtil.copy(object2, newLine);
    }
    setStartAttributes(startObject, newObject);
    setEndAttributes(endObject, newObject);
    LengthFieldName.setObjectLength(newObject);
    return newObject;
  }

  public Record getMergedObjectReverseLongest(final Coordinates point, final Record object1,
    final Record object2) {
    final LineString line1 = object1.getGeometryValue();
    final LineString line2 = object2.getGeometryValue();
    if (line1.getLength() >= line2.getLength()) {
      return getMergedObject(point, object1, object2);
    } else {
      return getMergedObject(point, object2, object1);
    }
  }

  /**
   * Get a new object that is the result of merging the two objects. The
   * attributes will be taken from the object with the longest length. If one
   * line needs to be reversed then the longest will be reversed.
   *
   * @param object1
   * @param object2
   * @return
   */
  public Record getMergedObjectReverseLongest(final Record object1, final Record object2) {
    final LineString line1 = object1.getGeometryValue();
    final LineString line2 = object2.getGeometryValue();
    if (line1.getLength() >= line2.getLength()) {
      return getMergedObject(object1, object2);
    } else {
      return getMergedObject(object2, object1);
    }
  }

  @Override
  public String getPropertyName() {
    return PROPERTY_NAME;
  }

  public Record getReverse(final Record object) {
    final Record reverse = object.clone();
    reverseAttributesAndGeometry(reverse);
    return reverse;
  }

  public String getReverseAttributeName(final String attributeName) {
    return this.reverseAttributeNameMap.get(attributeName);
  }

  public Map<String, Object> getReverseAttributes(final Map<String, Object> object) {
    final Map<String, Object> reverse = new LinkedHashMap<String, Object>(object);
    for (final Entry<String, String> pair : this.reverseAttributeNameMap.entrySet()) {
      final String fromAttributeName = pair.getKey();
      final String toAttributeName = pair.getValue();
      final Object toValue = object.get(toAttributeName);
      reverse.put(fromAttributeName, toValue);
    }
    for (final String attributeName : this.directionalAttributeValues.keySet()) {
      final Object value = getDirectionalAttributeValue(object, attributeName);
      reverse.put(attributeName, value);
    }
    return reverse;
  }

  public Map<String, Object> getReverseAttributesAndGeometry(final Map<String, Object> object) {
    final Map<String, Object> reverse = getReverseAttributes(object);
    final String geometryAttributeName = getRecordDefinition().getGeometryFieldName();
    if (geometryAttributeName != null) {
      final Geometry geometry = getReverseLine(object);
      reverse.put(geometryAttributeName, geometry);
    }
    return reverse;
  }

  public Map<String, Object> getReverseGeometry(final Map<String, Object> object) {
    final Map<String, Object> reverse = new LinkedHashMap<String, Object>(object);
    final String geometryAttributeName = getRecordDefinition().getGeometryFieldName();
    if (geometryAttributeName != null) {
      final Geometry geometry = getReverseLine(object);
      reverse.put(geometryAttributeName, geometry);
    }
    return reverse;
  }

  protected Geometry getReverseLine(final Map<String, Object> object) {
    final String geometryAttributeName = getRecordDefinition().getGeometryFieldName();
    final LineString line = (LineString)object.get(geometryAttributeName);
    if (line == null) {
      return null;
    } else {
      final LineString reverseLine = LineStringUtil.reverse(line);
      return reverseLine;
    }
  }

  public Map<String, String> getSideAttributeNamePairs() {
    return this.sideAttributeNamePairs;
  }

  protected String getSideAttributePair(final String attributeName) {
    return this.sideAttributeNamePairs.get(attributeName);
  }

  public Set<String> getStartAttributeNames() {
    return this.startAttributeNames;
  }

  public boolean hasDirectionalAttributes() {
    return !this.directionalAttributeValues.isEmpty() || !this.reverseAttributeNameMap.isEmpty();
  }

  public boolean hasDirectionalAttributeValues(final String attributeName) {
    return this.directionalAttributeValues.containsKey(attributeName);
  }

  public boolean isEndAttribute(final String attributeName) {
    return this.endAttributeNames.contains(attributeName);
  }

  protected boolean isNull(final Record object1, final String name1, final Record object2,
    final String name2, final Collection<String> equalExcludeAttributes) {
    final Object value1 = object1.getValue(name1);
    final Object value2 = object2.getValue(name2);
    if (value1 == null && value2 == null) {
      return true;
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Both values not null (" + name1 + "=" + value1 + ") != (" + name2 + " = "
          + value2 + ")");
        LOG.debug(object1.toString());
        LOG.debug(object2.toString());
      }
      return false;
    }
  }

  public boolean isSideAttribute(final String attributeName) {
    return this.sideAttributeNames.contains(attributeName);
  }

  public boolean isStartAttribute(final String attributeName) {
    return this.startAttributeNames.contains(attributeName);
  }

  public void reverseAttributes(final Map<String, Object> object) {
    final Map<String, Object> reverseAttributes = getReverseAttributes(object);
    object.putAll(reverseAttributes);
  }

  public void reverseAttributesAndGeometry(final Map<String, Object> object) {
    final Map<String, Object> reverseAttributes = getReverseAttributesAndGeometry(object);
    object.putAll(reverseAttributes);
  }

  public void reverseGeometry(final Map<String, Object> object) {
    final Map<String, Object> reverseAttributes = getReverseGeometry(object);
    object.putAll(reverseAttributes);

  }

  public void setDirectionalAttributeValues(
    final Map<String, Map<Object, Object>> directionalAttributeValues) {
    for (final Entry<String, Map<Object, Object>> entry : directionalAttributeValues.entrySet()) {
      final String attributeName = entry.getKey();
      final Map<Object, Object> values = entry.getValue();
      addDirectionalAttributeValues(attributeName, values);
    }
  }

  public void setEdgeSplitAttributes(final LineString line, final Coordinates point,
    final List<Edge<Record>> edges) {
    for (final Edge<Record> edge : edges) {
      final Record object = edge.getObject();
      setSplitAttributes(line, point, object);
    }
  }

  public void setEndAndSideAttributeNamePairs(final List<List<String>> endAndSideAttributePairs) {
    for (final List<String> endAndSideAttributePair : endAndSideAttributePairs) {
      final String startLeftAttributeName = endAndSideAttributePair.get(0);
      final String startRightAttributeName = endAndSideAttributePair.get(1);
      final String endLeftAttributeName = endAndSideAttributePair.get(2);
      final String endRightAttributeName = endAndSideAttributePair.get(3);
      addEndAndSideAttributePairs(startLeftAttributeName, startRightAttributeName,
        endLeftAttributeName, endRightAttributeName);
    }
  }

  public void setEndAttributeNamePairs(final Map<String, String> attributeNamePairs) {
    this.endAttributeNamePairs.clear();
    this.endAttributeNames.clear();
    this.startAttributeNames.clear();
    for (final Entry<String, String> pair : attributeNamePairs.entrySet()) {
      final String from = pair.getKey();
      final String to = pair.getValue();
      addEndAttributePair(from, to);
    }
  }

  public void setEndAttributes(final Record source, final Map<String, Object> newObject) {
    for (final String attributeName : this.endAttributeNames) {
      final Object value = source.getValue(attributeName);
      newObject.put(attributeName, value);
    }
  }

  public void setEndTurnAttributeNamePairs(final List<List<String>> endAndSideAttributePairs) {
    for (final List<String> endAndSideAttributePair : endAndSideAttributePairs) {
      final String startLeftAttributeName = endAndSideAttributePair.get(0);
      final String startRightAttributeName = endAndSideAttributePair.get(1);
      final String endLeftAttributeName = endAndSideAttributePair.get(2);
      final String endRightAttributeName = endAndSideAttributePair.get(3);
      addEndTurnAttributePairs(startLeftAttributeName, startRightAttributeName,
        endLeftAttributeName, endRightAttributeName);
    }
  }

  public void setSideAttributeNamePairs(final Map<String, String> attributeNamePairs) {
    this.sideAttributeNamePairs.clear();
    for (final Entry<String, String> pair : attributeNamePairs.entrySet()) {
      final String from = pair.getKey();
      final String to = pair.getValue();
      addSideAttributePair(from, to);
    }
  }

  public void setSplitAttributes(final LineString line, final Coordinates point, final Record object) {
    final LineString newLine = object.getGeometryValue();
    if (newLine != null) {
      final boolean firstPoint = LineStringUtil.isFromPoint(newLine, point);
      final boolean toPoint = LineStringUtil.isToPoint(newLine, point);
      if (firstPoint) {
        if (!toPoint) {
          clearStartAttributes(object);
        }
      } else if (toPoint) {
        clearEndAttributes(object);
      }
    }
  }

  public void setStartAttributes(final Record source, final Map<String, Object> newObject) {
    for (final String attributeName : this.startAttributeNames) {
      final Object value = source.getValue(attributeName);
      newObject.put(attributeName, value);
    }
  }

  @Override
  public String toString() {
    return "DirectionalAttributes";
  }
}
