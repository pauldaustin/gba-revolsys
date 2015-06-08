package com.revolsys.gis.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.convert.converter.Converter;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.converter.process.SourceToTargetProcess;
import com.revolsys.gis.jts.JtsGeometryUtil;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.util.CollectionUtil;
import com.vividsolutions.jts.geom.Geometry;

public class SimpleDataObjectConveter implements Converter<Record, Record> {
  private RecordDefinition dataObjectMetaData;

  private RecordFactory factory;

  private List<SourceToTargetProcess<Record, Record>> processors = new ArrayList<SourceToTargetProcess<Record, Record>>();

  public SimpleDataObjectConveter() {
  }

  public SimpleDataObjectConveter(final RecordDefinition dataObjectMetaData) {
    setDataObjectMetaData(dataObjectMetaData);
  }

  public SimpleDataObjectConveter(final RecordDefinition dataObjectMetaData,
    final List<SourceToTargetProcess<Record, Record>> processors) {
    setDataObjectMetaData(dataObjectMetaData);
    this.processors = processors;
  }

  public SimpleDataObjectConveter(final RecordDefinition dataObjectMetaData,
    final SourceToTargetProcess<Record, Record>... processors) {
    this(dataObjectMetaData, Arrays.asList(processors));
  }

  public void addProcessor(final SourceToTargetProcess<Record, Record> processor) {
    this.processors.add(processor);
  }

  @Override
  public Record convert(final Record sourceObject) {
    final Record targetObject = this.factory.createRecord(this.dataObjectMetaData);
    final Geometry sourceGeometry = sourceObject.getGeometryValue();
    final GeometryFactory geometryFactory = GeometryFactory.getFactory(sourceGeometry);
    final Geometry targetGeometry = geometryFactory.createGeometry(sourceGeometry);
    JtsGeometryUtil.copyUserData(sourceGeometry, targetGeometry);
    targetObject.setGeometryValue(targetGeometry);
    for (final SourceToTargetProcess<Record, Record> processor : this.processors) {
      processor.process(sourceObject, targetObject);
    }
    return targetObject;
  }

  public RecordDefinition getDataObjectMetaData() {
    return this.dataObjectMetaData;
  }

  public List<SourceToTargetProcess<Record, Record>> getProcessors() {
    return this.processors;
  }

  public void setDataObjectMetaData(final RecordDefinition dataObjectMetaData) {
    this.dataObjectMetaData = dataObjectMetaData;
    this.factory = dataObjectMetaData.getRecordFactory();
  }

  public void setProcessors(final List<SourceToTargetProcess<Record, Record>> processors) {
    this.processors = processors;
  }

  @Override
  public String toString() {
    return this.dataObjectMetaData.getPath() + "\n  "
      + CollectionUtil.toString("\n  ", this.processors);
  }
}
