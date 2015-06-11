package com.revolsys.swing.tree.datastore;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.tree.TreeNode;

import com.revolsys.data.record.io.RecordStoreConnectionManager;
import com.revolsys.data.record.io.RecordStoreConnectionRegistry;
import com.revolsys.swing.Icons;
import com.revolsys.swing.tree.model.node.AbstractTreeNode;

public class DataObjectStoreConnectionsTreeNode extends AbstractTreeNode {
  public static final Icon ICON = Icons.getIcon("folder_database");

  private ArrayList<TreeNode> children;

  public DataObjectStoreConnectionsTreeNode(final TreeNode parent) {
    super(parent, null);
    setName("Data Stores");
    setType("Data Stores");
    setIcon(ICON);
    setAllowsChildren(true);
    init();
  }

  @Override
  public List<TreeNode> getChildren() {
    return this.children;
  }

  protected void init() {
    this.children = new ArrayList<TreeNode>();
    final List<RecordStoreConnectionRegistry> registries = RecordStoreConnectionManager.get()
        .getVisibleConnectionRegistries();
    for (final RecordStoreConnectionRegistry registry : registries) {
      final DataObjectStoreConnectionRegistryTreeNode child = new DataObjectStoreConnectionRegistryTreeNode(
        this, registry);
      this.children.add(child);
    }
  }

}
