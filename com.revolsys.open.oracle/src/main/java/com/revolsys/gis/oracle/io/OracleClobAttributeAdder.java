package com.revolsys.gis.oracle.io;

import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.jdbc.field.JdbcFieldAdder;
import com.revolsys.jdbc.io.AbstractJdbcRecordStore;

public class OracleClobAttributeAdder extends JdbcFieldAdder {

  public OracleClobAttributeAdder() {
  }

  @Override
  public FieldDefinition addField(final AbstractJdbcRecordStore recordStore,
    final RecordDefinitionImpl metaData, final String dbName, final String name,
    final String dataTypeName, final int sqlType, final int length, final int scale,
    final boolean required, final String description) {
    final OracleJdbcClobAttribute attribute = new OracleJdbcClobAttribute(dbName, name, sqlType,
      length, required, description);
    metaData.addField(attribute);
    return attribute;
  }

}
