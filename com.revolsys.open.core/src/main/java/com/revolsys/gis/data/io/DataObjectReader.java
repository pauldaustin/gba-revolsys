package com.revolsys.gis.data.io;

import com.revolsys.data.record.Record;
import com.revolsys.gis.data.model.RecordDefinition;
import com.revolsys.io.Reader;

public interface DataObjectReader extends Reader<Record> {
  RecordDefinition getMetaData();
}
