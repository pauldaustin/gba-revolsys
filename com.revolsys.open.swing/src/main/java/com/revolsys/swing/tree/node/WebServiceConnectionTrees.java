package com.revolsys.swing.tree.node;

import java.awt.TextField;

import com.revolsys.collection.map.LinkedHashMapEx;
import com.revolsys.collection.map.MapEx;
import com.revolsys.io.connection.ConnectionRegistry;
import com.revolsys.record.io.format.esri.rest.ArcGisRestCatalog;
import com.revolsys.record.io.format.mapguide.MapGuideWebService;
import com.revolsys.swing.Borders;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.component.ValueField;
import com.revolsys.swing.layout.GroupLayouts;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.tree.BaseTreeNode;
import com.revolsys.swing.tree.TreeNodes;
import com.revolsys.webservice.WebServiceConnectionManager;
import com.revolsys.webservice.WebServiceConnectionRegistry;

public class WebServiceConnectionTrees extends ConnectionManagerTrees {

  static {
    final MenuFactory menu = MenuFactory.getMenu(WebServiceConnectionRegistry.class);
    TreeNodes.addMenuItemNodeValue(menu, "default", 0, "Add ArcGIS REST Connection", "world:add",
      ConnectionRegistry::isEditable, WebServiceConnectionTrees::addArcGISRestConnection);
    TreeNodes.addMenuItemNodeValue(menu, "default", 1, "Add MapGuide Connection", "world:add",
      ConnectionRegistry::isEditable, WebServiceConnectionTrees::addMapGuideRestConnection);
  }

  private static void addArcGISRestConnection(final WebServiceConnectionRegistry registry) {
    addWebServiceConnection(registry, ArcGisRestCatalog.J_TYPE);
  }

  private static void addMapGuideRestConnection(final WebServiceConnectionRegistry registry) {
    addWebServiceConnection(registry, MapGuideWebService.J_TYPE);
  }

  private static void addWebServiceConnection(final WebServiceConnectionRegistry registry,
    final String type) {
    final ValueField panel = new ValueField();
    panel.setTitle("Add Web Service Connection");
    Borders.titled(panel, "Web Service Connection");
    SwingUtil.addLabel(panel, "Name");
    final TextField nameField = new TextField(20);
    panel.add(nameField);

    SwingUtil.addLabel(panel, "Service URL");
    final TextField urlField = new TextField(50);
    panel.add(urlField);

    GroupLayouts.makeColumns(panel, 2, true);
    panel.showDialog();
    if (panel.isSaved()) {
      final String url = urlField.getText();
      if (url != null) {
        final String name = nameField.getText();
        final MapEx config = new LinkedHashMapEx();
        config.put("type", type);
        config.put("name", name);
        config.put("serviceUrl", url);
        registry.newConnection(config);
      }
    }
  }

  public static BaseTreeNode newWebServiceConnectionsTreeNode() {
    final WebServiceConnectionManager webServicesConnectionManager = WebServiceConnectionManager
      .get();
    final BaseTreeNode webServices = BaseTreeNode.newTreeNode(webServicesConnectionManager);
    webServices.setOpen(true);
    return webServices;
  }
}
