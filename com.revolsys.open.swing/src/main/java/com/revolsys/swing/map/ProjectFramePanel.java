package com.revolsys.swing.map;

import java.awt.Component;

import javax.swing.Icon;

import com.revolsys.properties.ObjectWithProperties;

public interface ProjectFramePanel extends ObjectWithProperties {
  Component createPanelComponent();

  Icon getIcon();

  String getName();
}
