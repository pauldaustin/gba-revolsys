package com.revolsys.swing.field;

import java.awt.Color;

import javax.swing.ImageIcon;
import javax.swing.JTextField;

import com.revolsys.swing.Icons;
import com.revolsys.swing.undo.UndoManager;

public interface Field extends Cloneable {

  Color DEFAULT_BACKGROUND = new JTextField().getBackground();

  Color DEFAULT_FOREGROUND = new JTextField().getForeground();

  Color DEFAULT_SELECTED_FOREGROUND = new JTextField().getSelectedTextColor();

  ImageIcon ERROR_ICON = Icons.getIcon("exclamation");

  Field clone();

  void firePropertyChange(String propertyName, Object oldValue, Object newValue);

  String getFieldName();

  String getFieldValidationMessage();

  <T> T getFieldValue();

  boolean isFieldValid();

  default void setEditable(final boolean editable) {
    setEnabled(editable);
  }

  void setEnabled(boolean enabled);

  void setFieldBackgroundColor(Color color);

  void setFieldForegroundColor(Color color);

  void setFieldInvalid(String message, Color foregroundColor, Color backgroundColor);

  void setFieldToolTip(String toolTip);

  void setFieldValid();

  void setFieldValue(Object value);

  void setUndoManager(UndoManager undoManager);

  void updateFieldValue();
}
