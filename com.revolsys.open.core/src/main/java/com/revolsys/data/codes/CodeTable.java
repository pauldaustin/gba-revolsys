package com.revolsys.data.codes;

import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

public interface CodeTable extends Cloneable {
  Map<Object, List<Object>> getCodes();

  List<String> getFieldAliases();

  <T> T getId(final Map<String, ? extends Object> values);

  <T> T getId(final Object... values);

  default Object getIdExact(final Object... values) {
    return getId(values);
  }

  String getIdFieldName();

  Map<String, ? extends Object> getMap(final Object id);

  String getName();

  JComponent getSwingEditor();

  <V> V getValue(final Object id);

  List<String> getValueFieldNames();

  List<Object> getValues(final Object id);

  default boolean isLoaded() {
    return true;
  }

  void refresh();
}
