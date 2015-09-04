package com.revolsys.gis.parallel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.context.HashMapContext;

import com.revolsys.data.record.ArrayRecord;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.gis.data.model.RecordMap;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.process.BaseInOutProcess;
import com.revolsys.util.JexlUtil;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

public class CreateObjectsWithinDistanceOfGeometry extends BaseInOutProcess<Record, Record> {

  private Map<String, Object> attributes = new HashMap<String, Object>();

  private double distance;

  private Channel<Record> geometryIn;

  private List<Record> geometryObjects = new ArrayList<Record>();

  private Map<RecordDefinition, Map<RecordDefinition, PreparedGeometry>> recordDefinitionGeometryMap = new HashMap<RecordDefinition, Map<RecordDefinition, PreparedGeometry>>();

  private String typePathTemplate;

  private Expression typePathTemplateExpression;

  private boolean writeOriginal;

  @Override
  protected void destroy() {
    super.destroy();
    if (this.geometryIn != null) {
      this.geometryIn.readDisconnect();
      this.geometryIn = null;
    }
    this.attributes = null;
    this.geometryObjects = null;
    this.recordDefinitionGeometryMap = null;
  }

  public Map<String, Object> getAttributes() {
    return this.attributes;
  }

  public double getDistance() {
    return this.distance;
  }

  public Channel<Record> getGeometryIn() {
    if (this.geometryIn == null) {
      setGeometryIn(new Channel<Record>());
    }
    return this.geometryIn;
  }

  public List<Record> getGeometryObjects() {
    return this.geometryObjects;
  }

  private final Map<RecordDefinition, PreparedGeometry> getRecordDefinitionGeometries(
    final RecordDefinition recordDefinition) {
    Map<RecordDefinition, PreparedGeometry> recordDefinitionGeometries = this.recordDefinitionGeometryMap
      .get(recordDefinition);
    if (recordDefinitionGeometries == null) {
      final PreparedGeometryFactory preparedGeometryFactory = new PreparedGeometryFactory();
      recordDefinitionGeometries = new LinkedHashMap<RecordDefinition, PreparedGeometry>();
      RecordDefinition newRecordDefinition;
      PreparedGeometry preparedGeometry;
      for (final Record object : this.geometryObjects) {
        Geometry geometry = object.getGeometry();
        if (geometry != null) {
          final JexlContext context = new HashMapContext();
          final Map<String, Object> vars = new HashMap<String, Object>(this.attributes);
          vars.putAll(new RecordMap(object));
          vars.put("typePath", recordDefinition.getPath());
          context.setVars(vars);
          final String typePath = (String)JexlUtil.evaluateExpression(context,
            this.typePathTemplateExpression);
          newRecordDefinition = new RecordDefinitionImpl(typePath, recordDefinition.getFields());
          if (this.distance > 0) {
            final BufferOp buffer = new BufferOp(geometry, new BufferParameters(1, 3, 2, 1.0D));
            geometry = buffer.getResultGeometry(this.distance);
          }
          geometry = DouglasPeuckerSimplifier.simplify(geometry, 2D);
          preparedGeometry = preparedGeometryFactory.create(geometry);
          recordDefinitionGeometries.put(newRecordDefinition, preparedGeometry);
        }
      }

      this.recordDefinitionGeometryMap.put(recordDefinition, recordDefinitionGeometries);
    }
    return recordDefinitionGeometries;
  }

  public String getTypeNameTemplate() {
    return this.typePathTemplate;
  }

  private void initializeGeometries(final Channel<Record> geometryIn) {
    if (geometryIn != null) {
      for (final Record object : geometryIn) {
        this.geometryObjects.add(object);
      }
    }
  }

  public boolean isWriteOriginal() {
    return this.writeOriginal;
  }

  @Override
  protected void preRun(final Channel<Record> in, final Channel<Record> out) {
    initializeGeometries(this.geometryIn);
  }

  @Override
  protected void process(final Channel<Record> in, final Channel<Record> out, final Record object) {
    if (this.writeOriginal) {
      out.write(object);
    }
    final RecordDefinition recordDefinition = object.getRecordDefinition();
    final Geometry geometryValue = object.getGeometry();
    final Map<RecordDefinition, PreparedGeometry> recordDefinitionGeometries = getRecordDefinitionGeometries(
      recordDefinition);
    for (final Entry<RecordDefinition, PreparedGeometry> recordDefinitionGeometry : recordDefinitionGeometries
      .entrySet()) {
      final RecordDefinition newRecordDefinition = recordDefinitionGeometry.getKey();
      final PreparedGeometry intersectsGeometry = recordDefinitionGeometry.getValue();
      if (intersectsGeometry.intersects(geometryValue)) {
        final Record newObject = new ArrayRecord(newRecordDefinition, object);
        out.write(newObject);
      }
    }
  }

  public void setAttributes(final Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public void setDistance(final double distance) {
    this.distance = distance;
  }

  public void setGeometryIn(final Channel<Record> geometryIn) {
    this.geometryIn = geometryIn;
    geometryIn.readConnect();
  }

  public void setGeometryObjects(final List<Record> geometryObjects) {
    this.geometryObjects = geometryObjects;
  }

  public void setTypeNameTemplate(final String typePathTemplate) {
    this.typePathTemplate = typePathTemplate;
    try {
      this.typePathTemplateExpression = JexlUtil.createExpression(typePathTemplate,
        "%\\{([^\\}]+)\\}");
    } catch (final Exception e) {
      throw new IllegalArgumentException(new StringBuilder().append("Invalid type name template: ")
        .append(typePathTemplate)
        .toString(), e);
    }
  }

  public void setWriteOriginal(final boolean writeOriginal) {
    this.writeOriginal = writeOriginal;
  }
}
