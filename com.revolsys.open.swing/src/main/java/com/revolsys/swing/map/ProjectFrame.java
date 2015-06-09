package com.revolsys.swing.map;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.net.ResponseCache;
import java.util.List;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.TreePath;

import org.springframework.core.io.FileSystemResource;

import com.revolsys.io.datastore.DataObjectStoreConnectionManager;
import com.revolsys.io.datastore.DataObjectStoreConnectionRegistry;
import com.revolsys.io.file.FolderConnectionManager;
import com.revolsys.io.map.MapObjectFactoryRegistry;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.net.urlcache.FileResponseCache;
import com.revolsys.swing.Icons;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.WindowManager;
import com.revolsys.swing.action.InvokeMethodAction;
import com.revolsys.swing.component.BaseFrame;
import com.revolsys.swing.component.ButtonTabComponent;
import com.revolsys.swing.listener.InvokeMethodPropertyChangeListener;
import com.revolsys.swing.logging.Log4jTableModel;
import com.revolsys.swing.map.component.layerchooser.LayerChooserPanel;
import com.revolsys.swing.map.layer.Layer;
import com.revolsys.swing.map.layer.LayerGroup;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.map.layer.arcgisrest.ArcGisServerRestLayer;
import com.revolsys.swing.map.layer.bing.BingLayer;
import com.revolsys.swing.map.layer.dataobject.DataObjectFileLayer;
import com.revolsys.swing.map.layer.dataobject.DataObjectStoreLayer;
import com.revolsys.swing.map.layer.geonames.GeoNamesBoundingBoxLayerWorker;
import com.revolsys.swing.map.layer.grid.GridLayer;
import com.revolsys.swing.map.layer.openstreetmap.OpenStreetMapLayer;
import com.revolsys.swing.map.layer.raster.GeoreferencedImageLayer;
import com.revolsys.swing.map.layer.wikipedia.WikipediaBoundingBoxLayerWorker;
import com.revolsys.swing.map.tree.ProjectTreeNodeModel;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.parallel.Invoke;
import com.revolsys.swing.parallel.SwingWorkerProgressBar;
import com.revolsys.swing.preferences.PreferencesDialog;
import com.revolsys.swing.table.worker.SwingWorkerTableModel;
import com.revolsys.swing.toolbar.ToolBar;
import com.revolsys.swing.tree.BaseTree;
import com.revolsys.swing.tree.ObjectTree;
import com.revolsys.swing.tree.ObjectTreePanel;
import com.revolsys.swing.tree.model.ObjectTreeModel;
import com.revolsys.util.ExceptionUtil;
import com.revolsys.util.OS;
import com.revolsys.util.PreferencesUtil;
import com.revolsys.util.Property;

public class ProjectFrame extends BaseFrame {
  public static final String SAVE_PROJECT_KEY = "Save Project";

  public static final String SAVE_CHANGES_KEY = "Save Changes";

  private static final long serialVersionUID = 1L;

  static {
    ResponseCache.setDefault(new FileResponseCache());

    // TODO move to a file config
    MapObjectFactoryRegistry.addFactory(LayerGroup.FACTORY);
    MapObjectFactoryRegistry.addFactory(DataObjectStoreLayer.FACTORY);
    MapObjectFactoryRegistry.addFactory(ArcGisServerRestLayer.FACTORY);
    MapObjectFactoryRegistry.addFactory(BingLayer.FACTORY);
    MapObjectFactoryRegistry.addFactory(OpenStreetMapLayer.FACTORY);
    MapObjectFactoryRegistry.addFactory(DataObjectFileLayer.FACTORY);
    MapObjectFactoryRegistry.addFactory(GridLayer.FACTORY);
    MapObjectFactoryRegistry.addFactory(WikipediaBoundingBoxLayerWorker.FACTORY);
    MapObjectFactoryRegistry.addFactory(GeoNamesBoundingBoxLayerWorker.FACTORY);
    MapObjectFactoryRegistry.addFactory(GeoreferencedImageLayer.FACTORY);

  }

  // public void expandConnectionManagers(final PropertyChangeEvent event) {
  // final Object newValue = event.getNewValue();
  // if (newValue instanceof ConnectionRegistry) {
  // final ConnectionRegistry<?> registry = (ConnectionRegistry<?>)newValue;
  // final ConnectionRegistryManager<?> connectionManager =
  // registry.getConnectionManager();
  // if (connectionManager != null) {
  // final List<?> connectionRegistries =
  // connectionManager.getConnectionRegistries();
  // if (connectionRegistries != null) {
  // final ObjectTree tree = catalogPanel.getTree();
  // tree.expandPath(connectionRegistries, connectionManager, registry);
  // }
  // }
  // }
  // }
  public static final String PROJECT_FRAME = "projectFrame";

  public static void addSaveActions(final JComponent component, final Project project) {
    final InputMap inputMap = component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), SAVE_PROJECT_KEY);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.META_DOWN_MASK), SAVE_PROJECT_KEY);

    inputMap.put(
      KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK),
      SAVE_CHANGES_KEY);
    inputMap.put(
      KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK),
      SAVE_CHANGES_KEY);

    final ActionMap actionMap = component.getActionMap();
    actionMap.put(SAVE_PROJECT_KEY, new InvokeMethodAction(SAVE_PROJECT_KEY, project,
      "saveAllSettings"));
    actionMap.put(SAVE_CHANGES_KEY,
      new InvokeMethodAction(SAVE_CHANGES_KEY, project, "saveChanges"));
  }

  public static ProjectFrame get(final Layer layer) {
    if (layer == null) {
      return null;
    } else {
      final LayerGroup project = layer.getProject();
      if (project == null) {
        return null;
      } else {
        return project.getProperty(PROJECT_FRAME);
      }
    }
  }

  private ObjectTreePanel tocPanel;

  private Project project;

  private MapPanel mapPanel;

  private BaseTree catalogTree;

  private boolean exitOnClose = true;

  private JTabbedPane leftTabs = new JTabbedPane();

  private JTabbedPane bottomTabs = new JTabbedPane();

  private JSplitPane leftRightSplit;

  private JSplitPane topBottomSplit;

  public ProjectFrame(final String title) {
    this(title, new Project());
    setVisible(true);
  }

  public ProjectFrame(final String title, final File projectDirectory) {
    this(title);
    Invoke.background("Load project", this, "loadProject", projectDirectory);
  }

  public ProjectFrame(final String title, final Project project) {
    super(title, false);
    this.project = project;
    init();
  }

  @SuppressWarnings("unchecked")
  public <C extends Component> C addBottomTab(final ProjectFramePanel panel) {
    final JTabbedPane tabs = getBottomTabs();

    final Object tableView = panel.getProperty("bottomTab");
    Component component = null;
    if (tableView instanceof Component) {
      component = (Component)tableView;
      if (component.getParent() != tabs) {
        component = null;
      }
    }
    if (component == null) {
      component = panel.createPanelComponent();

      if (component != null) {
        final int tabIndex = tabs.getTabCount();
        final String name = panel.getName();
        tabs.addTab(name, panel.getIcon(), component);
        tabs.setTabComponentAt(tabIndex, new ButtonTabComponent(tabs));
        final Component tabComponent = component;
        panel.setPropertyWeak("bottomTab", tabComponent);
        tabs.setSelectedIndex(tabIndex);
      }
    } else {
      tabs.setSelectedComponent(component);
    }
    return (C)component;
  }

  protected void addCatalogPanel() {
    this.catalogTree = LayerChooserPanel.createTree();

    addTabIcon(this.leftTabs, "tree_catalog", "Catalog", this.catalogTree, true);
  }

  protected void addLogPanel() {
    final JPanel panel = Log4jTableModel.createPanel();
    addTabIcon(this.bottomTabs, "error", "Logging", panel, false);
  }

  protected MapPanel addMapPanel() {
    this.mapPanel = new MapPanel(this.project);
    if (OS.isMac()) {
      // Make border on right/bottom to match the JTabbedPane UI on a mac
      this.mapPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 9, 9));
    }
    return this.mapPanel;
  }

  protected void addMenu(final JMenuBar menuBar, final MenuFactory menu) {
    final JMenu fileMenu = menu.createComponent();
    menuBar.add(fileMenu);
  }

  public int addTabIcon(final JTabbedPane tabs, final String iconName, final String toolTipText,
    Component component, final boolean useScrollPane) {

    if (useScrollPane) {
      final JScrollPane scrollPane = new JScrollPane(component);
      scrollPane.setBorder(BorderFactory.createEmptyBorder());
      component = scrollPane;
    }

    tabs.addTab(null, Icons.getIcon(iconName), component);
    final int tabIndex = tabs.getTabCount() - 1;
    tabs.setToolTipTextAt(tabIndex, toolTipText);
    return tabIndex;
  }

  protected void addTableOfContents() {
    final JPanel panel = new JPanel(new BorderLayout());

    final ToolBar toolBar = new ToolBar();
    panel.add(toolBar, BorderLayout.NORTH);

    final ProjectTreeNodeModel model = new ProjectTreeNodeModel();
    this.tocPanel = new ObjectTreePanel(this.project, model);
    final ObjectTree tree = this.tocPanel.getTree();
    tree.setRootVisible(true);

    Property.addListener(this.project, "layers", new InvokeMethodPropertyChangeListener(true, this,
      "expandLayers", PropertyChangeEvent.class));
    panel.add(this.tocPanel, BorderLayout.CENTER);

    addTabIcon(this.leftTabs, "tree_layers", "TOC", panel, true);
  }

  protected void addTasksPanel() {
    final JPanel panel = SwingWorkerTableModel.createPanel();
    final int tabIndex = addTabIcon(this.bottomTabs, "time", "Background Tasks", panel, false);

    final SwingWorkerProgressBar progressBar = this.mapPanel.getProgressBar();
    final JButton viewTasksAction = InvokeMethodAction.createButton(null, "View Running Tasks",
      Icons.getIcon("time_go"), this.bottomTabs, "setSelectedIndex", tabIndex);
    viewTasksAction.setBorderPainted(false);
    viewTasksAction.setBorder(null);
    progressBar.add(viewTasksAction, BorderLayout.EAST);
  }

  protected JMenuBar createMenuBar() {
    final JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);

    addMenu(menuBar, createMenuFile());

    final MenuFactory tools = createMenuTools();

    if (OS.isWindows()) {
      tools.addMenuItem("options", "Options...", "Options...", null, null, PreferencesDialog.get(),
        "showPanel");
    }
    addMenu(menuBar, tools);
    WindowManager.addMenu(menuBar);
    return menuBar;
  }

  protected MenuFactory createMenuFile() {
    final MenuFactory file = new MenuFactory("File");

    file.addMenuItem("project", "Save Project", "Save Project", Icons.getIcon("layout_save"),
      this.project, "saveAllSettings");
    file.addMenuItemTitleIcon("exit", "Exit", null, this, "exit");
    return file;
  }

  protected MenuFactory createMenuTools() {
    final MenuFactory tools = new MenuFactory("Tools");

    tools.addMenuItem("script", "Run Script...", "Run Script", Icons.getIcon("script_go"), this,
      "runScript");
    return tools;
  }

  @Override
  public void dispose() {
    if (SwingUtilities.isEventDispatchThread()) {
      setVisible(false);
      super.dispose();
      Property.removeAllListeners(this);
      if (this.project != null) {
        final DataObjectStoreConnectionRegistry dataStores = this.project.getDataStores();
        DataObjectStoreConnectionManager.get().removeConnectionRegistry(dataStores);
        if (Project.get() == this.project) {
          Project.set(null);
        }
      }
      if (this.tocPanel != null) {
        this.tocPanel.destroy();
      }

      this.tocPanel = null;
      this.leftTabs = null;
      this.bottomTabs = null;
      this.project = null;
      this.leftRightSplit = null;
      this.topBottomSplit = null;
      if (this.mapPanel != null) {
        this.mapPanel.destroy();
        this.mapPanel = null;
      }
      setMenuBar(null);
      final ActionMap actionMap = getRootPane().getActionMap();
      actionMap.put(SAVE_PROJECT_KEY, null);
      actionMap.put(SAVE_CHANGES_KEY, null);

      setRootPane(new JRootPane());
      removeAll();
    } else {
      Invoke.later(this, "dispose");
    }
  }

  public void exit() {
    final Project project = getProject();
    if (project != null && project.saveWithPrompt()) {
      final Window[] windows = Window.getOwnerlessWindows();
      for (final Window window : windows) {
        window.dispose();
      }
      System.exit(0);
    }
  }

  public void expandLayers(final Layer layer) {
    if (SwingUtilities.isEventDispatchThread()) {
      final List<Layer> pathList = layer.getPathList();
      final ObjectTree tree = this.tocPanel.getTree();
      final TreePath treePath = ObjectTree.createTreePath(pathList);
      if (layer instanceof LayerGroup) {
        final LayerGroup layerGroup = (LayerGroup)layer;
        tree.expandPath(treePath);
        for (final Layer childLayer : layerGroup) {
          expandLayers(childLayer);
        }
      } else {
        final ObjectTreeModel model = tree.getModel();
        model.fireTreeNodesChanged(treePath);
      }
    } else {
      Invoke.later(this, "expandLayers", layer);
    }
  }

  public void expandLayers(final PropertyChangeEvent event) {
    final Object source = event.getSource();
    if (source instanceof LayerGroup) {
      final Object newValue = event.getNewValue();
      if (newValue instanceof LayerGroup) {
        expandLayers((LayerGroup)newValue);
      }
    }
  }

  public JTabbedPane getBottomTabs() {
    return this.bottomTabs;
  }

  public double getControlWidth() {
    return 0.20;
  }

  public JTabbedPane getLeftTabs() {
    return this.leftTabs;
  }

  public MapPanel getMapPanel() {
    return this.mapPanel;
  }

  public Project getProject() {
    return this.project;
  }

  public ObjectTreePanel getTocPanel() {
    return this.tocPanel;
  }

  @Override
  protected void init() {
    setMinimumSize(new Dimension(600, 500));

    final JRootPane rootPane = getRootPane();

    addSaveActions(rootPane, this.project);

    Project.set(this.project);
    this.project.setProperty(PROJECT_FRAME, this);
    setLocationByPlatform(true);

    addMapPanel();

    this.leftTabs.setMinimumSize(new Dimension(100, 300));
    this.leftTabs.setPreferredSize(new Dimension(300, 700));

    this.mapPanel.setMinimumSize(new Dimension(300, 300));
    this.mapPanel.setPreferredSize(new Dimension(700, 700));
    this.leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.leftTabs, this.mapPanel);

    this.leftRightSplit.setBorder(BorderFactory.createEmptyBorder());
    this.bottomTabs.setBorder(BorderFactory.createEmptyBorder());
    this.bottomTabs.setPreferredSize(new Dimension(700, 200));

    this.topBottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.leftRightSplit,
      this.bottomTabs);
    this.bottomTabs.setMinimumSize(new Dimension(600, 100));

    this.topBottomSplit.setResizeWeight(1);

    add(this.topBottomSplit, BorderLayout.CENTER);

    addTableOfContents();
    addCatalogPanel();

    addTasksPanel();
    addLogPanel();
    setBounds((Object)null);

    createMenuBar();
    super.init();
  }

  public void loadProject(final File projectDirectory) {
    final FileSystemResource resource = new FileSystemResource(projectDirectory);
    this.project.readProject(resource);

    final Object frameBoundsObject = this.project.getProperty("frameBounds");
    setBounds(frameBoundsObject);

    final DataObjectStoreConnectionManager dataStoreConnectionManager = DataObjectStoreConnectionManager.get();
    dataStoreConnectionManager.removeConnectionRegistry("Project");
    dataStoreConnectionManager.addConnectionRegistry(this.project.getDataStores());

    final FolderConnectionManager folderConnectionManager = FolderConnectionManager.get();
    folderConnectionManager.removeConnectionRegistry("Project");
    folderConnectionManager.addConnectionRegistry(this.project.getFolderConnections());

    final BoundingBox initialBoundingBox = this.project.getInitialBoundingBox();
    if (!BoundingBox.isEmpty(initialBoundingBox)) {
      this.project.setGeometryFactory(initialBoundingBox.getGeometryFactory());
      this.project.setViewBoundingBox(initialBoundingBox);
    }
    getMapPanel().getViewport().setInitialized(true);
  }

  public void removeBottomTab(final ProjectFramePanel panel) {
    final JTabbedPane tabs = getBottomTabs();
    final Component component = panel.getProperty("bottomTab");
    if (component != null) {
      if (tabs != null) {
        tabs.remove(component);
      }
      panel.setProperty("bottomTab", null);
    }
  }

  public void runScript() {
    final JFileChooser fileChooser = SwingUtil.createFileChooser("Select Script",
      "com.revolsys.swing.tools.script", "directory");
    final FileNameExtensionFilter groovyFilter = new FileNameExtensionFilter("Groovy Script",
      "groovy");
    fileChooser.addChoosableFileFilter(groovyFilter);
    fileChooser.setMultiSelectionEnabled(false);
    final int returnVal = fileChooser.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {

      final Binding binding = new Binding();
      final GroovyShell shell = new GroovyShell(binding);
      final File scriptFile = fileChooser.getSelectedFile();
      final String[] args = new String[0];
      try {
        PreferencesUtil.setUserString("com.revolsys.swing.tools.script", "directory",
          scriptFile.getParent());
        shell.run(scriptFile, args);
      } catch (final Throwable e) {
        ExceptionUtil.log(getClass(), "Unable to run script:" + scriptFile, e);
      }
    }
  }

  public void setBounds(final Object frameBoundsObject) {
    if (SwingUtilities.isEventDispatchThread()) {
      boolean sizeSet = false;
      if (frameBoundsObject instanceof List) {
        try {
          @SuppressWarnings("unchecked")
          final List<Number> frameBoundsList = (List<Number>)frameBoundsObject;
          if (frameBoundsList.size() == 4) {
            int x = frameBoundsList.get(0).intValue();
            int y = frameBoundsList.get(1).intValue();
            int width = frameBoundsList.get(2).intValue();
            int height = frameBoundsList.get(3).intValue();

            final Rectangle screenBounds = SwingUtil.getScreenBounds(x, y);

            width = Math.min(width, screenBounds.width);
            height = Math.min(height, screenBounds.height);
            setSize(width, height);

            if (x < screenBounds.x || x > screenBounds.x + screenBounds.width) {
              x = 0;
            } else {
              x = Math.min(x, screenBounds.x + screenBounds.width - width);
            }
            if (y < screenBounds.y || x > screenBounds.y + screenBounds.height) {
              y = 0;
            } else {
              y = Math.min(y, screenBounds.y + screenBounds.height - height);
            }
            setLocation(x, y);
            sizeSet = true;
          }
        } catch (final Throwable t) {
        }
      }
      if (!sizeSet) {
        final Rectangle screenBounds = SwingUtil.getScreenBounds();
        setLocation(screenBounds.x + 10, screenBounds.y + 10);
        setSize(screenBounds.width - 20, screenBounds.height - 20);
      }
      final int leftRightDividerLocation = (int)(getWidth() * 0.2);
      this.leftRightSplit.setDividerLocation(leftRightDividerLocation);

      final int topBottomDividerLocation = (int)(getHeight() * 0.75);
      this.topBottomSplit.setDividerLocation(topBottomDividerLocation);
    } else {
      if (frameBoundsObject == null) {
        Invoke.later(this, "setBounds", new Object());
      } else {
        Invoke.later(this, "setBounds", frameBoundsObject);
      }
    }
  }

  public void setExitOnClose(final boolean exitOnClose) {
    this.exitOnClose = exitOnClose;
  }

  @Override
  public void windowClosing(final WindowEvent e) {
    if (this.exitOnClose) {
      exit();
    } else {
      dispose();
    }
  }

}
