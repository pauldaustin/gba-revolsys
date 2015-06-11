package com.revolsys.gis.esri.gdb.file.capi;

import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.revolsys.data.codes.CodeTable;
import com.revolsys.format.esri.gdb.xml.model.CodedValueDomain;
import com.revolsys.format.esri.gdb.xml.model.Domain;
import com.revolsys.gis.esri.gdb.file.FileGdbRecordStore;

public class FileGdbDomainCodeTable implements CodeTable {
  private static final Logger LOG = LoggerFactory.getLogger(FileGdbDomainCodeTable.class);

  private final CodedValueDomain domain;

  private final String name;

  private final FileGdbRecordStore recordStore;

  private JComponent swingEditor;

  public FileGdbDomainCodeTable(final FileGdbRecordStore recordStore, final CodedValueDomain domain) {
    this.recordStore = recordStore;
    this.domain = domain;
    this.name = domain.getDomainName();
  }

  @Override
  public FileGdbDomainCodeTable clone() {
    try {
      return (FileGdbDomainCodeTable)super.clone();
    } catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  private Object createValue(final String name) {
    synchronized (this.recordStore) {
      final Object id = this.domain.addCodedValue(name);
      this.recordStore.alterDomain(this.domain);
      LOG.info(this.domain.getDomainName() + " created code " + id + "=" + name);
      return id;
    }
  }

  @Override
  public List<String> getFieldAliases() {
    return this.domain.getFieldAliases();
  }

  @Override
  public Map<Object, List<Object>> getCodes() {
    return this.domain.getCodes();
  }

  public Domain getDomain() {
    return this.domain;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getId(final Map<String, ? extends Object> values) {
    final Object id = this.domain.getId(values);
    if (id == null) {
      return (T)createValue(this.domain.getName(values));
    }
    return (T)id;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getId(final Object... values) {
    final Object id = this.domain.getId(values);
    if (id == null) {
      return (T)createValue((String)values[0]);
    }
    return (T)id;
  }

  @Override
  public String getIdFieldName() {
    return this.domain.getIdFieldName();
  }

  @Override
  public Map<String, ? extends Object> getMap(final Object id) {
    return this.domain.getMap(id);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public JComponent getSwingEditor() {
    return this.swingEditor;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(final Object id) {
    return (V)this.domain.getValue(id);
  }

  @Override
  public List<String> getValueFieldNames() {
    return this.domain.getValueFieldNames();
  }

  @Override
  public List<Object> getValues(final Object id) {
    return this.domain.getValues(id);
  }

  @Override
  public void refresh() {
  }

  public void setSwingEditor(final JComponent swingEditor) {
    this.swingEditor = swingEditor;
  }

  @Override
  public String toString() {
    return this.domain.toString();
  }
}
