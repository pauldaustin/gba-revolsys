package com.revolsys.gis.parallel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.io.StatisticsMap;
import com.revolsys.gis.jts.JtsGeometryUtil;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.process.BaseInOutProcess;
import com.vividsolutions.jts.geom.Geometry;

public class ExcludeSavedObjectsInOutProcess extends
  BaseInOutProcess<Record, Record> {

  private Set<String> originalIds = new HashSet<String>();

  private StatisticsMap statistics = new StatisticsMap(
    "Excluded as already loaded from previous area");

  @Override
  protected void destroy() {
    statistics.disconnect();
    originalIds = null;
    statistics = null;
  }

  private String getId(final Record object) {
    final Object id = object.getIdValue();
    if (id == null) {
      return null;
    } else {
      final RecordDefinition metaData = object.getRecordDefinition();
      return metaData.getPath() + "." + id;
    }
  }

  public StatisticsMap getStatistics() {
    return statistics;
  }

  @Override
  protected void init() {
    statistics.connect();
  }

  @Override
  protected void process(final Channel<Record> in,
    final Channel<Record> out, final Record object) {
    final String id = getId(object);
    if (id == null) {
      out.write(object);
    } else if (originalIds.contains(id.toString())) {
      statistics.add("Excluded as already loaded from previous area", object);
    } else {
      final Set<String> ids = Collections.singleton(id);
      final Geometry geometry = object.getGeometryValue();
      JtsGeometryUtil.setGeometryProperty(geometry, "ORIGINAL_IDS", ids);
      out.write(object);
    }
  }

  public void setObjects(final Collection<? extends Record> objects) {
    for (final Record object : objects) {
      final Set<String> ids = object.getValueByPath("GEOMETRY.ORIGINAL_IDS");
      if (ids != null) {
        originalIds.addAll(ids);
      }
    }
  }

  public void setStatistics(final StatisticsMap statistics) {
    this.statistics = statistics;
  }
}
