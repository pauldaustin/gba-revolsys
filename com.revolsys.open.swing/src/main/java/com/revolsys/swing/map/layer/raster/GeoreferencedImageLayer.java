package com.revolsys.swing.map.layer.raster;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.DoubleCoordinates;
import com.revolsys.io.FileNames;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.map.InvokeMethodMapObjectFactory;
import com.revolsys.io.map.MapObjectFactory;
import com.revolsys.io.map.MapSerializerUtil;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.raster.AbstractGeoreferencedImageFactory;
import com.revolsys.raster.GeoreferencedImage;
import com.revolsys.raster.GeoreferencedImageFactory;
import com.revolsys.raster.MappedLocation;
import com.revolsys.spring.resource.SpringUtil;
import com.revolsys.swing.Icons;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.action.enablecheck.AndEnableCheck;
import com.revolsys.swing.action.enablecheck.EnableCheck;
import com.revolsys.swing.component.BasePanel;
import com.revolsys.swing.component.TabbedValuePanel;
import com.revolsys.swing.component.ValueField;
import com.revolsys.swing.layout.GroupLayouts;
import com.revolsys.swing.map.layer.AbstractLayer;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.parallel.Invoke;
import com.revolsys.swing.tree.MenuSourcePropertyEnableCheck;
import com.revolsys.swing.tree.MenuSourceRunnable;
import com.revolsys.util.Property;
import com.vividsolutions.jts.geom.Point;

public class GeoreferencedImageLayer extends AbstractLayer {

  public static final MapObjectFactory FACTORY = new InvokeMethodMapObjectFactory(
    "geoReferencedImage", "Geo-referenced Image", GeoreferencedImageLayer.class, "create");

  static {
    final MenuFactory menu = MenuFactory.getMenu(GeoreferencedImageLayer.class);
    menu.addGroup(0, "table");
    menu.addGroup(2, "edit");

    final EnableCheck readonly = new MenuSourcePropertyEnableCheck("readOnly", false);
    final EnableCheck editable = new MenuSourcePropertyEnableCheck("editable");
    final EnableCheck showOriginalImage = new MenuSourcePropertyEnableCheck("showOriginalImage");
    final EnableCheck hasTransform = new MenuSourcePropertyEnableCheck("hasTransform");

    menu.addMenuItem("table",
      MenuSourceRunnable.createAction("View Tie-Points", "table_go", "showTiePointsTable"));

    menu.addCheckboxMenuItem("edit",
      MenuSourceRunnable.createAction("Editable", "pencil", readonly, "toggleEditable"), editable);

    final EnableCheck hasChanges = new MenuSourcePropertyEnableCheck("hasChanges");
    menu.addMenuItem("edit",
      MenuSourceRunnable.createAction("Save Changes", "map_save", hasChanges, "saveChanges"));

    menu.addMenuItem("edit",
      MenuSourceRunnable.createAction("Cancel Changes", "map_cancel", "cancelChanges"));

    menu
      .addCheckboxMenuItem("edit",
        MenuSourceRunnable.createAction("Show Original Image", (String)null,
          new AndEnableCheck(editable, hasTransform), "toggleShowOriginalImage"),
      showOriginalImage);

    menu.addMenuItem("edit",
      MenuSourceRunnable.createAction("Fit to Screen", "arrow_out", editable, "fitToViewport"));

    menu.deleteMenuItem("refresh", "Refresh");
  }

  public static GeoreferencedImageLayer create(final Map<String, Object> properties) {
    return new GeoreferencedImageLayer(properties);
  }

  private GeoreferencedImage image;

  private Resource resource;

  private boolean showOriginalImage = true;

  private String url;

  public GeoreferencedImageLayer(final Map<String, Object> properties) {
    super(properties);
    setType("geoReferencedImage");
    setSelectSupported(false);
    setQuerySupported(false);
    setRenderer(new GeoreferencedImageLayerRenderer(this));
    setIcon(Icons.getIcon("picture"));
  }

  @Override
  protected ValueField addPropertiesTabGeneralPanelSource(final BasePanel parent) {
    final ValueField panel = super.addPropertiesTabGeneralPanelSource(parent);

    if (this.url.startsWith("file:")) {
      final String fileName = this.url.replaceFirst("file:(//)?", "");
      SwingUtil.addReadOnlyTextField(panel, "File", fileName);
    } else {
      SwingUtil.addReadOnlyTextField(panel, "URL", this.url);
    }
    final String fileNameExtension = FileNames.getFileNameExtension(this.url);
    if (Property.hasValue(fileNameExtension)) {
      SwingUtil.addReadOnlyTextField(panel, "File Extension", fileNameExtension);
      final GeoreferencedImageFactory factory = IoFactoryRegistry.getInstance()
        .getFactoryByFileExtension(GeoreferencedImageFactory.class, fileNameExtension);
      if (factory != null) {
        SwingUtil.addReadOnlyTextField(panel, "File Type", factory.getName());
      }
    }
    GroupLayouts.makeColumns(panel, 2, true);
    return panel;
  }

  public void cancelChanges() {
    if (this.image == null && this.resource != null) {
      GeoreferencedImage image = null;
      final Resource imageResource = SpringUtil.getResource(this.url);
      if (imageResource.exists()) {
        try {
          image = AbstractGeoreferencedImageFactory.loadGeoreferencedImage(imageResource);
          if (image == null) {
            LoggerFactory.getLogger(GeoreferencedImageLayer.class)
              .error("Cannot load image: " + this.url);
          }
        } catch (final RuntimeException e) {
          LoggerFactory.getLogger(GeoreferencedImageLayer.class)
            .error("Unable to load image: " + this.url, e);
        }
      } else {
        LoggerFactory.getLogger(GeoreferencedImageLayer.class)
          .error("Image does not exist: " + this.url);
      }
      setImage(image);
    } else {
      this.image.cancelChanges();
    }
    firePropertyChange("hasChanges", true, false);
  }

  @Override
  public TabbedValuePanel createPropertiesPanel() {
    final TabbedValuePanel propertiesPanel = super.createPropertiesPanel();
    final TiePointsPanel tiePointsPanel = new TiePointsPanel(this);
    SwingUtil.setTitledBorder(tiePointsPanel, "Tie Points");

    propertiesPanel.addTab("Geo-Referencing", tiePointsPanel);
    return propertiesPanel;
  }

  @Override
  protected TiePointsPanel createTableViewComponent() {
    return new TiePointsPanel(this);
  }

  @Override
  protected boolean doInitialize() {
    final String url = getProperty("url");
    if (Property.hasValue(url)) {
      this.url = url;
      this.resource = SpringUtil.getResource(url);
      cancelChanges();
      return true;
    } else {
      LoggerFactory.getLogger(getClass())
        .error("Layer definition does not contain a 'url' property");
      return false;
    }
  }

  @Override
  protected boolean doSaveChanges() {
    if (this.image == null) {
      return true;
    } else {
      return this.image.saveChanges();
    }
  }

  public BoundingBox fitToViewport() {
    final Project project = getProject();
    if (project == null || this.image == null || !isInitialized()) {
      return new BoundingBox();
    } else {
      final BoundingBox oldValue = this.image.getBoundingBox();
      final BoundingBox viewBoundingBox = project.getViewBoundingBox();
      if (viewBoundingBox.isEmpty()) {
        return viewBoundingBox;
      } else {
        final double viewRatio = viewBoundingBox.getAspectRatio();
        final double imageRatio = this.image.getImageAspectRatio();
        BoundingBox boundingBox;
        if (viewRatio > imageRatio) {
          boundingBox = viewBoundingBox.expandPercent(-1 + imageRatio / viewRatio, 0.0);
        } else if (viewRatio < imageRatio) {
          boundingBox = viewBoundingBox.expandPercent(0.0, -1 + viewRatio / imageRatio);
        } else {
          boundingBox = viewBoundingBox;
        }
        this.image.setBoundingBox(boundingBox);
        firePropertyChange("boundingBox", oldValue, boundingBox);
        return boundingBox;
      }
    }
  }

  @Override
  public BoundingBox getBoundingBox() {
    final GeoreferencedImage image = getImage();
    if (image == null) {
      return new BoundingBox();
    } else {
      final BoundingBox boundingBox = image.getBoundingBox();
      if (boundingBox.isEmpty()) {
        return fitToViewport();
      }
      return boundingBox;
    }
  }

  @Override
  public BoundingBox getBoundingBox(final boolean visibleLayersOnly) {
    if (isExists() && (isVisible() || !visibleLayersOnly)) {
      return getBoundingBox();
    } else {
      return new BoundingBox(getGeometryFactory());
    }
  }

  @Override
  public GeometryFactory getGeometryFactory() {
    if (this.image == null) {
      return getBoundingBox().getGeometryFactory();
    } else {
      return this.image.getGeometryFactory();
    }
  }

  public GeoreferencedImage getImage() {
    return this.image;
  }

  @Override
  public boolean isHasChanges() {
    if (this.image == null) {
      return false;
    } else {
      return this.image.isHasChanages();
    }
  }

  public boolean isShowOriginalImage() {
    return this.showOriginalImage;
  }

  public void setBoundingBox(final BoundingBox boundingBox) {
    if (this.image != null) {
      this.image.setBoundingBox(boundingBox);
    }
  }

  public void setImage(final GeoreferencedImage image) {
    final GeoreferencedImage old = this.image;
    if (this.image != null) {
      Property.removeListener(image, this);
    }
    this.image = image;
    if (image == null) {
      setExists(false);
    } else {
      setExists(true);
      Property.addListener(image, this);
    }
    firePropertyChange("image", old, this.image);
  }

  public void setShowOriginalImage(final boolean showOriginalImage) {
    final Object oldValue = this.showOriginalImage;
    this.showOriginalImage = showOriginalImage;
    firePropertyChange("showOriginalImage", oldValue, showOriginalImage);
  }

  @Override
  public void setVisible(final boolean visible) {
    super.setVisible(visible);
    if (!visible) {
      setEditable(false);
    }
  }

  public void showTiePointsTable() {
    if (SwingUtilities.isEventDispatchThread()) {
      showTableView();
    } else {
      Invoke.later(this, "showTiePointsTable");
    }
  }

  public Point sourcePixelToTargetPoint(final MappedLocation tiePoint) {
    final Coordinates sourcePixel = tiePoint.getSourcePixel();
    final BoundingBox boundingBox = getBoundingBox();
    final double[] coordinates = new double[] {
      sourcePixel.getX(), sourcePixel.getY()
    };
    final AffineTransform transform = this.image.getAffineTransformation(boundingBox);
    if (!this.showOriginalImage) {
      transform.transform(coordinates, 0, coordinates, 0, 1);
    }
    final double imageX = coordinates[0];
    final double imageY = coordinates[1];
    final GeoreferencedImage image = getImage();
    final double xPercent = imageX / image.getImageWidth();
    final double yPercent = imageY / image.getImageHeight();

    final double modelWidth = boundingBox.getWidth();
    final double modelHeight = boundingBox.getHeight();

    final double modelX = boundingBox.getMinX() + modelWidth * xPercent;
    final double modelY = boundingBox.getMinY() + modelHeight * yPercent;
    final GeometryFactory geometryFactory = boundingBox.getGeometryFactory();
    final Point imagePoint = geometryFactory.point(modelX, modelY);
    return imagePoint;
  }

  public Coordinates targetPointToSourcePixel(Point targetPoint) {
    final GeoreferencedImage image = getImage();
    final BoundingBox boundingBox = getBoundingBox();
    targetPoint = boundingBox.getGeometryFactory().copy(targetPoint);
    final double modelX = targetPoint.getX();
    final double modelY = targetPoint.getY();
    final double modelDeltaX = modelX - boundingBox.getMinX();
    final double modelDeltaY = modelY - boundingBox.getMinY();

    final double modelWidth = boundingBox.getWidth();
    final double modelHeight = boundingBox.getHeight();

    final double xRatio = modelDeltaX / modelWidth;
    final double yRatio = modelDeltaY / modelHeight;

    final double imageX = image.getImageWidth() * xRatio;
    final double imageY = image.getImageHeight() * yRatio;
    final double[] coordinates = new double[] {
      imageX, imageY
    };
    if (!isShowOriginalImage()) {
      try {
        final AffineTransform transform = image.getAffineTransformation(boundingBox)
          .createInverse();
        transform.transform(coordinates, 0, coordinates, 0, 1);
      } catch (final NoninvertibleTransformException e) {
      }
    }
    return new DoubleCoordinates(coordinates);
  }

  public void toggleShowOriginalImage() {
    final boolean showOriginalImage = isShowOriginalImage();
    setShowOriginalImage(!showOriginalImage);
  }

  @Override
  public Map<String, Object> toMap() {
    final Map<String, Object> map = super.toMap();
    map.remove("querySupported");
    map.remove("selectSupported");
    map.remove("editable");
    map.remove("TableView");
    MapSerializerUtil.add(map, "url", this.url);
    MapSerializerUtil.add(map, "showOriginalImage", this.showOriginalImage);

    final Map<String, Object> imageSettings;
    if (this.image == null) {
      imageSettings = getProperty("imageSettings");
    } else {
      imageSettings = this.image.toMap();
    }
    MapSerializerUtil.add(map, "imageSettings", imageSettings);

    return map;
  }
}
