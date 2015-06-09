package com.revolsys.swing.map.layer.record.table.predicate;

import java.awt.Component;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.revolsys.util.Property;
import com.revolsys.gis.model.data.equals.EqualsRegistry;
import com.revolsys.swing.map.form.RecordLayerForm;
import com.revolsys.swing.map.layer.record.table.model.LayerRecordTableModel;
import com.revolsys.swing.table.BaseJxTable;

public class FormAllFieldsModifiedPredicate implements HighlightPredicate {

  public static void add(final RecordLayerForm form, final BaseJxTable table) {
    final LayerRecordTableModel model = table.getTableModel();
    final FormAllFieldsModifiedPredicate predicate = new FormAllFieldsModifiedPredicate(form, model);
    ModifiedAttributePredicate.addModifiedHighlighters(table, predicate);
  }

  private final LayerRecordTableModel model;

  private final Reference<RecordLayerForm> form;

  public FormAllFieldsModifiedPredicate(final RecordLayerForm form,
    final LayerRecordTableModel model) {
    this.form = new WeakReference<>(form);
    this.model = model;
  }

  @Override
  public boolean isHighlighted(final Component renderer, final ComponentAdapter adapter) {
    try {
      final int rowIndex = adapter.convertRowIndexToModel(adapter.row);
      final String fieldName = this.model.getFieldName(rowIndex);
      if (fieldName != null) {
        final RecordLayerForm form = this.form.get();
        if (form.isFieldValid(fieldName)) {
          if (form.hasOriginalValue(fieldName)) {
            final Object fieldValue = form.getFieldValue(fieldName);
            final Object originalValue = form.getOriginalValue(fieldName);
            boolean equal = EqualsRegistry.equal(originalValue, fieldValue);
            if (!equal) {
              if (originalValue == null) {
                if (fieldValue instanceof String) {
                  final String string = (String)fieldValue;
                  if (!Property.hasValue(string)) {
                    equal = true;
                  }
                }
              }
            }
            return !equal;
          }
        }
      }
    } catch (final IndexOutOfBoundsException e) {
    }
    return false;

  }
}
