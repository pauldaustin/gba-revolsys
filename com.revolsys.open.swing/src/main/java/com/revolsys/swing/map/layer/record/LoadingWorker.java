package com.revolsys.swing.map.layer.record;

import java.util.List;
import java.util.concurrent.CancellationException;

import org.slf4j.LoggerFactory;

import com.revolsys.data.query.Query;
import com.revolsys.gis.algorithm.index.RecordQuadTree;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.swing.map.layer.AbstractLayer;
import com.revolsys.swing.parallel.AbstractSwingWorker;

public class LoadingWorker extends AbstractSwingWorker<RecordQuadTree, Void> {
  private final RecordStoreLayer layer;

  private final BoundingBox viewportBoundingBox;

  public LoadingWorker(final RecordStoreLayer layer, final BoundingBox viewportBoundingBox) {
    this.layer = layer;
    this.viewportBoundingBox = viewportBoundingBox;

  }

  @Override
  protected RecordQuadTree doInBackground() throws Exception {
    try {
      final RecordQuadTree index = new RecordQuadTree();
      final GeometryFactory geometryFactory = this.layer.getGeometryFactory();
      final BoundingBox queryBoundingBox = this.viewportBoundingBox.convert(geometryFactory);
      Query query = this.layer.getQuery();
      if (query != null) {
        query = query.clone();
        query.setBoundingBox(queryBoundingBox);
        if (!this.layer.isDeleted()) {
          final List<LayerRecord> records = this.layer.query(query);
          index.insertAll(records);
        }
      }
      return index;
    } catch (final Exception e) {
      if (this.layer.isDeleted()) {
        return null;
      } else {
        throw e;
      }
    }
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
        final RecordQuadTree index = get();

        this.layer.setIndex(this.viewportBoundingBox, index);
      }
    } catch (final CancellationException e) {
      this.layer.clearLoading(this.viewportBoundingBox);
    } catch (final Throwable t) {
      final String typePath = this.layer.getTypePath();
      LoggerFactory.getLogger(getClass()).error("Unable to load " + typePath, t);
      this.layer.clearLoading(this.viewportBoundingBox);
    }
  }
}
