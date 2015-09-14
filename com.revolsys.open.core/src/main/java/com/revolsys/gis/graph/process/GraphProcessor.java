package com.revolsys.gis.graph.process;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.revolsys.gis.graph.Edge;
import com.revolsys.gis.graph.RecordGraph;
import com.revolsys.gis.model.coordinates.CoordinatesPrecisionModel;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.process.BaseInOutProcess;
import com.revolsys.record.Record;
import com.revolsys.util.ObjectProcessor;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class GraphProcessor extends BaseInOutProcess<Record, Record> {
  private static final Logger LOG = LoggerFactory.getLogger(GraphProcessor.class);

  private RecordGraph graph;

  private CoordinatesPrecisionModel precisionModel;

  private List<ObjectProcessor<RecordGraph>> processors = new ArrayList<ObjectProcessor<RecordGraph>>();

  public CoordinatesPrecisionModel getPrecisionModel() {
    return this.precisionModel;
  }

  public List<ObjectProcessor<RecordGraph>> getProcessors() {
    return this.processors;
  }

  @Override
  protected void init() {
    super.init();
    this.graph = new RecordGraph();
    if (this.precisionModel != null) {
      this.graph.setPrecisionModel(this.precisionModel);
    }
  }

  @Override
  protected void postRun(final Channel<Record> in, final Channel<Record> out) {
    if (out != null) {
      processGraph();
      for (final Edge<Record> edge : this.graph.getEdges()) {
        final Record object = edge.getObject();
        out.write(object);
      }
    }
  }

  @Override
  protected void process(final Channel<Record> in, final Channel<Record> out, final Record object) {
    final Geometry geometry = object.getGeometry();
    if (geometry instanceof LineString) {
      final LineString line = (LineString)geometry;
      this.graph.addEdge(object, line);
    } else {
      if (out != null) {
        out.write(object);
      }
    }
  }

  private void processGraph() {
    if (this.graph != null) {
      for (final ObjectProcessor<RecordGraph> processor : this.processors) {
        LOG.info(processor.getClass().getName());
        processor.process(this.graph);
      }
    }
  }

  public void setPrecisionModel(final CoordinatesPrecisionModel precisionModel) {
    this.precisionModel = precisionModel;
  }

  public void setProcessors(final List<ObjectProcessor<RecordGraph>> processors) {
    this.processors = processors;
  }
}
