package com.revolsys.format.gml.type;

import com.revolsys.datatype.DataType;
import com.revolsys.format.xml.XmlWriter;

public class SimpleFieldType extends AbstractGmlFieldType {

  public SimpleFieldType(final DataType dataType) {
    super(dataType, "xs:" + dataType.getName());
  }

  @Override
  protected void writeValueText(final XmlWriter out, final Object value) {
    out.text(value);
  }
}
