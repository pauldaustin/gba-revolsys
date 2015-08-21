package com.revolsys.swing.map.overlay;

import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.jts.geom.GeometryEditUtil;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.IndexedLineSegment;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.util.CollectionUtil;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class CloseLocation {

  private final LayerRecord object;

  private final int[] vertexIndex;

  private final IndexedLineSegment segment;

  private final AbstractRecordLayer layer;

  private final Geometry geometry;

  private final Point point;

  public CloseLocation(final AbstractRecordLayer layer, final LayerRecord object,
    final Geometry geometry, final int[] vertexIndex, final IndexedLineSegment segment,
    final Point point) {
    this.object = object;
    this.layer = layer;
    this.geometry = geometry;
    this.vertexIndex = vertexIndex;
    this.segment = segment;
    this.point = point;
  }

  @SuppressWarnings("unchecked")
  public <G extends Geometry> G getGeometry() {
    return (G)this.geometry;
  }

  public GeometryFactory getGeometryFactory() {
    return this.layer.getGeometryFactory();
  }

  public Object getId() {
    Object id = null;
    if (this.object != null) {
      id = this.object.getIdValue();
    }
    if (id == null) {
      id = "NEW";
    }
    return id;
  }

  public String getIdFieldName() {
    return getMetaData().getIdFieldName();
  }

  public String getIndexString() {
    int[] index = this.vertexIndex;
    if (index != null) {
    } else {
      index = this.segment.getIndex();
    }
    return CollectionUtil.toString(CollectionUtil.arrayToList(index));
  }

  public AbstractRecordLayer getLayer() {
    return this.layer;
  }

  public RecordDefinition getMetaData() {
    return this.layer.getRecordDefinition();
  }

  public LayerRecord getObject() {
    return this.object;
  }

  public Point getPoint() {
    return this.point;
  }

  public IndexedLineSegment getSegment() {
    return this.segment;
  }

  public int[] getSegmentIndex() {
    return this.segment.getIndex();
  }

  public String getType() {
    if (this.geometry instanceof Point) {
      return "Point";
    } else if (this.segment != null) {
      return "Edge";
    } else {
      if (GeometryEditUtil.isFromPoint(this.geometry, this.vertexIndex)
        || GeometryEditUtil.isToPoint(this.geometry, this.vertexIndex)) {
        return "End-Vertex";
      } else {
        return "Vertex";
      }
    }
  }

  public String getTypePath() {
    final RecordDefinition metaData = getMetaData();
    return metaData.getPath();
  }

  public int[] getVertexIndex() {
    return this.vertexIndex;
  }

  @Override
  public String toString() {
    final StringBuffer string = new StringBuffer();
    string.append(getTypePath());
    string.append(", ");
    final RecordDefinition metaData = getMetaData();
    string.append(metaData.getIdFieldName());
    string.append("=");
    final Object id = getId();
    string.append(id);
    string.append(", ");
    string.append(getType());
    int[] index = this.vertexIndex;
    if (index != null) {
      string.append(", index=");
    } else {
      string.append(", index=");
      index = this.segment.getIndex();
    }
    final String indexString = CollectionUtil.toString(CollectionUtil.arrayToList(index));
    string.append(indexString);
    return string.toString();
  }

}
