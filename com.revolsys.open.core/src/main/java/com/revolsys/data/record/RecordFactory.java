package com.revolsys.data.record;

import com.revolsys.data.record.schema.RecordDefinition;

/**
 * A DataObject factory
 *
 * @author paustin
 */
public interface RecordFactory {
  /**
   * Create an instance of DataObject implementation supported by this factory
   * using the metadata
   *
   * @param metaData The metadata used to create the instance.
   * @return The DataObject instance.
   */
  Record createRecord(RecordDefinition metaData);
}
