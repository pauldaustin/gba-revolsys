package com.revolsys.gis.io;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;

public class Statistics {
  private final Map<String, Long> counts = new TreeMap<String, Long>();

  private final Logger log;

  private boolean logCounts = true;

  private String message;

  private int providerCount = 0;

  public Statistics() {
    this(null);
  }

  public Statistics(final String message) {
    this(Statistics.class.getName(), message);
  }

  public Statistics(final String category, final String message) {
    this.log = Logger.getLogger(category);
    this.message = message;
  }

  public void add(final Record object) {
    if (object != null) {
      final RecordDefinition type = object.getRecordDefinition();
      add(type);
    }
  }

  public void add(final Record object, final long count) {
    final RecordDefinition type = object.getRecordDefinition();
    add(type, count);

  }

  public void add(final RecordDefinition type) {
    final String path = type.getPath();
    add(path);
  }

  public void add(final RecordDefinition type, final long count) {
    final String path = type.getPath();
    add(path, count);
  }

  public void add(final String name) {
    add(name, 1);
  }

  public synchronized boolean add(final String name, final long count) {
    final Long oldCount = this.counts.get(name);
    if (oldCount == null) {
      this.counts.put(name, count);
      return true;
    } else {
      this.counts.put(name, oldCount + count);
      return false;
    }
  }

  public synchronized void addCountsText(final StringBuffer sb) {
    int totalCount = 0;
    if (this.message != null) {
      sb.append(this.message);
    }
    sb.append("\n");
    for (final Entry<String, Long> entry : this.counts.entrySet()) {
      sb.append(entry.getKey());
      sb.append("\t");
      final Long count = entry.getValue();
      totalCount += count;
      sb.append(count);
      sb.append("\n");
    }
    sb.append("Total");
    sb.append("\t");
    sb.append(totalCount);
    sb.append("\n");
  }

  public synchronized void clearCounts() {
    this.counts.clear();
  }

  public synchronized void clearCounts(final String typeName) {
    this.counts.remove(typeName);
  }

  public synchronized void connect() {
    this.providerCount++;
  }

  public synchronized void disconnect() {
    this.providerCount--;
    if (this.providerCount <= 0) {
      logCounts();
    }
  }

  public synchronized Long get(final String name) {
    if (name != null) {
      final Long count = this.counts.get(name);
      return count;
    } else {
      return null;
    }
  }

  public String getMessage() {
    return this.message;
  }

  public synchronized Set<String> getNames() {
    return this.counts.keySet();
  }

  public boolean isLogCounts() {
    return this.logCounts;
  }

  public synchronized String logCounts() {
    final StringBuffer sb = new StringBuffer();
    addCountsText(sb);
    final String string = sb.toString();
    if (isLogCounts() && !this.counts.isEmpty()) {
      this.log.info(string);
    }
    return string;
  }

  public void setLogCounts(final boolean logCounts) {
    this.logCounts = logCounts;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return this.message;
  }
}
