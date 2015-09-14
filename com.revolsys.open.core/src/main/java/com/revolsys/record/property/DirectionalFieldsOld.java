package com.revolsys.record.property;

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

import com.revolsys.equals.EqualsInstance;
import com.revolsys.equals.RecordEquals;
import com.revolsys.gis.graph.Edge;
import com.revolsys.gis.jts.LineStringUtil;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.record.Record;
import com.revolsys.record.Records;
import com.revolsys.record.schema.RecordDefinition;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class DirectionalFieldsOld extends AbstractRecordDefinitionProperty {
  private static final Logger LOG = LoggerFactory.getLogger(DirectionalFieldsOld.class);

  public static final String PROPERTY_NAME = DirectionalFieldsOld.class.getName() + ".propertyName";

  public static boolean canMergeRecords(final Coordinates point, final Record record1,
    final Record record2) {
    final Set<String> excludes = Collections.emptySet();
    final DirectionalFieldsOld property = getProperty(record1);
    return property.canMerge(point, record1, record2, excludes);
  }

  public static boolean canMergeRecords(final Coordinates point, final Record record1,
    final Record record2, final Set<String> equalExcludeFieldNames) {
    final DirectionalFieldsOld property = getProperty(record1);
    return property.canMerge(point, record1, record2, equalExcludeFieldNames);
  }

  public static void edgeSplitFieldValues(final LineString line, final Coordinates point,
    final List<Edge<Record>> edges) {
    if (!edges.isEmpty()) {
      final Edge<Record> firstEdge = edges.get(0);
      final Record record = firstEdge.getObject();
      final DirectionalFieldsOld property = getProperty(record);
      property.setEdgeSplitFieldValues(line, point, edges);
    }
  }

  public static boolean equalsRecords(final Record record1, final Record record2) {
    final Set<String> excludes = Collections.emptySet();
    return equalsRecords(record1, record2, excludes);
  }

  public static boolean equalsRecords(final Record record1, final Record record2,
    final Collection<String> equalExcludeFieldNames) {
    final DirectionalFieldsOld property = getProperty(record1);
    return property.equals(record1, record2, equalExcludeFieldNames);
  }

  public static Set<String> getCantMergeFieldNamesRecords(final Coordinates point,
    final Record record1, final Record record2, final Set<String> equalExcludeFieldNames) {
    final DirectionalFieldsOld property = getProperty(record1);
    return property.getCantMergeFieldNames(point, record1, record2, equalExcludeFieldNames);
  }

  public static DirectionalFieldsOld getProperty(final Record record) {
    final RecordDefinition recordDefinition = record.getRecordDefinition();
    return getProperty(recordDefinition);
  }

  public static DirectionalFieldsOld getProperty(final RecordDefinition recordDefinition) {
    DirectionalFieldsOld property = recordDefinition.getProperty(PROPERTY_NAME);
    if (property == null) {
      property = new DirectionalFieldsOld();
      property.setRecordDefinition(recordDefinition);
    }
    return property;
  }

  public static Record getReverseRecord(final Record record) {
    final DirectionalFieldsOld property = getProperty(record);
    final Record reverse = property.getReverse(record);
    return reverse;
  }

  public static boolean hasProperty(final Record record) {
    final RecordDefinition recordDefinition = record.getRecordDefinition();
    return recordDefinition.getProperty(PROPERTY_NAME) != null;
  }

  public static Record merge(final Coordinates point, final Record record1, final Record record2) {
    final DirectionalFieldsOld property = getProperty(record1);
    return property.getMergedRecord(point, record1, record2);
  }

  public static Record merge(final Record record1, final Record record2) {
    final DirectionalFieldsOld property = getProperty(record1);
    return property.getMergedRecord(record1, record2);
  }

  public static Record mergeLongest(final Coordinates point, final Record record1,
    final Record record2) {
    final DirectionalFieldsOld property = getProperty(record1);
    return property.getMergedRecordReverseLongest(point, record1, record2);
  }

  public static Record mergeLongest(final Record record1, final Record record2) {
    final DirectionalFieldsOld property = getProperty(record1);
    return property.getMergedRecordReverseLongest(record1, record2);
  }

  public static void reverse(final Record record) {
    final DirectionalFieldsOld property = getProperty(record);
    property.reverseFieldValuesAndGeometry(record);
  }

  private final Map<String, Map<Object, Object>> directionalFieldValues = new HashMap<String, Map<Object, Object>>();

  private final List<List<String>> endAndSideFieldNamePairs = new ArrayList<List<String>>();

  private final Map<String, String> endFieldNamePairs = new HashMap<String, String>();

  private final Set<String> fromFieldNames = new HashSet<String>();

  private final Map<String, String> reverseFieldNameMap = new HashMap<String, String>();

  private final Map<String, String> sideFieldNamePairs = new HashMap<String, String>();

  private final Set<String> sideFieldNames = new HashSet<String>();

  private final Set<String> toFieldNames = new HashSet<String>();

  public DirectionalFieldsOld() {
  }

  public void addDirectionalFieldValues(final String fieldName,
    final Map<? extends Object, ? extends Object> values) {
    final Map<Object, Object> newValues = new LinkedHashMap<Object, Object>();
    for (final Entry<? extends Object, ? extends Object> entry : values.entrySet()) {
      final Object value1 = entry.getKey();
      final Object value2 = entry.getValue();
      addValue(newValues, value1, value2);
      addValue(newValues, value2, value1);
    }
    this.directionalFieldValues.put(fieldName, newValues);
  }

  public void addEndAndSideFieldNamePairs(final String startLeftFieldName,
    final String startRightFieldName, final String endLeftFieldName,
    final String endRightFieldName) {
    this.endAndSideFieldNamePairs.add(
      Arrays.asList(startLeftFieldName, startRightFieldName, endLeftFieldName, endRightFieldName));
    addEndFieldNamePairInternal(startLeftFieldName, endLeftFieldName);
    addEndFieldNamePairInternal(startRightFieldName, endRightFieldName);
    addFieldNamePair(this.reverseFieldNameMap, startLeftFieldName, endRightFieldName);
    addFieldNamePair(this.reverseFieldNameMap, endLeftFieldName, startRightFieldName);
  }

  public void addEndFieldNamePair(final String startFieldName, final String endFieldName) {
    addEndFieldNamePairInternal(startFieldName, endFieldName);
    addFieldNamePair(this.reverseFieldNameMap, startFieldName, endFieldName);
  }

  private void addEndFieldNamePairInternal(final String startFieldName, final String endFieldName) {
    addFieldNamePair(this.endFieldNamePairs, startFieldName, endFieldName);
    this.fromFieldNames.add(startFieldName);
    this.toFieldNames.add(endFieldName);
  }

  /**
   * Add a mapping from the fromFieldName to the toFieldName and an
   * inverse mapping to the namePairs map.
   *
   * @param namePairs The name pair mapping.
   * @param fromFieldName The from attribute name.
   * @param toFieldName The to attribute name.
   */
  private void addFieldNamePair(final Map<String, String> namePairs, final String fromFieldName,
    final String toFieldName) {
    final String fromPair = namePairs.get(fromFieldName);
    if (fromPair == null) {
      final String toPair = namePairs.get(toFieldName);
      if (toPair == null) {
        namePairs.put(fromFieldName, toFieldName);
        namePairs.put(toFieldName, fromFieldName);
      } else if (toPair.equals(fromFieldName)) {
        throw new IllegalArgumentException(
          "Cannot override mapping " + toFieldName + "=" + toPair + " to " + fromFieldName);
      }
    } else if (fromPair.equals(toFieldName)) {
      throw new IllegalArgumentException(
        "Cannot override mapping " + fromFieldName + "=" + fromPair + " to " + toFieldName);
    }
  }

  public void addSideFieldNamePair(final String leftFieldName, final String rightFieldName) {
    addFieldNamePair(this.sideFieldNamePairs, leftFieldName, rightFieldName);
    this.sideFieldNames.add(leftFieldName);
    this.sideFieldNames.add(rightFieldName);
    addFieldNamePair(this.reverseFieldNameMap, leftFieldName, rightFieldName);
  }

  protected void addValue(final Map<Object, Object> map, final Object key, final Object value) {
    final Object oldValue = map.get(key);
    if (oldValue != null && !oldValue.equals(value)) {
      throw new IllegalArgumentException(
        "Cannot override mapping " + key + "=" + oldValue + " with " + value);
    }
    map.put(key, value);
  }

  public boolean canMerge(final Coordinates point, final Record record1, final Record record2,
    final Collection<String> equalExcludeFieldNames) {
    final boolean[] forwardsIndicators = getForwardsIndicators(point, record1, record2);

    if (forwardsIndicators != null) {
      final RecordDefinition recordDefinition = getRecordDefinition();
      final EqualIgnoreFieldNames equalIgnore = EqualIgnoreFieldNames.getProperty(recordDefinition);
      for (final String fieldName : recordDefinition.getFieldNames()) {
        if (!RecordEquals.isFieldIgnored(recordDefinition, equalExcludeFieldNames, fieldName)
          && !equalIgnore.isFieldIgnored(fieldName)) {
          if (!canMerge(fieldName, point, record1, record2, equalExcludeFieldNames,
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

  public boolean canMerge(final String fieldName, final Coordinates point, final Record record1,
    final Record record2, final Collection<String> equalExcludeFieldNames,
    final boolean[] forwardsIndicators) {
    final RecordDefinition recordDefinition = getRecordDefinition();
    if (fieldName.equals(recordDefinition.getGeometryFieldName())) {
      final LineString line1 = record1.getGeometry();
      final LineString line2 = record2.getGeometry();
      return !line1.equals(line2);
    }
    if (forwardsIndicators == null) {
      return false;
    } else {
      final boolean line1Forwards = forwardsIndicators[0];
      final boolean line2Forwards = forwardsIndicators[1];
      if (hasDirectionalFieldValues(fieldName)) {
        if (line1Forwards != line2Forwards) {
          final Object value1 = record1.getValue(fieldName);
          final Object value2 = getDirectionalFieldValue(record2, fieldName);
          if (EqualsInstance.INSTANCE.equals(value1, value2, equalExcludeFieldNames)) {
            return true;
          } else {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Different values (" + fieldName + "=" + value1 + ") != (" + fieldName
                + " = " + value2 + ")");
              LOG.debug(record1.toString());
              LOG.debug(record2.toString());
            }
            return false;
          }
        }
      } else if (isFromField(fieldName)) {
        return canMergeFromField(fieldName, record1, line1Forwards, record2, line2Forwards,
          equalExcludeFieldNames);
      } else if (isToField(fieldName)) {
        return canMergeToField(fieldName, record1, line1Forwards, record2, line2Forwards,
          equalExcludeFieldNames);
      } else if (isSideField(fieldName)) {
        if (line1Forwards != line2Forwards) {
          final String oppositeFieldName = getSideFieldName(fieldName);
          if (oppositeFieldName == null) { // only check the pair once
            return true;
          } else {
            return equals(record1, fieldName, record2, oppositeFieldName, equalExcludeFieldNames);
          }
        }
      }
      return equals(record1, fieldName, record2, fieldName, equalExcludeFieldNames);
    }
  }

  protected boolean canMergeFromField(final String startFieldName, final Record record1,
    final boolean line1Forwards, final Record record2, final boolean line2Forwards,
    final Collection<String> equalExcludeFieldNames) {
    final String endFieldName = this.endFieldNamePairs.get(startFieldName);
    if (line1Forwards) {
      if (line2Forwards) {
        // -->*--> true true
        return isNull(record1, endFieldName, record2, startFieldName, equalExcludeFieldNames);
      } else {
        // -->*<-- true false
        return true;
      }
    } else {
      if (line2Forwards) {
        // <--*--> false true
        return isNull(record1, startFieldName, record2, startFieldName, equalExcludeFieldNames);
      } else {
        // <--*<-- false false
        return isNull(record1, startFieldName, record2, endFieldName, equalExcludeFieldNames);
      }
    }
  }

  protected boolean canMergeToField(final String endFieldName, final Record record1,
    final boolean line1Forwards, final Record record2, final boolean line2Forwards,
    final Collection<String> equalExcludeFieldNames) {
    final String startFieldName = this.endFieldNamePairs.get(endFieldName);
    if (line1Forwards) {
      if (line2Forwards) {
        // -->*--> true true
        return isNull(record1, endFieldName, record2, startFieldName, equalExcludeFieldNames);
      } else {
        // -->*<-- true false
        return isNull(record1, endFieldName, record2, endFieldName, equalExcludeFieldNames);
      }
    } else {
      if (line2Forwards) {
        // <--*--> false true
        return true;
      } else {
        // <--*<-- false false
        return isNull(record1, startFieldName, record2, endFieldName, equalExcludeFieldNames);
      }
    }
  }

  public void clearFromFieldValues(final Record record) {
    for (final String fieldName : this.fromFieldNames) {
      record.setValue(fieldName, null);
    }
  }

  public void clearToFieldValues(final Record record) {
    for (final String fieldName : this.toFieldNames) {
      record.setValue(fieldName, null);
    }
  }

  public boolean equals(final Record record1, final Record record2,
    final Collection<String> equalExcludeFieldNames) {
    final RecordDefinition recordDefinition = getRecordDefinition();
    final EqualIgnoreFieldNames equalIgnore = EqualIgnoreFieldNames.getProperty(recordDefinition);
    for (final String fieldName : recordDefinition.getFieldNames()) {
      if (!equalExcludeFieldNames.contains(fieldName) && !equalIgnore.isFieldIgnored(fieldName)) {
        if (!equals(fieldName, record1, record2, equalExcludeFieldNames)) {
          return false;
        }
      }
    }
    return true;
  }

  protected boolean equals(final Record record1, final String name1, final Record record2,
    final String name2, final Collection<String> equalExcludeFieldNames) {
    final Object value1 = record1.getValue(name1);
    final Object value2 = record2.getValue(name2);
    if (EqualsInstance.INSTANCE.equals(value1, value2, equalExcludeFieldNames)) {
      return true;
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug(
          "Different values (" + name1 + "=" + value1 + ") != (" + name2 + " = " + value2 + ")");
        LOG.debug(record1.toString());
        LOG.debug(record2.toString());
      }
      return false;
    }
  }

  protected boolean equals(final String fieldName, final Record record1, final Record record2,
    final Collection<String> equalExcludeFieldNames) {
    final LineString line1 = record1.getGeometry();
    final LineString line2 = record2.getGeometry();
    final RecordDefinition recordDefinition = getRecordDefinition();
    if (fieldName.equals(recordDefinition.getGeometryFieldName())) {
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
      return equalsReverse(fieldName, record1, record2, equalExcludeFieldNames);
    } else {
      return equals(record1, fieldName, record2, fieldName, equalExcludeFieldNames);
    }
  }

  private boolean equalsReverse(final String fieldName, final Record record1, final Record record2,
    final Collection<String> equalExcludeFieldNames) {
    if (hasDirectionalFieldValues(fieldName)) {
      final Object value1 = record1.getValue(fieldName);
      final Object value2 = getDirectionalFieldValue(record2, fieldName);
      if (EqualsInstance.INSTANCE.equals(value1, value2, equalExcludeFieldNames)) {
        return true;
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Different values (" + fieldName + "=" + value1 + ") != (" + fieldName + " = "
            + value2 + ")");
          LOG.debug(record1.toString());
          LOG.debug(record2.toString());
        }
        return false;
      }
    } else {
      final String reverseFieldName = getReverseFieldName(fieldName);
      if (reverseFieldName == null) {
        return equals(record1, fieldName, record2, fieldName, equalExcludeFieldNames);
      } else {
        return equals(record1, fieldName, record2, reverseFieldName, equalExcludeFieldNames);
      }
    }
  }

  public Set<String> getCantMergeFieldNames(final Coordinates point, final Record record1,
    final Record record2, final Collection<String> equalExcludeFieldNames) {
    final RecordDefinition recordDefinition = getRecordDefinition();
    final boolean[] forwardsIndicators = getForwardsIndicators(point, record1, record2);
    if (forwardsIndicators != null) {
      final Set<String> fieldNames = new LinkedHashSet<String>();
      final EqualIgnoreFieldNames equalIgnore = EqualIgnoreFieldNames.getProperty(recordDefinition);
      for (final String fieldName : recordDefinition.getFieldNames()) {
        if (!equalExcludeFieldNames.contains(fieldName) && !equalIgnore.isFieldIgnored(fieldName)) {
          if (!canMerge(fieldName, point, record1, record2, equalExcludeFieldNames,
            forwardsIndicators)) {
            fieldNames.add(fieldName);
          }
        }
      }
      return fieldNames;
    } else {
      final String geometryFieldName = recordDefinition.getGeometryFieldName();
      return Collections.singleton(geometryFieldName);
    }
  }

  protected Object getDirectionalFieldValue(final Map<String, ? extends Object> record,
    final String fieldName) {
    final Object value = record.get(fieldName);

    final Map<Object, Object> valueMap = this.directionalFieldValues.get(fieldName);
    if (valueMap != null) {
      if (valueMap.containsKey(value)) {
        final Object directionalValue = valueMap.get(value);
        return directionalValue;
      }
    }
    return value;
  }

  public Map<String, Map<Object, Object>> getDirectionalFieldValues() {
    return this.directionalFieldValues;
  }

  public List<List<String>> getEndAndSideFieldNamePairs() {
    return this.endAndSideFieldNamePairs;
  }

  public Map<String, String> getEndFieldNamePairs() {
    return this.endFieldNamePairs;
  }

  public Set<String> getEndFieldNames() {
    return this.toFieldNames;
  }

  protected boolean[] getForwardsIndicators(final Coordinates point, final Record record1,
    final Record record2) {
    final LineString line1 = record1.getGeometry();
    final LineString line2 = record2.getGeometry();

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

  public Set<String> getFromFieldNames() {
    return this.fromFieldNames;
  }

  public Map<String, Object> getMergedMap(final Coordinates point, final Record record1,
    Record record2) {
    final LineString line1 = record1.getGeometry();
    LineString line2 = record2.getGeometry();
    final CoordinatesList points1 = CoordinatesListUtil.get(line1);
    final CoordinatesList points2 = CoordinatesListUtil.get(line2);

    Record fromRecord;
    Record toRecord;

    LineString newLine;
    final int lastPoint1 = points1.size() - 1;
    final int lastPoint2 = points2.size() - 1;

    if (points1.equal(0, points2, 0) && points1.equal2d(0, point)) {
      record2 = getReverse(record2);
      line2 = record2.getGeometry();
      fromRecord = record2;
      toRecord = record1;
      newLine = LineStringUtil.merge(point, line1, line2);
    } else
      if (points1.equal(lastPoint1, points2, lastPoint2) && points1.equal2d(lastPoint1, point)) {
      record2 = getReverse(record2);
      line2 = record2.getGeometry();
      fromRecord = record1;
      toRecord = record2;
      newLine = LineStringUtil.merge(point, line1, line2);
    } else if (points1.equal(lastPoint1, points2, 0) && points1.equal2d(lastPoint1, point)) {
      fromRecord = record1;
      toRecord = record2;
      newLine = LineStringUtil.merge(point, line1, line2);
    } else if (points1.equal(0, points2, lastPoint2) && points1.equal2d(0, point)) {
      fromRecord = record2;
      toRecord = record1;
      newLine = LineStringUtil.merge(point, line2, line1);
    } else {
      throw new IllegalArgumentException("Lines for records don't touch");
    }

    final Map<String, Object> newValues = new LinkedHashMap<>(record1);
    setFromFieldValues(fromRecord, toRecord, newValues);
    setToFieldValues(toRecord, fromRecord, newValues);
    final RecordDefinition recordDefinition = record1.getRecordDefinition();
    final String geometryFieldName = recordDefinition.getGeometryFieldName();
    newValues.put(geometryFieldName, newLine);
    return newValues;
  }

  /**
   * Get a new record that is the result of merging the two records. The
   * attributes will be taken from the record with the longest length. If one
   * line needs to be reversed then the second record will be reversed.
   *
   * @param record1
   * @param record2
   * @return
   */
  public Record getMergedRecord(final Coordinates point, final Record record1, Record record2) {
    final LineString line1 = record1.getGeometry();
    LineString line2 = record2.getGeometry();
    final CoordinatesList points1 = CoordinatesListUtil.get(line1);
    final CoordinatesList points2 = CoordinatesListUtil.get(line2);

    Record fromRecord;
    Record toRecord;

    final boolean line1Longer = line1.getLength() > line2.getLength();
    LineString newLine;
    final int lastPoint1 = points1.size() - 1;
    final int lastPoint2 = points2.size() - 1;

    if (points1.equal(0, points2, 0) && points1.equal2d(0, point)) {
      record2 = getReverse(record2);
      line2 = record2.getGeometry();
      fromRecord = record2;
      toRecord = record1;
      newLine = LineStringUtil.merge(point, line1, line2);
    } else
      if (points1.equal(lastPoint1, points2, lastPoint2) && points1.equal2d(lastPoint1, point)) {
      record2 = getReverse(record2);
      line2 = record2.getGeometry();
      fromRecord = record1;
      toRecord = record2;
      newLine = LineStringUtil.merge(point, line1, line2);
    } else if (points1.equal(lastPoint1, points2, 0) && points1.equal2d(lastPoint1, point)) {
      fromRecord = record1;
      toRecord = record2;
      newLine = LineStringUtil.merge(point, line1, line2);
    } else if (points1.equal(0, points2, lastPoint2) && points1.equal2d(0, point)) {
      fromRecord = record2;
      toRecord = record1;
      newLine = LineStringUtil.merge(point, line2, line1);
    } else {
      throw new IllegalArgumentException("Lines for records don't touch");
    }

    Record newRecord;
    if (line1Longer) {
      newRecord = Records.copy(record1, newLine);
    } else {
      newRecord = Records.copy(record2, newLine);
    }
    setFromFieldValues(fromRecord, toRecord, newRecord);
    setToFieldValues(toRecord, fromRecord, newRecord);
    LengthFieldName.setRecordLength(newRecord);
    return newRecord;
  }

  /**
   * Get a new record that is the result of merging the two records. The
   * attributes will be taken from the record with the longest length. If one
   * line needs to be reversed then the second record will be reversed.
   *
   * @param record1
   * @param record2
   * @return
   */
  public Record getMergedRecord(final Record record1, Record record2) {
    final LineString line1 = record1.getGeometry();
    LineString line2 = record2.getGeometry();
    final CoordinatesList points1 = CoordinatesListUtil.get(line1);
    final CoordinatesList points2 = CoordinatesListUtil.get(line2);

    Record fromRecord;
    Record toRecord;

    final boolean line1Longer = line1.getLength() > line2.getLength();
    LineString newLine;

    if (points1.equal(0, points2, 0)) {
      record2 = getReverse(record2);
      line2 = record2.getGeometry();
      fromRecord = record2;
      toRecord = record1;
      newLine = LineStringUtil.merge(line1, line2);
    } else if (points1.equal(points1.size() - 1, points2, points2.size() - 1)) {
      record2 = getReverse(record2);
      line2 = record2.getGeometry();
      fromRecord = record1;
      toRecord = record2;
      newLine = LineStringUtil.merge(line1, line2);
    } else if (points1.equal(points1.size() - 1, points2, 0)) {
      fromRecord = record1;
      toRecord = record2;
      newLine = LineStringUtil.merge(line1, line2);
    } else if (points1.equal(0, points2, points2.size() - 1)) {
      fromRecord = record2;
      toRecord = record1;
      newLine = LineStringUtil.merge(line2, line1);
    } else {
      throw new IllegalArgumentException("Lines for records don't touch");
    }

    Record newRecord;
    if (line1Longer) {
      newRecord = Records.copy(record1, newLine);
    } else {
      newRecord = Records.copy(record2, newLine);
    }
    setFromFieldValues(fromRecord, toRecord, newRecord);
    setToFieldValues(toRecord, fromRecord, newRecord);
    LengthFieldName.setRecordLength(newRecord);
    return newRecord;
  }

  public Record getMergedRecordReverseLongest(final Coordinates point, final Record record1,
    final Record record2) {
    final LineString line1 = record1.getGeometry();
    final LineString line2 = record2.getGeometry();
    if (line1.getLength() >= line2.getLength()) {
      return getMergedRecord(point, record1, record2);
    } else {
      return getMergedRecord(point, record2, record1);
    }
  }

  /**
   * Get a new record that is the result of merging the two records. The
   * attributes will be taken from the record with the longest length. If one
   * line needs to be reversed then the longest will be reversed.
   *
   * @param record1
   * @param record2
   * @return
   */
  public Record getMergedRecordReverseLongest(final Record record1, final Record record2) {
    final LineString line1 = record1.getGeometry();
    final LineString line2 = record2.getGeometry();
    if (line1.getLength() >= line2.getLength()) {
      return getMergedRecord(record1, record2);
    } else {
      return getMergedRecord(record2, record1);
    }
  }

  @Override
  public String getPropertyName() {
    return PROPERTY_NAME;
  }

  public Record getReverse(final Record record) {
    final Record reverse = record.clone();
    reverseFieldValuesAndGeometry(reverse);
    return reverse;
  }

  public String getReverseFieldName(final String fieldName) {
    return this.reverseFieldNameMap.get(fieldName);
  }

  public Map<String, Object> getReverseFieldValues(final Map<String, Object> record) {
    final Map<String, Object> reverse = new LinkedHashMap<String, Object>(record);
    for (final Entry<String, String> pair : this.reverseFieldNameMap.entrySet()) {
      final String fromFieldName = pair.getKey();
      final String toFieldName = pair.getValue();
      final Object toValue = record.get(toFieldName);
      reverse.put(fromFieldName, toValue);
    }
    for (final String fieldName : this.directionalFieldValues.keySet()) {
      final Object value = getDirectionalFieldValue(record, fieldName);
      reverse.put(fieldName, value);
    }
    return reverse;
  }

  public Map<String, Object> getReverseFieldValuesAndGeometry(final Map<String, Object> record) {
    final Map<String, Object> reverse = getReverseFieldValues(record);
    final String geometryFieldName = getRecordDefinition().getGeometryFieldName();
    if (geometryFieldName != null) {
      final Geometry geometry = getReverseLine(record);
      reverse.put(geometryFieldName, geometry);
    }
    return reverse;
  }

  public Map<String, Object> getReverseGeometry(final Map<String, Object> record) {
    final Map<String, Object> reverse = new LinkedHashMap<String, Object>(record);
    final String geometryFieldName = getRecordDefinition().getGeometryFieldName();
    if (geometryFieldName != null) {
      final Geometry geometry = getReverseLine(record);
      reverse.put(geometryFieldName, geometry);
    }
    return reverse;
  }

  protected Geometry getReverseLine(final Map<String, Object> record) {
    final String geometryFieldName = getRecordDefinition().getGeometryFieldName();
    final LineString line = (LineString)record.get(geometryFieldName);
    if (line == null) {
      return null;
    } else {
      final LineString reverseLine = LineStringUtil.reverse(line);
      return reverseLine;
    }
  }

  protected String getSideFieldName(final String fieldName) {
    return this.sideFieldNamePairs.get(fieldName);
  }

  public Map<String, String> getSideFieldNamePairs() {
    return this.sideFieldNamePairs;
  }

  public boolean hasDirectionalFields() {
    return !this.directionalFieldValues.isEmpty() || !this.reverseFieldNameMap.isEmpty();
  }

  public boolean hasDirectionalFieldValues(final String fieldName) {
    return this.directionalFieldValues.containsKey(fieldName);
  }

  public boolean isFromField(final String fieldName) {
    return this.fromFieldNames.contains(fieldName);
  }

  protected boolean isNull(final Record record1, final String name1, final Record record2,
    final String name2, final Collection<String> equalExcludeFieldNames) {
    final Object value1 = record1.getValue(name1);
    final Object value2 = record2.getValue(name2);
    if (value1 == null && value2 == null) {
      return true;
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Both values not null (" + name1 + "=" + value1 + ") != (" + name2 + " = "
          + value2 + ")");
        LOG.debug(record1.toString());
        LOG.debug(record2.toString());
      }
      return false;
    }
  }

  public boolean isSideField(final String fieldName) {
    return this.sideFieldNames.contains(fieldName);
  }

  public boolean isToField(final String fieldName) {
    return this.toFieldNames.contains(fieldName);
  }

  public void reverseFieldValues(final Map<String, Object> record) {
    final Map<String, Object> reverseFieldValues = getReverseFieldValues(record);
    record.putAll(reverseFieldValues);
  }

  public void reverseFieldValuesAndGeometry(final Map<String, Object> record) {
    final Map<String, Object> reverseFieldValues = getReverseFieldValuesAndGeometry(record);
    record.putAll(reverseFieldValues);
  }

  public void reverseGeometry(final Map<String, Object> record) {
    final Map<String, Object> reverseFieldValues = getReverseGeometry(record);
    record.putAll(reverseFieldValues);

  }

  public void setDirectionalFieldValues(
    final Map<String, Map<Object, Object>> directionalFieldValues) {
    for (final Entry<String, Map<Object, Object>> entry : directionalFieldValues.entrySet()) {
      final String fieldName = entry.getKey();
      final Map<Object, Object> values = entry.getValue();
      addDirectionalFieldValues(fieldName, values);
    }
  }

  public void setEdgeSplitFieldValues(final LineString line, final Coordinates point,
    final List<Edge<Record>> edges) {
    for (final Edge<Record> edge : edges) {
      final Record record = edge.getObject();
      setSplitFieldValues(line, point, record);
    }
  }

  public void setEndAndSideFieldNamePairs(final List<List<String>> endAndSideFieldNamePairs) {
    for (final List<String> endAndSideFieldNamePair : endAndSideFieldNamePairs) {
      final String startLeftFieldName = endAndSideFieldNamePair.get(0);
      final String startRightFieldName = endAndSideFieldNamePair.get(1);
      final String endLeftFieldName = endAndSideFieldNamePair.get(2);
      final String endRightFieldName = endAndSideFieldNamePair.get(3);
      addEndAndSideFieldNamePairs(startLeftFieldName, startRightFieldName, endLeftFieldName,
        endRightFieldName);
    }
  }

  public void setEndFieldNamePairs(final Map<String, String> fieldNamePairs) {
    this.endFieldNamePairs.clear();
    this.toFieldNames.clear();
    this.fromFieldNames.clear();
    for (final Entry<String, String> pair : fieldNamePairs.entrySet()) {
      final String from = pair.getKey();
      final String to = pair.getValue();
      addEndFieldNamePair(from, to);
    }
  }

  public void setFromFieldValues(final Record fromRecord, final Record toRecord,
    final Map<String, Object> newRecord) {
    for (final String fieldName : this.fromFieldNames) {
      Object value = fromRecord.getValue(fieldName);
      if (value == null) {
        value = toRecord.getValue(fieldName);
      }
      newRecord.put(fieldName, value);
    }
  }

  public void setSideFieldNamePairs(final Map<String, String> fieldNamePairs) {
    this.sideFieldNamePairs.clear();
    for (final Entry<String, String> pair : fieldNamePairs.entrySet()) {
      final String from = pair.getKey();
      final String to = pair.getValue();
      addSideFieldNamePair(from, to);
    }
  }

  public void setSplitFieldValues(final LineString line, final Coordinates point,
    final Record record) {
    final LineString newLine = record.getGeometry();
    if (newLine != null) {
      final boolean firstPoint = LineStringUtil.isFromPoint(newLine, point);
      final boolean toPoint = LineStringUtil.isToPoint(newLine, point);
      if (firstPoint) {
        if (!toPoint) {
          clearFromFieldValues(record);
        }
      } else if (toPoint) {
        clearToFieldValues(record);
      }
    }
  }

  public void setToFieldValues(final Record toRecord, final Record fromRecord,
    final Map<String, Object> newValues) {
    for (final String fieldName : this.toFieldNames) {
      Object value = toRecord.getValue(fieldName);
      if (value == null) {
        value = fromRecord.getValue(fieldName);
      }
      newValues.put(fieldName, value);
    }
  }

  @Override
  public String toString() {
    return "DirectionalFieldsOld";
  }
}
