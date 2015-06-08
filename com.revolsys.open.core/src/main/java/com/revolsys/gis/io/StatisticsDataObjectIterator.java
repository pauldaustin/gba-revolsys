package com.revolsys.gis.io;

import com.revolsys.data.record.Record;

public class StatisticsDataObjectIterator implements DataObjectIterator {
  private DataObjectIterator reader;

  private Statistics statistics;

  public StatisticsDataObjectIterator() {
  }

  public StatisticsDataObjectIterator(final DataObjectIterator reader) {
    setReader(reader);
  }

  @Override
  public void close() {
    this.reader.close();
    this.statistics.disconnect();
  }

  /**
   * @return the reader
   */
  public DataObjectIterator getReader() {
    return this.reader;
  }

  /**
   * @return the stats
   */
  public Statistics getStatistics() {
    return this.statistics;
  }

  @Override
  public boolean hasNext() {
    return this.reader.hasNext();
  }

  @Override
  public Record next() {
    final Record object = this.reader.next();
    if (object != null) {
      this.statistics.add(object);
    }
    return object;
  }

  @Override
  public void open() {
    this.reader.open();
  }

  @Override
  public void remove() {
    this.reader.remove();

  }

  /**
   * @param reader the reader to set
   */
  public void setReader(final DataObjectIterator reader) {
    this.reader = reader;
    if (this.statistics == null) {
      setStatistics(new Statistics("Read " + reader.toString()));
    }
  }

  /**
   * @param stats the stats to set
   */
  public void setStatistics(final Statistics statistics) {
    if (this.statistics != null) {

    }
    this.statistics = statistics;
    if (statistics != null) {
      statistics.connect();
    }
  }

  @Override
  public String toString() {
    return this.reader.toString();
  }

}
