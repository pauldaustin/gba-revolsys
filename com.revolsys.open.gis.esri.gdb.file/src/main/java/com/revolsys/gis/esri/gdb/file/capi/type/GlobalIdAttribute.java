package com.revolsys.gis.esri.gdb.file.capi.type;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.gis.esri.gdb.file.capi.swig.Guid;
import com.revolsys.gis.esri.gdb.file.capi.swig.Row;
import com.revolsys.io.esri.gdb.xml.model.Field;

public class GlobalIdAttribute extends AbstractFileGdbAttribute {
  public GlobalIdAttribute(final Field field) {
    this(field.getName(), field.getLength(),
      field.getRequired() == Boolean.TRUE || !field.isIsNullable());
  }

  public GlobalIdAttribute(final String name, final int length,
    final boolean required) {
    super(name, DataTypes.STRING, length, required);
  }

  @Override
  public Object getValue(final Row row) {
    final Guid guid = row.getGlobalId();
    return guid.toString();
  }

  @Override
  public void setPostInsertValue(final DataObject object, final Row row) {
    final Guid guid = row.getGlobalId();
    final String name = getName();
    final String string = guid.toString();
    object.setValue(name, string);
  }

  @Override
  public Object setValue(final DataObject object, final Row row,
    final Object value) {
    return null;
  }

}
