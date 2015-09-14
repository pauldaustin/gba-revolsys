package com.revolsys.record;

import com.revolsys.record.schema.RecordDefinition;

/**
 * The ArrayRecordFactory is an implementation of {@link RecordFactory}
 * for creating {@link ArrayRecord} instances.
 *
 * @author Paul Austin
 * @see ArrayRecord
 */
public class ArrayRecordFactory implements RecordFactory {

  public static final RecordFactory INSTANCE = new ArrayRecordFactory();

  /**
   * Create an instance of ArrayRecord using the recordDefinition
   *
   * @param recordDefinition The recordDefinition used to create the instance.
   * @return The record instance.
   */
  @Override
  public ArrayRecord createRecord(final RecordDefinition recordDefinition) {
    return new ArrayRecord(recordDefinition);
  }
}
