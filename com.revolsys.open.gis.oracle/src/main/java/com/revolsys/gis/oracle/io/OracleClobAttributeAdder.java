package com.revolsys.gis.oracle.io;

import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.jdbc.attribute.JdbcAttributeAdder;

public class OracleClobAttributeAdder extends JdbcAttributeAdder {

  public OracleClobAttributeAdder() {
  }

  @Override
  public FieldDefinition addAttribute(final RecordDefinitionImpl metaData,
    final String name, final String dataTypeName, final int sqlType,
    final int length, final int scale, final boolean required,
    final String description) {
    final OracleJdbcClobAttribute attribute = new OracleJdbcClobAttribute(name,
      sqlType, length, required, description);
    metaData.addAttribute(attribute);
    return attribute;
  }

}
