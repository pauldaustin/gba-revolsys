package com.revolsys.gis.data.model;

import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.schema.RecordDefinition;

/**
 * The ArrayDataObjectFactory is an implementation of {@link RecordFactory}
 * for creating {@link ArrayRecord} instances.
 * 
 * @author Paul Austin
 * @see ArrayRecord
 */
public class ArrayDataObjectFactory implements RecordFactory {

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
