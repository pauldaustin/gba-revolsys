package com.revolsys.swing.map.layer.record;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;

import org.slf4j.LoggerFactory;

import com.revolsys.data.query.Query;
import com.revolsys.data.query.functions.F;
import com.revolsys.data.record.schema.Attribute;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.swing.map.layer.AbstractLayer;
import com.revolsys.swing.parallel.AbstractSwingWorker;

public class LoadingWorker extends AbstractSwingWorker<List<LayerRecord>, Void> {
  private final BoundingBox viewportBoundingBox;

  private final DataObjectStoreLayer layer;

  public LoadingWorker(final DataObjectStoreLayer layer,
    final BoundingBox viewportBoundingBox) {
    this.layer = layer;
    this.viewportBoundingBox = viewportBoundingBox;

  }

  @Override
  protected List<LayerRecord> doInBackground() throws Exception {
    final GeometryFactory geometryFactory = this.layer.getGeometryFactory();
    final BoundingBox queryBoundingBox = this.viewportBoundingBox.convert(geometryFactory);
    Query query = this.layer.getQuery();
    final Attribute geometryAttribute = this.layer.getGeometryAttribute();
    if (query != null && geometryAttribute != null
      && !queryBoundingBox.isEmpty()) {
      query = query.clone();
      query.and(F.envelopeIntersects(geometryAttribute, queryBoundingBox));
      final List<LayerRecord> records = this.layer.query(query);
      return records;
    }
    return Collections.emptyList();
  }

  public AbstractLayer getLayer() {
    return this.layer;
  }

  public BoundingBox getViewportBoundingBox() {
    return this.viewportBoundingBox;
  }

  @Override
  public String toString() {
    final String typePath = this.layer.getTypePath();
    return "Loading: " + typePath;
  }

  @Override
  protected void uiTask() {
    try {
      if (!isCancelled()) {
        final List<LayerRecord> records = get();

        this.layer.setRecords(this.viewportBoundingBox, records);
      }
    } catch (final CancellationException e) {
      this.layer.clearLoading(this.viewportBoundingBox);
    } catch (final Throwable t) {
      final String typePath = this.layer.getTypePath();
      LoggerFactory.getLogger(getClass())
        .error("Unable to load " + typePath, t);
      this.layer.clearLoading(this.viewportBoundingBox);
    }
  }
}
