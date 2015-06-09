package com.revolsys.swing.map.layer.record.table.predicate;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComponent;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.color.ColorUtil;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.revolsys.awt.WebColors;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.data.codes.CodeTable;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.table.record.model.RecordRowTableModel;
import com.revolsys.swing.table.record.row.RecordRowTable;

public class ModifiedAttributePredicate implements HighlightPredicate {
  public static void add(final RecordRowTable table) {
    final RecordRowTableModel model = (RecordRowTableModel)table.getModel();
    final ModifiedAttributePredicate predicate = new ModifiedAttributePredicate(model);
    addModifiedHighlighters(table, predicate);
  }

  public static void addModifiedHighlighters(final JXTable table, final HighlightPredicate predicate) {

    table.addHighlighter(new ColorHighlighter(new AndHighlightPredicate(predicate,
      HighlightPredicate.EVEN), ColorUtil.setAlpha(WebColors.YellowGreen, 127), WebColors.Black,
      WebColors.LimeGreen, Color.WHITE));

    table.addHighlighter(new ColorHighlighter(new AndHighlightPredicate(predicate,
      HighlightPredicate.ODD), WebColors.YellowGreen, WebColors.Black, WebColors.Green, Color.WHITE));
  }

  private final RecordRowTableModel model;

  public ModifiedAttributePredicate(final RecordRowTableModel model) {
    this.model = model;
  }

  @Override
  public boolean isHighlighted(final Component renderer, final ComponentAdapter adapter) {
    try {
      final int rowIndex = adapter.convertRowIndexToModel(adapter.row);
      final Record object = this.model.getRecord(rowIndex);
      if (object instanceof LayerRecord) {
        final LayerRecord layerObject = (LayerRecord)object;
        final int columnIndex = adapter.convertColumnIndexToModel(adapter.column);
        final String attributeName = this.model.getFieldName(columnIndex);
        final boolean highlighted = layerObject.isModified(attributeName);
        if (highlighted) {
          final RecordDefinition metaData = layerObject.getRecordDefinition();
          final String fieldName = metaData.getFieldName(columnIndex);
          final Object originalValue = layerObject.getOriginalValue(fieldName);
          final CodeTable codeTable = metaData.getCodeTableByFieldName(fieldName);
          String text;
          if (originalValue == null) {
            text = "-";
          } else if (codeTable == null) {
            text = StringConverterRegistry.toString(originalValue);
          } else {
            text = codeTable.getValue(originalValue);
            if (text == null) {
              text = "-";
            }
          }
          final JComponent component = adapter.getComponent();
          component.setToolTipText(text);
        }
        return highlighted;
      }
    } catch (final IndexOutOfBoundsException e) {
    }
    return false;
  }
}
