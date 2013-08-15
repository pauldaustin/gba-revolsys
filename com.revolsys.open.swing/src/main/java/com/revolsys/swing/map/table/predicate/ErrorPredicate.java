package com.revolsys.swing.map.table.predicate;

import java.awt.Color;
import java.awt.Component;

import org.jdesktop.swingx.color.ColorUtil;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.swing.table.dataobject.row.DataObjectRowTable;
import com.revolsys.swing.table.dataobject.row.DataObjectRowTableModel;

public class ErrorPredicate implements HighlightPredicate {

  public static void add(final DataObjectRowTable table) {
    final DataObjectRowTableModel model = (DataObjectRowTableModel)table.getModel();
    final Highlighter highlighter = getHighlighter(model);
    table.addHighlighter(highlighter);
  }

  public static Highlighter getHighlighter(final DataObjectRowTableModel model) {
    final ErrorPredicate predicate = new ErrorPredicate(model);
    return new ColorHighlighter(predicate, ColorUtil.setAlpha(Color.RED, 64),
      Color.RED, Color.RED, Color.YELLOW);
  }

  private final DataObjectRowTableModel model;

  public ErrorPredicate(final DataObjectRowTableModel model) {
    this.model = model;
  }

  @Override
  public boolean isHighlighted(final Component renderer,
    final ComponentAdapter adapter) {
    try {
      final int rowIndex = adapter.convertRowIndexToModel(adapter.row);
      final DataObject object = model.getObject(rowIndex);
      if (object != null) {
        final int columnIndex = adapter.convertRowIndexToModel(adapter.column);
        if (!object.isValid(columnIndex)) {
          return true;
        }
      }
    } catch (final IndexOutOfBoundsException e) {
    }
    return false;
  }
}
