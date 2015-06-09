package com.revolsys.format.esri.gdb.xml.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import com.revolsys.data.codes.CodeTable;

public class CodedValueDomain extends Domain implements CodeTable {
  private List<CodedValue> codedValues = new ArrayList<CodedValue>();

  private Map<Object, List<Object>> idValueMap = new HashMap<Object, List<Object>>();

  private Map<String, Object> stringIdMap = new HashMap<String, Object>();

  private Map<String, Object> valueIdMap = new HashMap<String, Object>();

  private int maxId = 0;

  private JComponent swingEditor;

  public synchronized void addCodedValue(final Object code, final String name) {
    final CodedValue value = new CodedValue(code, name);
    this.codedValues.add(value);
    final List<Object> values = Collections.<Object> singletonList(name);
    this.idValueMap.put(code, values);
    this.stringIdMap.put(code.toString(), code);
    this.valueIdMap.put(name.toLowerCase(), code);
    if (code instanceof Number) {
      final int id = ((Number)code).intValue();
      if (this.maxId < id) {
        this.maxId = id;
      }
    }
  }

  public synchronized Object addCodedValue(final String name) {
    Object id;
    switch (getFieldType()) {
      case esriFieldTypeInteger:
        id = (int)++this.maxId;
        break;
      case esriFieldTypeSmallInteger:
        id = (short)++this.maxId;
        break;

      default:
        throw new RuntimeException("Cannot generate code for field type " + getFieldType());
    }
    addCodedValue(id, name);
    return id;
  }

  @Override
  public CodedValueDomain clone() {
    final CodedValueDomain clone = (CodedValueDomain)super.clone();
    clone.idValueMap = new HashMap<Object, List<Object>>();
    clone.stringIdMap = new HashMap<String, Object>();
    clone.valueIdMap = new HashMap<String, Object>();
    clone.codedValues = new ArrayList<CodedValue>();
    for (final CodedValue codedValue : this.codedValues) {
      clone.addCodedValue(codedValue.getCode(), codedValue.getName());
    }
    return clone;
  }

  @Override
  public List<String> getAttributeAliases() {
    return Collections.emptyList();
  }

  public List<CodedValue> getCodedValues() {
    return this.codedValues;
  }

  @Override
  public Map<Object, List<Object>> getCodes() {
    return Collections.unmodifiableMap(this.idValueMap);
  }

  @Override
  public <T> T getId(final Map<String, ? extends Object> values) {
    final Object name = getName(values);
    return (T)getId(name);
  }

  @Override
  public <T> T getId(final Object... values) {
    if (values.length == 1) {
      final Object value = values[0];
      if (value == null) {
        return null;
      } else if (this.idValueMap.containsKey(value)) {
        return (T)value;
      } else if (this.stringIdMap.containsKey(value.toString())) {
        return (T)this.stringIdMap.get(value.toString());
      } else {
        final String lowerValue = ((String)value).toLowerCase();
        final Object id = this.valueIdMap.get(lowerValue);
        return (T)id;
      }
    } else {
      throw new IllegalArgumentException("Expecting only a single value " + values);
    }
  }

  @Override
  public String getIdAttributeName() {
    return getDomainName() + "_ID";
  }

  @Override
  public Map<String, ? extends Object> getMap(final Object id) {
    final Object value = getValue(id);
    return Collections.singletonMap("NAME", value);
  }

  @Override
  public String getName() {
    return super.getDomainName();
  }

  public String getName(final Map<String, ? extends Object> values) {
    return (String)values.get("NAME");
  }

  @Override
  public JComponent getSwingEditor() {
    return this.swingEditor;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(final Object id) {
    final List<Object> values = getValues(id);
    if (values == null) {
      return null;
    } else {
      final Object value = values.get(0);
      return (V)value;
    }
  }

  @Override
  public List<String> getValueFieldNames() {
    return Arrays.asList("NAME");
  }

  @Override
  public List<Object> getValues(final Object id) {
    if (id == null) {
      return null;
    } else {
      List<Object> values = this.idValueMap.get(id);
      if (values == null) {
        final Object objectId = this.stringIdMap.get(id.toString());
        if (objectId == null) {
          return null;
        } else {
          values = this.idValueMap.get(objectId);
        }
      }
      return Collections.unmodifiableList(values);
    }
  }

  @Override
  public void refresh() {
  }

  public synchronized void setCodedValues(final List<CodedValue> codedValues) {
    this.codedValues = new ArrayList<CodedValue>();
    for (final CodedValue codedValue : codedValues) {
      final Object code = codedValue.getCode();
      final String name = codedValue.getName();
      addCodedValue(code, name);

    }
  }

  public void setSwingEditor(final JComponent swingEditor) {
    this.swingEditor = swingEditor;
  }
}
