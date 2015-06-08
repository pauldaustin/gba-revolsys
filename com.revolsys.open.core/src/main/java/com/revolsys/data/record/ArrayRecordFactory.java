package com.revolsys.data.record;

import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.data.model.ArrayRecord;

/**
 * The ArrayRecordFactory is an implementation of {@link RecordFactory}
 * for creating {@link ArrayRecord} instances.
 *
 * @author Paul Austin
 * @see ArrayRecord
 */
public class ArrayRecordFactory implements RecordFactory {

  /**
   * Create an instance of ArrayDataObject using the metadata
   *
   * @param metaData The metadata used to create the instance.
   * @return The DataObject instance.
   */
  @Override
  public ArrayRecord createRecord(final RecordDefinition metaData) {
    return new ArrayRecord(metaData);
  }
}
