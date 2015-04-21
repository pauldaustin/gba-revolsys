package com.revolsys.gis.data.model;

import java.util.Map;

import org.apache.commons.jexl.JexlContext;

import com.revolsys.data.record.Record;

public class DataObjectJexlContext implements JexlContext {

  private final DataObjectMap map = new DataObjectMap();

  public DataObjectJexlContext() {
  }

  public DataObjectJexlContext(final Record object) {
    this.map.setObject(object);
  }

  @Override
  public Map getVars() {
    return map;
  }

  public void setObject(final Record object) {
    this.map.setObject(object);
  }

  @Override
  public void setVars(final Map map) {
    this.map.putAll(map);
  }
}
