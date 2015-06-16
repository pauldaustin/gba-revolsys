package com.revolsys.swing.map.layer.record.component;

import java.util.List;
import java.util.function.Supplier;

import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.swing.field.AbstractRecordQueryField;
import com.revolsys.swing.field.Field;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;

public class RecordLayerQueryTextField extends AbstractRecordQueryField {

  private static final long serialVersionUID = 1L;

  public static Supplier<Field> factory(final AbstractRecordLayer layer, final String fieldName,
    final String displayFieldName) {
    return () -> {
      return new RecordLayerQueryTextField(fieldName, layer, displayFieldName);
    };
  }

  private final AbstractRecordLayer layer;

  public RecordLayerQueryTextField(final String fieldName, final AbstractRecordLayer layer,
    final String displayFieldName) {
    super(fieldName, layer.getTypePath(), displayFieldName);
    this.layer = layer;
  }

  @Override
  public RecordLayerQueryTextField clone() {
    final String fieldName = getFieldName();
    final String displayFieldName = getDisplayFieldName();
    return new RecordLayerQueryTextField(fieldName, this.layer, displayFieldName);
  }

  @Override
  protected LayerRecord getRecord(final Object identifier) {
    return this.layer.getRecordById(identifier);
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    return this.layer.getRecordDefinition();
  }

  @SuppressWarnings({
    "unchecked", "rawtypes"
  })
  @Override
  protected List<Record> getRecords(final Query query) {
    return (List)this.layer.query(query);
  }
}
