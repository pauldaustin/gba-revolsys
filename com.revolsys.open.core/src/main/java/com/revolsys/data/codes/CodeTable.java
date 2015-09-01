package com.revolsys.data.codes;

import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import com.revolsys.data.identifier.Identifier;

public interface CodeTable extends Cloneable {
  Map<Object, List<Object>> getCodes();

  List<String> getFieldAliases();

  <T> T getId(final Map<String, ? extends Object> values);

  <T> T getId(final Object... values);

  default Identifier getIdentifier(final Map<String, ? extends Object> values) {
    final Object id = getId(values);
    return Identifier.create(id);
  }

  default Identifier getIdentifier(final Object... values) {
    final Object id = getId(values);
    return Identifier.create(id);
  }

  default Object getIdExact(final Object... values) {
    return getId(values);
  }

  String getIdFieldName();

  Map<String, ? extends Object> getMap(final Object id);

  String getName();

  JComponent getSwingEditor();

  default <V> V getValue(final Identifier id) {
    return getValue(id.getValue(0));
  }

  @SuppressWarnings("unchecked")
  default <V> V getValue(final Object id) {
    final List<Object> values = getValues(id);
    if (values != null) {
      return (V)values.get(0);
    } else {
      return null;
    }
  }

  List<String> getValueFieldNames();

  default List<Object> getValues(final Identifier id) {
    return getValues(id.getValue(0));
  }

  List<Object> getValues(final Object id);

  default boolean isLoaded() {
    return true;
  }

  void refresh();
}
