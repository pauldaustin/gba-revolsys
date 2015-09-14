package com.revolsys.record;

import com.revolsys.record.schema.RecordDefinition;

/**
 * A record factory
 *
 * @author paustin
 */
public interface RecordFactory {
  /**
   * Create an instance of record implementation supported by this factory
   * using the metadata
   *
   * @param recordDefinition The metadata used to create the instance.
   * @return The record instance.
   */
  Record createRecord(RecordDefinition recordDefinition);
}
