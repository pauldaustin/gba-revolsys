package com.revolsys.ui.web.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public final class SiteNode implements Comparable, Cloneable {
  private static final Logger log = Logger.getLogger(SiteNode.class);

  /** The parent node. */
  private SiteNode parent;

  /** The controller for the node. */
  private SiteNodeController controller;

  /** The direct child nodes. */
  private final Collection nodes = new ArrayList();

  /** The map of node names to nodes. */
  private final Map nodeMap = new TreeMap();

  /** The path to the node. */
  private String path;

  /** The map from regular expression to site node. */
  private final Map regexNodes = new HashMap();

  /** The ordered set of site node patterns. */
  private final Set regexPatterns = new LinkedHashSet();

  /**
   * Construct a new root SiteNode.
   */
  public SiteNode() {
  }

  /**
   * Construct a deep copy of the node. Does not retain the reference to the
   * parent node.
   * 
   * @param node The node to copy.
   */
  public SiteNode(final SiteNode node) {
    this.path = node.path;
    if (node.hasController()) {
      this.controller = (SiteNodeController)node.controller.clone();
    }
    setNodes(node.getNodes());

  }

  /**
   * Construct a new SiteNode for the controller.
   * 
   * @param controller The controller.
   */
  public SiteNode(final SiteNodeController controller) {
    this.controller = controller;
  }

  /**
   * Construct a new site node with the specified path.
   * 
   * @param path The path.
   */
  public SiteNode(final String path) {
    this.path = path;
  }

  /**
   * Add the node as a child of this node.
   * 
   * @param node The node.
   */
  protected void addNode(final SiteNode node) {
    node.setParent(this);
    nodeMap.put(node.getPath(), node);
    nodes.add(node);
  }

  /**
   * Add the path for the parent node and this node to the string buffer.
   * 
   * @param buffer The buffer to add the path to.
   */
  private void addPath(final StringBuffer buffer) {
    if (hasParent()) {
      parent.addPath(buffer);
    }
    if (buffer.length() == 0 || buffer.charAt(buffer.length() - 1) != '/') {
      buffer.append("/");
    }
    if (path != null) {
      buffer.append(getPath());
    }
  }

  /**
   * Create a deep copy of this node. Does not retain the reference to the
   * parent node.
   * 
   * @return The cloned node.
   */
  @Override
  protected Object clone() {
    return new SiteNode(this);
  }

  /**
   * Compare this node to another node. The comparison is performed on the path.
   * 
   * @param o The object to compare.
   * @return -1 (less than), 0 (equal) or 1 (greater than).
   */
  public int compareTo(final Object o) {
    if (o instanceof SiteNode) {
      final SiteNode node = (SiteNode)o;
      if (path == null) {
        return -1;
      } else if (node.path == null) {
        return 1;
      } else {
        return path.compareTo(node.path);
      }
    }
    return -1;
  }

  /**
   * Find the site node matching the path.
   * 
   * @param path The path.
   * @return The site node.
   */
  public SiteNode findSiteNode(final String path) {
    SiteNode node = null;
    final int slashIndex = path.indexOf('/');
    if (slashIndex == 0) {
      final String childPath = path.substring(slashIndex + 1);
      node = findSiteNode(childPath);
    } else if (slashIndex != -1) {
      final String childName = path.substring(0, slashIndex);
      final String childPath = path.substring(slashIndex + 1);
      final SiteNode childNode = findSiteNode(childName);
      if (childNode != null) {
        node = childNode.findSiteNode(childPath);
      }
    } else {
      node = getNode(path);
    }
    if (node == null) {
      for (final Iterator nodeIter = regexPatterns.iterator(); nodeIter.hasNext();) {
        final Pattern pattern = (Pattern)nodeIter.next();
        if (pattern.matcher(path).matches()) {
          return (SiteNode)regexNodes.get(pattern.pattern());
        }
      }
      return null;
    } else {
      return node;
    }
  }

  /**
   * Get the controller for this node.
   * 
   * @return The controller for this node.
   */
  public SiteNodeController getController() {
    return controller;
  }

  /**
   * Get the controller for the node specified by the path (see
   * {@link #findSiteNode(String)} for a description how the path is processed.
   * 
   * @param path The path.
   * @return The controller.
   */
  public SiteNodeController getController(final String path) {
    final SiteNode pageNode = findSiteNode(path);
    if (pageNode != null) {
      return pageNode.getController();
    } else {
      return null;
    }
  }

  /**
   * Get the full path to the node, including the path's of all parents.
   * 
   * @return The path.
   */
  public String getFullPath() {
    final StringBuffer path = new StringBuffer();
    addPath(path);
    return path.toString();
  }

  /**
   * Get the child site node with the specified name. The name cannot contain
   * the '/' character, see ({@link #findSiteNode(String)} to get a child node
   * using a path.
   * 
   * @param name The child node name.
   * @return The site node or null if not found.
   */
  public SiteNode getNode(final String name) {
    return (SiteNode)nodeMap.get(name);
  }

  /**
   * Get the collection of child nodes.
   * 
   * @return The collection of nodes.
   */
  public Collection getNodes() {
    return nodes;
  }

  /**
   * @return Returns the parent.
   */
  public SiteNode getParent() {
    return parent;
  }

  /**
   * Get the path for the node relative to the parenr node.
   * 
   * @return The path.
   */
  public String getPath() {
    if (path != null) {
      return path;
    } else if (parent == null) {
      return null;
    } else if (controller != null) {
      return controller.getPath();
    } else {
      return null;
    }
  }

  /**
   * Check to see if this node has a controller.
   * 
   * @return True if the node has a controller.
   */
  public boolean hasController() {
    return controller != null;
  }

  /**
   * Check to see if this node has a parent.
   * 
   * @return True if the node has a parent.
   */
  public boolean hasParent() {
    return parent != null;
  }

  /**
   * Merge the values of the specified node with this node.
   * 
   * @param node The node to merge from.
   */
  protected void mergeNode(final SiteNode node) {
    setNodes(node.getNodes());
    final SiteNodeController controller = node.getController();
    if (controller != null) {
      setController(controller);
    }
  }

  /**
   * Set the controller for this node.
   * 
   * @param controller The controller.
   */
  public void setController(final SiteNodeController controller) {
    this.controller = controller;
  }

  /**
   * Set the controller for the node specified by the path name. If the
   * controller's node already has a parent node, clone the controller so that
   * changing the values on the controller on one path does not affect the
   * other. See {@link #setNode(String, SiteNode)} for details on how the
   * controller for the node is set.
   * 
   * @param path The path.
   * @param controller The controller.
   */
  public void setController(
    final String path,
    final SiteNodeController controller) {
    SiteNode controllerNode = controller.getNode();
    // If the controller's node already has a parent clone it.
    if (controllerNode.hasParent()) {
      final SiteNodeController newController = (SiteNodeController)controller.clone();
      controllerNode = newController.getNode();
    }
    setNode(path, controllerNode);

  }

  /**
   * Set the node for the specified path. A node will be created for each
   * element in the path if one does not exist. If the last element does not
   * exist the node will be set as the node for that element. If it does exist
   * the values for the existing node will be merged from the new node. *
   * 
   * @param path The path.
   * @param node The node.
   */
  public void setNode(final String path, final SiteNode node) {
    if (path != null && path.trim().length() > 0) {
      if (path.startsWith("regex:")) {
        addNode(node);
        final String regex = path.substring(6);
        regexNodes.put(regex, node);
        regexPatterns.add(Pattern.compile(regex));
      } else {
        final String[] names = path.split("/");
        SiteNode currentNode = this;
        for (int i = 0; i < names.length; i++) {
          final String name = names[i].trim();
          if (name.length() > 0) {
            SiteNode childNode = currentNode.getNode(name);
            if (childNode == null) {
              if (i == names.length - 1) {
                // If the last node doesn't exist add the new node as that node
                if (node.hasParent()) {
                  childNode = (SiteNode)node.clone();
                } else {
                  childNode = node;
                }
                childNode.setPath(name);
              } else {
                // Otherwise create a new node
                childNode = new SiteNode(name);
              }
              currentNode.addNode(childNode);
            } else {
              if (i == names.length - 1) {
                // If the last node already exists merge in the values from the
                // new node
                mergeNode(node);
              }
            }
            currentNode = childNode;
          }
        }
      }
    } else {
      mergeNode(node);
    }
  }

  /**
   * Set the child site nodes. The collection can contain {@link SiteNode} or
   * {@link SiteNodeController} instances. If the node's path contains a '/'
   * character a node will be created for each element of the path. For
   * {@link SiteNodeController} instances the node from the controller will be
   * merged with the existing node for the path if a node with that path already
   * existed.
   * 
   * @param nodes The site nodes
   */
  public void setNodes(final Collection nodes) {
    for (final Iterator nodeIter = nodes.iterator(); nodeIter.hasNext();) {
      Object element = nodeIter.next();
      String path = null;
      boolean clone = false;
      if (element instanceof BeanReference) {
        final BeanReference ref = (BeanReference)element;
        element = ref.getReferencedBean();
        clone = true;
        path = ref.getName();
      }
      if (element instanceof SiteNode) {
        SiteNode siteNode = (SiteNode)element;
        if (clone) {
          siteNode = (SiteNode)siteNode.clone();
        }
        if (path == null) {
          path = siteNode.getPath();
        }
        setNode(path, siteNode);
      } else if (element instanceof SiteNodeController) {
        SiteNodeController controller = (SiteNodeController)element;
        if (clone) {
          controller = (SiteNodeController)controller.clone();
        }
        if (path == null) {
          path = controller.getPath();
        }
        setController(path, controller);
      }
    }
  }

  /**
   * @param parent The parent to set.
   */
  public void setParent(final SiteNode parent) {
    this.parent = parent;
  }

  /**
   * @param path The path to set.
   */
  public void setPath(final String path) {
    this.path = path;
  }

  /**
   * Get the string representation of the node.
   * 
   * @return The string representation.
   */
  @Override
  public String toString() {
    return getFullPath();
  }
}
